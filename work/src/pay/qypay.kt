package com.github.rwsbillyang.wxSDK.work.pay


import com.github.rwsbillyang.wxSDK.WxApi


abstract class QyPayApi: WxApi() {
    override val base = "https://api.mch.weixin.qq.com"
    override val group = "mmpaymkttransfers"

    companion object{
        const val SEND_RED_PACK = "sendworkwxredpack"
        const val QUERY_RED_PACK = "queryworkwxredpack"

        const val PAY = "promotion/paywwsptrans2pocket"
        const val QUERY_PAY = "promotion/querywwsptrans2pocket"
    }

    /**
     * 发放企业红包
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90275
     * */
    fun sendRedPack(body: Map<String, Any?>) = doPost(SEND_RED_PACK,body)


    /**
     * 查询红包记录
     * */
    fun queryRedPack(mch_billno: String) = doPost(QUERY_RED_PACK, mapOf("mch_billno" to mch_billno))


    /**
     * 向员工付款
     * */
    fun pay(body: Map<String, Any?>) = doPost(PAY,body)


    /**
     * 查询付款记录
     * https://work.weixin.qq.com/api/doc/90000/90135/90279
     * */
    fun queryPay(body: Map<String, Any?>) = doPost(QUERY_PAY,body)
}