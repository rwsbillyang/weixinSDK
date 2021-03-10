/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 12:22
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


import com.github.rwsbillyang.wxSDK.wxPay.util.RequestParameters
import io.ktor.client.statement.*
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking

import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant

/**
 * 使用CertificatesVerifier，用于验证微信的回复或通知：验证是否真正来自于微信。
 * 验证回复头中一些列信息是否为空，以及签名是否合法
 *
 * 如果验证商户的请求签名正确，微信支付会在应答的HTTP头部中包括应答签名。我们建议商户验证应答签名。
 * 同样的，微信支付会在回调的HTTP头部中包括回调报文的签名。商户必须验证回调的签名，以确保回调是由微信支付发送。
 *
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/qian-ming-zhi-nan-1/qian-ming-yan-zheng
 * https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient
 * */
class WechatPayValidator(private val verifier: Verifier) : Validator {

    fun validate(parameters: RequestParameters): Boolean {
        try {
            validateParameters(parameters)
            val message = buildMessage(parameters.timestamp!!, parameters.nonce!!, parameters.body!!)

            //微信支付签名使用微信支付平台私钥，证书序列号包含在应答HTTP头部的Wechatpay-Serial
            //商户上送敏感信息时使用微信支付平台公钥加密，证书序列号包含在请求HTTP头部的Wechatpay-Serial
            val serial = parameters.serial
            val signature = parameters.signature
            if (!verifier.verify(serial!!, message.toByteArray(charset("utf-8")), signature!!)) {
                throw verifyFail(
                    "serial=[%s] message=[%s] sign=[%s], request-id=[%s]",
                    serial, message, signature, parameters.requestId
                )
            }
        } catch (e: IllegalArgumentException) {
            log.warn("validate RequestParameters, IllegalArgumentException: ${e.message}")
            return false
        } catch (e: IOException) {
            log.warn("validate RequestParameters, IOException: ${e.message}")
            return false
        } catch (e: Exception) {
            log.warn("validate RequestParameters, Exception: ${e.message}")
            return false
        }
        return true
    }

    private fun validateParameters(parameters: RequestParameters) {
        val requestId = parameters.requestId
        if (requestId.isNullOrBlank()) {
            throw parameterError("empty Request-ID")
        }

        if (parameters.serial.isNullOrBlank()) {
            throw parameterError("empty Wechatpay-Serial, request-id=[%s]", requestId)
        } else if (parameters.signature.isNullOrBlank()) {
            throw parameterError("empty Wechatpay-Signature, request-id=[%s]", requestId)
        } else if (parameters.timestamp.isNullOrBlank()) {
            throw parameterError("empty Wechatpay-Timestamp, request-id=[%s]", requestId)
        } else if (parameters.nonce.isNullOrBlank()) {
            throw parameterError("empty Wechatpay-Nonce, request-id=[%s]", requestId)
        } else {
            val timestamp = parameters.timestamp
            try {
                val instant = Instant.ofEpochSecond(timestamp.toLong())
                // 拒绝5分钟之外的应答
                if (Duration.between(instant, Instant.now()).abs().toMinutes() >= 5) {
                    throw parameterError(
                        "timestamp=[%s] expires, request-id=[%s]",
                        timestamp, requestId
                    )
                }
            } catch (e: DateTimeException) {
                throw parameterError(
                    "invalid timestamp=[%s], request-id=[%s]",
                    timestamp, requestId
                )
            } catch (e: NumberFormatException) {
                throw parameterError(
                    "invalid timestamp=[%s], request-id=[%s]",
                    timestamp, requestId
                )
            }
        }
    }


    //微信支付签名使用微信支付平台私钥，证书序列号包含在应答HTTP头部的Wechatpay-Serial
    //商户上送敏感信息时使用微信支付平台公钥加密，证书序列号包含在请求HTTP头部的Wechatpay-Serial
    override fun validate(response: HttpResponse): String {
        val requestId = response.headers["Request-ID"] ?: throw parameterError("empty Request-ID")
        val serial = response.headers["Wechatpay-Serial"] ?: throw parameterError(
            "empty Wechatpay-Serial, request-id=[%s]",
            requestId
        )
        val signature = response.headers["Wechatpay-Signature"]
            ?: throw parameterError("empty Wechatpay-Signature, request-id=[%s]", requestId)
        val nonce = response.headers["Wechatpay-Nonce"] ?: throw parameterError(
            "empty Wechatpay-Nonce, request-id=[%s]",
            requestId
        )
        val timestamp = response.headers["Wechatpay-Timestamp"]
            ?: throw parameterError("empty Wechatpay-Timestamp, request-id=[%s]", requestId)

        try {
            val instant = Instant.ofEpochSecond(timestamp.toLong())
            // 拒绝5分钟之外的应答
            if (Duration.between(instant, Instant.now()).abs().toMinutes() >= 5) {
                throw parameterError(
                    "timestamp=[%s] expires, request-id=[%s]",
                    timestamp, requestId
                )
            }
        } catch (e: DateTimeException) {
            throw parameterError(
                "invalid timestamp=[%s], request-id=[%s]",
                timestamp, requestId
            )
        } catch (e: NumberFormatException) {
            throw parameterError(
                "invalid timestamp=[%s], request-id=[%s]",
                timestamp, requestId
            )
        }


        val bodyText = runBlocking { response.readText() }
        val message = buildMessage(timestamp, nonce, bodyText)

        if (!verifier.verify(serial, message.toByteArray(charset("utf-8")), signature)) {
            throw verifyFail(
                "serial=[%s] message=[%s] sign=[%s], request-id=[%s]",
                serial, message, signature,
                response.headers["Request-ID"]
            )
        }
        return bodyText

    }


    private fun buildMessage(timestamp: String, nonce: String, body: String) = "$timestamp\n$nonce\n$body\n"


    companion object {
        private val log = LoggerFactory.getLogger("WechatPayValidator")
        fun parameterError(msg: String, vararg args: Any?): RuntimeException {
            var message = msg
            message = String.format(message, *args)
            log.warn("parameterError: $message")
            return IllegalArgumentException("parameter error: $message")
        }

        fun verifyFail(msg: String, vararg args: Any?): RuntimeException {
            var message = msg
            message = String.format(message, *args)
            log.warn("signature verify fail:: $message")
            return IllegalArgumentException("signature verify fail: $message")
        }
    }
}