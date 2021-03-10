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



import com.github.rwsbillyang.wxSDK.wxPay.WxPay
import com.github.rwsbillyang.wxSDK.wxPay.util.Parameters
import com.github.rwsbillyang.wxSDK.wxPay.util.PayNotifyUtil

import io.ktor.client.statement.*
import io.ktor.client.statement.HttpResponse

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import org.slf4j.LoggerFactory
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
    companion object{
        private val log = LoggerFactory.getLogger("WechatPayValidator")
    }

    /**
     * 向微信发出请求，对得到的回复的验证
     * */
    override fun validate(response: HttpResponse): String {
        //log.info("validate response, response2Parameters...")
        val parameters = response2Parameters(response)

        if(!validateParameters(parameters)) {
            log.warn("validate HttpResponse fail")
            throw IllegalArgumentException("has null value")
        }
        return parameters.body
    }

    /**
     * 验证上面的回复中的参数，或验证微信回调通知中请求头中的参数
     * */
    fun validateParameters(parameters: Parameters): Boolean {
        //log.info("validateParameters=${Json.encodeToString(parameters)}")

        if(parameters.requestId.isNullOrBlank()){
            log.warn ("empty Request-ID")
            return false
        }else if (parameters.serial.isNullOrBlank()) {
            log.warn ("empty Wechatpay-Serial")
            return false
        } else if (parameters.signature.isNullOrBlank()) {
            log.warn("empty Wechatpay-Signature")
            return false
        } else if (parameters.timestamp.isNullOrBlank()) {
            log.warn("empty Wechatpay-Timestamp")
            return false
        } else if (parameters.nonce.isNullOrBlank()) {
            log.warn("empty Wechatpay-Nonce")
            return false
        } else {
            val timestamp = parameters.timestamp
            try {
                val instant = Instant.ofEpochSecond(timestamp.toLong())
                // 拒绝5分钟之外的应答
                if (Duration.between(instant, Instant.now()).abs().toMinutes() >= 5) {
                    log.warn("timestamp=$timestamp expires")
                    return false
                }
            } catch (e: DateTimeException) {
                log.warn("invalid timestamp=$timestamp")
                return false
            } catch (e: NumberFormatException) {
                log.warn("invalid timestamp=$timestamp")
                return false
            }
        }

        val message = buildMessage(parameters.timestamp, parameters.nonce, parameters.body)
        return verifier.verify(parameters.serial, message.toByteArray(charset("utf-8")), parameters.signature)
    }


    private fun response2Parameters(response: HttpResponse): Parameters{
        val bodyText = runBlocking { response.readText() }

        return Parameters(
            response.headers["Request-ID"],
            response.headers["Wechatpay-Serial"],
            response.headers["Wechatpay-Signature"],
            response.headers["Wechatpay-Timestamp"],
            response.headers["Wechatpay-Nonce"],
            bodyText
        )
    }

    private fun buildMessage(timestamp: String, nonce: String, body: String) = "$timestamp\n$nonce\n$body\n"
}

