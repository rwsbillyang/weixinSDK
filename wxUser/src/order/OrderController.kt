/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-30 22:21
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.wxUser.order

import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxSDK.wxPay.*
import com.github.rwsbillyang.wxSDK.wxPay.util.NotifyAnswer
import com.github.rwsbillyang.wxUser.account.AccountExpire
import com.github.rwsbillyang.wxUser.account.AccountServiceBase
import com.github.rwsbillyang.wxUser.fakeRpc.IPayWechatNotifier
import com.github.rwsbillyang.wxUser.product.ProductService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class AccountOrderController : OrderHandler(), KoinComponent {
    private val log = LoggerFactory.getLogger("OrderController")

    private val service: AccountOrderService by inject()
    private val productService: ProductService by inject()

    private val accountHelper: AccountServiceBase by inject()//具体类型取决于上层业务库中哪个子类绑定了接口
    private val wechatNotifier: IPayWechatNotifier by inject()//具体类型取决于上层业务库中哪个子类绑定了接口


    /**
     * @param appId
     * @param pId Product._id
     * @param uIdStr Account._id
     * @param oId openId
     * @param agentId 前端决定，非空则为企业微信的agent支付，否则更新到account中
     * */
    fun wxPrepay(appId: String?, pId: String?, uIdStr: String?, oId: String?, ip: String, agentId: Int?): DataBox<JsPaySignature> {
        if (appId.isNullOrBlank()) return DataBox.ko("invalid parameter: no appId, not config wxpay?")
        if (pId.isNullOrBlank()) return DataBox.ko("invalid parameter: no productId")
        if (oId.isNullOrBlank()) return DataBox.ko("no openId, please check X-Auth-oId")
        if(uIdStr.isNullOrBlank())return DataBox.ko("no openId, please check X-Auth-uId")

        val ctx = WxPay.ApiContextMap[appId] ?: return DataBox.ko("no wxPay config in WxPay, config it correctly?")

        val uId = uIdStr.toObjectId()
        val attach = AccountExpire.id(uIdStr, agentId)

        val product = productService.findOne(pId)
        if (product == null) {
            val msg = "not found product: pId=$pId"
            log.warn(msg)
            return DataBox.ko(msg)
        }
        val order = AccountOrder(ObjectId(), product, uId, oId, appId, agentId)
        service.insert(order)

        //payNotifyPath: ^https?://([^\\s/?#\\[\\]\\@]+\\@)?([^\\s/?#\\@:]+)(?::\\d{2,5})?([^\\s?#\\[\\]]*)$”"}
        val transaction = Transaction(appId, ctx.mchId,
            order._id.to64String(), product.name, product.actualPrice, oId,
            ip, WxPay.payNotifyPath(appId), attach
        )

        val res = WxPayApi(appId).orderByJs(transaction)
        if (!res.isOK() || res.prepayId == null) {
            log.warn("prepay return: $res")
            return DataBox.ko("${res.code}: ${res.message}")
        }

        return DataBox.ok(JsPaySignature(appId, res.prepayId!!))
    }




    //SUCCESS：支付成功
    override fun onSuccess(orderPayDetail: OrderPayDetail): NotifyAnswer {
        val order = service.findOne(orderPayDetail.orderId)
        if (order == null) {
            log.warn("Not_found_order: $orderPayDetail")
            return NotifyAnswer("Not_found_order", "out_trade_no(${orderPayDetail.orderId}) is wrong?")
        }

        if (order.status != OrderConstant.STATUS_TODO_PAY) {
            log.warn("duplicated notify? orderId=${orderPayDetail.orderId}")
            return NotifyAnswer.success()
        }
        runBlocking {
            launch{
                log.info("success! update orderStatus, orderId=${orderPayDetail.orderId}")
                service.updateOrder(order._id, OrderConstant.STATUS_TODO_DELIVER, orderPayDetail)
                val attach = orderPayDetail.attach
                if(attach == null){
                    log.warn("not found attach")
                }else{
                    val product = order.product

                    val array = attach.split("/")
                    val accountId = array.firstOrNull()
                    //val agentId = if(array.size == 2) array[1].toInt() else null
                    if(accountId != null){
                        //update Edition and expiration: uId, openId, product snapshot
                        val newExpire = accountHelper.updateAccountExpiration(accountId, order.agentId, product.edition, product.year, product.month, product.bonus)

                        val account = accountHelper.findById(accountId)
                        if(account != null){
                            if (newExpire > 0) {
                                service.updateOrder(order._id, OrderConstant.STATUS_DONE)
                                wechatNotifier.onPaySuccess(account, order.appId, order.agentId, product.name,"￥${product.actualPrice/100.0}元", newExpire, "支付成功")
                            } else {
                                log.warn("fail to update Edition and expiration: orderId=${orderPayDetail.orderId}")
                            }
                        }else{
                            log.warn("not found account: attach=$attach")
                        }
                    }
                }
            }
        }


        return NotifyAnswer.success()
    }



    //PAYERROR：支付失败(其他原因，如银行返回失败)
    override fun onPayError(orderPayDetail: OrderPayDetail): NotifyAnswer {
        service.updateOrder(orderPayDetail.orderId.toObjectId(), null, orderPayDetail)
        return super.onPayError(orderPayDetail)
    }

    override fun onUnknownState(orderPayDetail: OrderPayDetail): NotifyAnswer {
        service.updateOrder(orderPayDetail.orderId.toObjectId(), null, orderPayDetail)
        return super.onUnknownState(orderPayDetail)
    }
}