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

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString



/**
 * https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pages/Overview.shtml
 * */
class WxPayApi(private val appId: String): WxApi() {

    override val base = "https://api.mch.weixin.qq.com/v3/pay"
    override val group ="transactions"
    override fun url(name: String, requestParams: Map<String, String?>?, needAccessToken: Boolean)
            = super.url(name, requestParams, false)

    override fun accessToken(): String? {
        TODO("Not yet implemented")
    }

    private val ctx = WxPay.ApiContextMap[appId]

    /**
     * 返回R泛型类型结果
     * */
    private inline fun  <reified R> doWxPayGet(url: String): R = runBlocking {
        requireNotNull(ctx){"not config wxPayConfig in DB when call doWxPayGet? "}


        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val response: HttpResponse = client.get(url){
                header("Authorization", getAuthorizationHeader("GET", url.substringAfter("weixin.qq.com"), ""))
            }
            val bodyText = ctx.validator.validate(response)
            apiJson.decodeFromString(bodyText)
        }
    }

    /**
     * 返回R泛型类型结果
     * */
    private inline fun  <reified T, reified R> doWxPayPost(name: String, data: T? = null, parameters: Map<String, String?>? = null): R = runBlocking {
        requireNotNull(ctx){"not config wxPayConfig in DB when call doWxPayPost? "}

        val url = url(name, parameters)
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val response: HttpResponse = client.post(url) {
                header("Authorization", getAuthorizationHeader("POST", url.substringAfter("weixin.qq.com"),
                    data?.let { apiJson.encodeToString(data) }?:""))
                data?.let { body = data }
            }
            val bodyText = ctx.validator.validate(response)
            apiJson.decodeFromString(bodyText)
        }
    }

    //https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient WechatPayUploadHttpPost
//    inline fun <reified R> doWxPayUpload(name: String, filePath: String,
//                                    parameters: Map<String, String?>? = null,
//                                    formData: Map<String, String>? = null) :R= runBlocking {
//        //append("media",filePath, ContentType.Video, file.length())
//        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
    // https://ktor.io/docs/request.html#customizing-requests
//            client.post(url(name, parameters)) {
//                val file = File(filePath)
//                body = MultiPartFormDataContent(formData {
//                    formData?.forEach { append(it.key, it.value) }
//                    val meta = String.format("{\"filename\":\"%s\",\"sha256\":\"%s\"}", filePath, fileSha256)
//                    append("meta",meta)
//                    appendInput("media", size = file.length()) {
//                        FileInputStream(file).asInput()
//                    }
//                })
//            }
//        }
//    }

    //GET or POST
    private fun getAuthorizationHeader(method: String, canonicalUrl: String, body: String): String{
        requireNotNull(ctx){"not config wxPayConfig in DB when call getAuthorizationHeader? "}
        val credentials = ctx.credentials
        return credentials.schema + " " + credentials.getToken(method, canonicalUrl, body)

    }

    fun orderByApp(transaction: Transaction): ResponseOrder = doWxPayPost("app", transaction)
    fun orderByJs(transaction: Transaction): ResponseOrder = doWxPayPost("jsapi", transaction)
    fun orderByNative(transaction: Transaction): ResponseOrderNative = doWxPayPost("native", transaction)
    fun orderByH5(transaction: Transaction): ResponseOrderH5 = doWxPayPost("h5", transaction)

    fun queryOrderByTransactionId(transactionId: String): OrderPayDetail
            = doWxPayGet(url("id/${transactionId}", mapOf("mchid" to WxPay.ApiContextMap[appId]?.mchId)))
    fun queryOrderByOrderId(orderId: String): OrderPayDetail
            = doWxPayGet(url("out-trade-no/${orderId}", mapOf("mchid" to WxPay.ApiContextMap[appId]?.mchId)))

    /**
     * 商户订单支付失败需要生成新单号重新发起支付，要对原订单号调用关单，避免重复支付；
     * 系统下单后，用户支付超时，系统退出不再受理，避免用户继续，请调用关单接口。
     *
     * 订单生成后不能马上调用关单接口，最短调用时间间隔为5分钟。
     * */
    fun closeOrder(orderId: String): ResponseClose
            = doWxPayPost("out-trade-no/${orderId}/close", mapOf("mchid" to WxPay.ApiContextMap[appId]?.mchId))

    //https://wechatpay-api.gitbook.io/wechatpay-api-v3/jie-kou-wen-dang/ping-tai-zheng-shu
    internal fun downloadPlatformCerts(): ResponseCertificates
            = doWxPayGet("https://api.mch.weixin.qq.com/v3/certificates")
}

