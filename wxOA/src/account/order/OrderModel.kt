/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-30 21:07
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

@file:UseContextualSerialization(ObjectId::class)

package com.github.rwsbillyang.wxOA.account.order


import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
import com.github.rwsbillyang.ktorKit.util.DatetimeUtil
import com.github.rwsbillyang.ktorKit.util.toUtc
import com.github.rwsbillyang.wxOA.account.product.Product
import com.github.rwsbillyang.wxSDK.wxPay.OrderPayDetail
import io.ktor.resources.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.*

//订单状态：
// 待支付 0：
// 支付失败 1：
// 待发货；2：
// 待收货；3：
// 已收货，待评价；4：
// 已完成: 5；
// 10：退款新申请，11： 退款成功；12：退款拒绝
// -1: 已取消，-2：已关闭 -3：删除
object OrderConstant{
        const val STATUS_TODO_PAY = 0
        const val STATUS_TODO_FAIL = 1
        const val STATUS_TODO_DELIVER = 2
        const val STATUS_TODO_RECEIVE = 3
        const val STATUS_TODO_COMMENT = 4
        const val STATUS_DONE = 5

        const val STATUS_REFUND_NEW = 11
        const val STATUS_REFUND_APPROVAL = 12
        const val STATUS_REFUND_DONE = 13
        const val STATUS_REFUND_REJECT = 15


        const val STATUS_CANCELED = -1
        const val STATUS_CLOSED = -2
        const val STATUS_REMOVED = -3

        const val FROM_DEFAULT = 0 //null or 0 means default
        const val FROM_PINTUAN = 1
        const val FROM_FENXIAO = 2
        const val FROM_SALE_CHANNEL = 3



        const val TYPE_WECHAT = 1
}
/**
 * 虚拟物品，账户有效期订单
 * @param _id 订单id
 *
 * @param product 所购买的版本
 * @param uId 付款人用户id 已注册用户的付款
 * @param oId 付款人openID 未注册用户的付款
 * @param time 生成时间
 * @param type 订单类型，暂只是微信订单
 * @param from 来源： 默认，拼团、分销等，暂只有默认
 * @param share 分账时才大于0，否则未0
 * @param status 订单状态：未付款，已付款，已发货
 * @param orderPayDetail 清算商回传的订单支付详情
 *
 * */
@Serializable
data class AccountOrder(
        val _id: ObjectId,
        val product: Product,
        val uId: ObjectId?,//用户id 已注册用户的付款
        val oId: String, //付款人openID 未注册用户的付款
        val appId: String,
        //val agentId: Int? = null,
        val time: Long = System.currentTimeMillis(),
        val type: Int = OrderConstant.TYPE_WECHAT,
        val from: Int = OrderConstant.FROM_DEFAULT,
        val share: Int = 0,
        val status: Int = OrderConstant.STATUS_TODO_PAY,
        @SerialName("detail")
        val orderPayDetail: OrderPayDetail? = null
        )


@Serializable
@Resource("/list")
data class AccountOrderListParams(
        override val umi: String,
        val type: Int? = null,
        val status: Int? = null,
        val from: Int? = null,
        val start: String? = null,
        val end: String? = null,
        val transactionId: String? = null,
        val appId: String? = null,
        val lastId: ObjectId? = null
): IUmiPaginationParams {
        override fun toFilter(): Bson {
                val typeFilter = type?.let { AccountOrder::type eq it }
                val statusFilter = status?.let { AccountOrder::status eq it }
                val fromFilter = from?.let { AccountOrder::from eq it }
                val transactionIdFilter = transactionId?.let { AccountOrder:: orderPayDetail / OrderPayDetail::transactionId eq it }
                //val objectIdFilter = lastId?.let { Order::_id lt it }
                val startFilter = start?.let { DatetimeUtil.parse(it)}?.toUtc()?.let { AccountOrder::time gte it }
                val endFilter = end?.let { DatetimeUtil.parse(it)}?.toUtc()?.let { AccountOrder::time lte it }
                val appIdF = appId?.let{  AccountOrder::appId eq it }
                return and(typeFilter, statusFilter, fromFilter, transactionIdFilter, appIdF,  startFilter, endFilter)
        }
}

//class OrderList(
//        val data: List<AccountOrder>? = null,
//        val total: Long? = 0L
//): Box()