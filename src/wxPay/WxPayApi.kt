package com.github.rwsbillyang.wxSDK.wxPay

import com.github.rwsbillyang.wxSDK.common.WxApi


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

    fun orderByApp(transaction: Transaction):ResponseOrder = doPost2("app", transaction)
    fun orderByJs(transaction: Transaction):ResponseOrder = doPost2("jsapi", transaction)
    fun orderByNative(transaction: Transaction):ResponseOrderNative = doPost2("native", transaction)
    fun orderByH5(transaction: Transaction):ResponseOrderH5 = doPost2("h5", transaction)

    fun queryOrderByTransactionId(transactionId: String):ResponseQueryOrder
            = doGet2("id/${transactionId}", mapOf("mchid" to WxPay.context.mchId))
    fun queryOrderByOrderId(orderId: String):ResponseQueryOrder
            = doGet2("out-trade-no/${orderId}", mapOf("mchid" to WxPay.context.mchId))

    /**
     * 商户订单支付失败需要生成新单号重新发起支付，要对原订单号调用关单，避免重复支付；
     * 系统下单后，用户支付超时，系统退出不再受理，避免用户继续，请调用关单接口。
     *
     * 订单生成后不能马上调用关单接口，最短调用时间间隔为5分钟。
     * */
    fun closeOrder(orderId: String):ResponseClose
            = doPost2("out-trade-no/${orderId}/close", mapOf("mchid" to WxPay.context.mchId))

    //https://wechatpay-api.gitbook.io/wechatpay-api-v3/jie-kou-wen-dang/ping-tai-zheng-shu
    internal fun downloadPlatformCerts(): ResponseCertificates
            = doGetByUrl("https://api.mch.weixin.qq.com/v3/certificates")
}

