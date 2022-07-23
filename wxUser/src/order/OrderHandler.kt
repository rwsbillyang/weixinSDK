/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-08 16:24
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


import com.github.rwsbillyang.wxSDK.wxPay.OrderPayDetail
import com.github.rwsbillyang.wxSDK.wxPay.util.NotifyAnswer
import com.github.rwsbillyang.wxSDK.wxPay.util.PayNotifyBean
import com.github.rwsbillyang.wxSDK.wxPay.util.WxPayNotifyErrorType
import org.slf4j.LoggerFactory

open class OrderHandler {
    private val log = LoggerFactory.getLogger("OrderHandler")

    /**
     * 处理微信发送过来的支付通知
     * https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pay/transactions/chapter3_11.shtml
     * */
    fun onWxPayNotify(
        appId: String,
        payNotifyBean: PayNotifyBean?,
        orderPayDetail: OrderPayDetail?,
        errType: WxPayNotifyErrorType
    ): NotifyAnswer {
        when (errType) {
            WxPayNotifyErrorType.EmptyRequestBody -> {
                log.warn("EmptyRequestBody")
                return NotifyAnswer.success()
            }
            WxPayNotifyErrorType.ValidateFail -> {
                log.warn("ValidateFail")
                return NotifyAnswer.success()
            }
            WxPayNotifyErrorType.TransactionNotSuccess -> {
                log.warn("TransactionNotSuccess")
                return NotifyAnswer.success()
            }
            WxPayNotifyErrorType.SUCCESS -> {
                if (orderPayDetail == null) {
                    log.warn("Should NOT come here: WxPayNotifyErrorType.SUCCESS, but orderPayDetail is null?")
                    return NotifyAnswer.success()
                }

                return when (orderPayDetail.tradeState) {
                    "SUCCESS" -> onSuccess(orderPayDetail)
                    "REFUND" -> onRefund(orderPayDetail)
                    "NOTPAY" -> onNotPay(orderPayDetail)
                    "CLOSED" -> onClosed(orderPayDetail)
                    "REVOKED" -> onRevoked(orderPayDetail)
                    "USERPAYING" -> onUserPaying(orderPayDetail)
                    "PAYERROR" -> onPayError(orderPayDetail)
                    else -> onUnknownState(orderPayDetail)
                }
            }
        }
    }



    //SUCCESS：支付成功
    open fun onSuccess(orderPayDetail: OrderPayDetail): NotifyAnswer {

        return NotifyAnswer.success()
    }

    //REFUND：转入退款
    open fun onRefund(orderPayDetail: OrderPayDetail): NotifyAnswer {
        log.warn("TODO: onRefund, orderPayDetail=$orderPayDetail")
        return NotifyAnswer.success()
    }

    //NOTPAY：未支付
    open fun onNotPay(orderPayDetail: OrderPayDetail): NotifyAnswer {
        log.warn("TODO: onNotPay, orderPayDetail=$orderPayDetail")
        return NotifyAnswer.success()
    }

    //CLOSED：已关闭
    open fun onClosed(orderPayDetail: OrderPayDetail): NotifyAnswer {
        log.warn("TODO: onClosed, orderPayDetail=$orderPayDetail")
        return NotifyAnswer.success()
    }

    //REVOKED：已撤销（付款码支付）
    open fun onRevoked(orderPayDetail: OrderPayDetail): NotifyAnswer {
        log.warn("TODO: onRevoked, orderPayDetail=$orderPayDetail")
        return NotifyAnswer.success()
    }

    //USERPAYING：用户支付中（付款码支付）
    open fun onUserPaying(orderPayDetail: OrderPayDetail): NotifyAnswer {
        log.warn("TODO: onUserPaying, orderPayDetail=$orderPayDetail")
        return NotifyAnswer.success()
    }

    //PAYERROR：支付失败(其他原因，如银行返回失败)
    open fun onPayError(orderPayDetail: OrderPayDetail): NotifyAnswer {
        log.warn("TODO: onPayError, orderPayDetail=$orderPayDetail")
        return NotifyAnswer.success()
    }

    open fun onUnknownState(orderPayDetail: OrderPayDetail): NotifyAnswer {
        log.warn("TODO: onUnknownState, orderPayDetail=$orderPayDetail")
        return NotifyAnswer.success()
    }
}