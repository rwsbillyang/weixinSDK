/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 12:21
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

package com.github.rwsbillyang.wxSDK.wxPay.auth



import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.client.methods.HttpRequestWrapper
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * 用私钥及商户个体信息，生成一系列信息及签名，作为token，放置于请求头中：
 *      Authorization: 认证类型[WECHATPAY2-SHA256-RSA2048] 签名信息
 *
 * 微信支付API v3要求商户对请求进行签名。微信支付会在收到请求后进行签名的验证。如果签名验证不通过，
 * 微信支付API v3将会拒绝处理请求，并返回401 Unauthorized。
 *
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/qian-ming-zhi-nan-1/qian-ming-sheng-cheng#ji-suan-qian-ming-zhi
 * https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient
 * */
class WechatPayCredentials(var merchantId: String,  var signer: Signer): Credentials {
    companion object {
        private val log = LoggerFactory.getLogger("WechatPay2Credentials")
        private const val SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val RANDOM = SecureRandom()
    }
    override val schema = "WECHATPAY2-SHA256-RSA2048"

    @Throws(IOException::class)
    override fun getToken(request: HttpRequestWrapper): String {
        val nonceStr = generateNonceStr()
        val timestamp = generateTimestamp()
        val message = buildMessage(nonceStr, timestamp, request)
        log.debug("authorization message=[$message]")
        val signature = signer.sign(message.toByteArray(StandardCharsets.UTF_8))
        val token = ("mchid=\"" + merchantId + "\","
                + "nonce_str=\"" + nonceStr + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + signature.certificateSerialNumber + "\","
                + "signature=\"" + signature.sign + "\"")
        log.debug("authorization token=[$token]")
        return token
    }

    private fun generateTimestamp(): Long {
        return System.currentTimeMillis() / 1000
    }

    private fun generateNonceStr(): String {
        val nonceChars = CharArray(32)
        for (index in nonceChars.indices) {
            nonceChars[index] = SYMBOLS[RANDOM.nextInt(SYMBOLS.length)]
        }
        return String(nonceChars)
    }


    @Throws(IOException::class)
    private fun buildMessage(nonce: String, timestamp: Long, request: HttpRequestWrapper): String {
        val uri = request.uri
        var canonicalUrl = uri.rawPath
        if (uri.query != null) {
            canonicalUrl += "?" + uri.rawQuery
        }
        var body = ""
        // PATCH,POST,PUT
        if (request.original is WechatPayUploadHttpPost) {
            body = (request.original as WechatPayUploadHttpPost).meta
        } else if (request is HttpEntityEnclosingRequest) {
            body = EntityUtils.toString((request as HttpEntityEnclosingRequest).entity)
        }
        return """
            ${request.requestLine.method}
            $canonicalUrl
            $timestamp
            $nonce
            $body
            
            """.trimIndent()
    }



}
