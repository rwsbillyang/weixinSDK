/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 22:00
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

package com.github.rwsbillyang.wxSDK.wxPay.util

import com.github.rwsbillyang.wxSDK.security.AesUtil
import com.github.rwsbillyang.wxSDK.wxPay.EncryptData
import com.github.rwsbillyang.wxSDK.wxPay.OrderPayDetail
import com.github.rwsbillyang.wxSDK.wxPay.WxPay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

//val list = arrayListOf(
//    "Request-ID",
//    "Wechatpay-Serial",
//    "Wechatpay-Signature",
//    "Wechatpay-Timestamp",
//    "Wechatpay-Nonce"
//)

/**
 * 微信支付会在回调的HTTP头部中包括回调报文的签名。商户必须验证回调的签名，以确保回调是由微信支付发送。
 * */
@Serializable
class RequestParameters(
    val requestId: String?,
    val serial: String?,
    val signature: String?,
    val timestamp: String?,
    val nonce: String?,
    val body: String?
)

/**
 * https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pay/transactions/chapter3_11.shtml
 * 通知ID	id	string[1,32]	是	通知的唯一ID 示例值：EV-2018022511223320873
 * 通知创建时间	create_time	string[1,16]	是	通知创建的时间，遵循rfc3339标准格式，格式为YYYY-MM-DDTHH:mm:ss+TIMEZONE，YYYY-MM-DD表示年月日，T出现在字符串中，表示time元素的开头，HH:mm:ss.表示时分秒，TIMEZONE表示时区（+08:00表示东八区时间，领先UTC 8小时，即北京时间）。例如：2015-05-20T13:29:35+08:00表示北京时间2015年05月20日13点29分35秒。 示例值：2015-05-20T13:29:35+08:00
 * 通知类型	event_type	string[1,32]	是	通知的类型，支付成功通知的类型为TRANSACTION.SUCCESS 示例值：TRANSACTION.SUCCESS
 * 通知数据类型	resource_type	string[1,32]	是	通知的资源数据类型，支付成功通知为encrypt-resource 示例值：encrypt-resource
 * 通知数据	resource	object	是	通知资源数据 json格式，见示例
 * 回调摘要	summary	string[1,64]	是	回调摘要 示例值：支付成功
 * */
@Serializable
class PayNotifyBean(
    val id: String,
    @SerialName("create_time")
    val time: String,
    @SerialName("event_type")
    val type: String,
    val summary: String,
    @SerialName("resource_type")
    val resType: String,
    val resource: EncryptData
)

/**
 * 支付通知http应答码为200或204才会当作正常接收，当回调处理异常时，应答的HTTP状态码应为500，或者4xx。
 * @param code 返回状态码	code	string[1,32]	是	错误码，SUCCESS为清算机构接收成功，其他错误码为失败。示例值：SUCCESS
 * @param message 返回信息	message	string[1,64]	是	返回信息，如非空，为错误原因。示例值：系统错误
 * */
@Serializable
class NotifyAnswer(
    val code: String = "SUCCESS",
    val message: String? = null
){
    fun isSuccess() = code == "SUCCESS"
}

enum class WxPayNotifyErrorType(val errMsg: String?){
    EmptyRequestBody("fail to deserialize reuqest body"),
    ValidateFail("fail to validate"),
    TransactionNotSuccess("PayNotifyBean.type != TRANSACTION.SUCCESS"),
    SUCCESS(null)
}



object PayNotifyUtil {
    private val log = LoggerFactory.getLogger("PayNotifyUtil")
    /**
     * 微信支付会在回调的HTTP头部中包括回调报文的签名。商户必须验证回调的签名，以确保回调是由微信支付发送。
     * 加密不能保证通知请求来自微信。微信会对发送给商户的通知进行签名，并将签名值放在通知的HTTP头Wechatpay-Signature。
     * 商户应当验证签名，以确认请求来自微信，而不是其他的第三方。
     *
     * @param parameters 由请求头及请求body构造而成
     * @param block 回调代码块
     * https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pay/transactions/chapter3_11.shtml
     * */
    fun handleNotify(
        appId: String,
        parameters: RequestParameters,
        block: (
            notifyBean: PayNotifyBean?,
            orderPayDetail: OrderPayDetail?,
            errType: WxPayNotifyErrorType
        ) -> NotifyAnswer
    ):NotifyAnswer {
        val notifyBean = parameters.body?.let { Json.decodeFromString<PayNotifyBean>(it) }
        if(notifyBean == null){
            log.warn("Json.decodeFromString fail? RequestParameters=$RequestParameters")
            return block(null, null, WxPayNotifyErrorType.EmptyRequestBody)
        }
        val ctx = WxPay.ApiContextMap[appId]
        if(ctx == null){
            log.warn("no wxPay config for appId=$appId")
            return block(null, null, WxPayNotifyErrorType.EmptyRequestBody)
        }
        if (!ctx.validator.validate(parameters)) {
            log.warn("fake notify? body: RequestParameters=$RequestParameters")
            return block(notifyBean, null, WxPayNotifyErrorType.ValidateFail)
        }

        return if (notifyBean.type == "TRANSACTION.SUCCESS") {
            val decryptor = AesUtil(ctx.apiV3Key)
            val res = notifyBean.resource
            val json = decryptor.decryptToString(
                res.associatedData?.toByteArray(),
                res.nonce.toByteArray(),
                res.ciphertext
            )
            val orderPayDetail = Json.decodeFromString<OrderPayDetail>(json)
            block(notifyBean, orderPayDetail, WxPayNotifyErrorType.SUCCESS)
        } else {
            log.warn("notifyBean.type is not TRANSACTION.SUCCESS, RequestParameters=$RequestParameters")
            block(notifyBean, null, WxPayNotifyErrorType.TransactionNotSuccess)
        }
    }
}