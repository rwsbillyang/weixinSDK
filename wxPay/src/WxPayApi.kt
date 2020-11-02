/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:33
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

package com.github.rwsbillyang.wxSDK.wxPay

import com.github.rwsbillyang.wxSDK.WxApi


/**
 * https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pages/Overview.shtml
 * */
object WxPayApi: WxApi() {
    override val base = "https://api.mch.weixin.qq.com/v3/pay"
    override val group ="transactions"
    override fun url(name: String, requestParams: Map<String, String?>?, needAccessToken: Boolean)
            = super.url(name, requestParams, false)

    override var client = WxPay.client()

    override fun accessToken(): String? {
        TODO("Not yet implemented")
    }

    fun orderByApp(transaction: Transaction): ResponseOrder = doPost2("app", transaction)
    fun orderByJs(transaction: Transaction): ResponseOrder = doPost2("jsapi", transaction)
    fun orderByNative(transaction: Transaction): ResponseOrderNative = doPost2("native", transaction)
    fun orderByH5(transaction: Transaction): ResponseOrderH5 = doPost2("h5", transaction)

    fun queryOrderByTransactionId(transactionId: String): OrderPayDetail
            = doGet2("id/${transactionId}", mapOf("mchid" to WxPay.context.mchId))
    fun queryOrderByOrderId(orderId: String): OrderPayDetail
            = doGet2("out-trade-no/${orderId}", mapOf("mchid" to WxPay.context.mchId))

    /**
     * 商户订单支付失败需要生成新单号重新发起支付，要对原订单号调用关单，避免重复支付；
     * 系统下单后，用户支付超时，系统退出不再受理，避免用户继续，请调用关单接口。
     *
     * 订单生成后不能马上调用关单接口，最短调用时间间隔为5分钟。
     * */
    fun closeOrder(orderId: String): ResponseClose
            = doPost2("out-trade-no/${orderId}/close", mapOf("mchid" to WxPay.context.mchId))

    //https://wechatpay-api.gitbook.io/wechatpay-api-v3/jie-kou-wen-dang/ping-tai-zheng-shu
    internal fun downloadPlatformCerts(): ResponseCertificates
            = doGetByUrl("https://api.mch.weixin.qq.com/v3/certificates")
}

