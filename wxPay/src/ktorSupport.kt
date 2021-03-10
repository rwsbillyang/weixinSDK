/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-12-14 18:13
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

import com.github.rwsbillyang.wxSDK.wxPay.util.*
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets


fun Routing.wxPayNotify(pathPrefix: String = WxPay.payNotifyUrlPrefix,
                        block: (appId: String,
                                notifyBean: PayNotifyBean?,
                                orderPayDetail: OrderPayDetail?,
                                errType: WxPayNotifyErrorType
                        ) -> NotifyAnswer) {
    // "/api/sale/wx/payNotify"
    post("$pathPrefix{appId}") {

        //https://pay.weixin.qq.com/wiki/doc/apiv3/wechatpay/wechatpay7_1.shtml
        // 微信支付的回调，在HTTP头部包含了以下四个HTTP头：
        //Wechatpay-Timestamp
        //Wechatpay-Nonce
        //Wechatpay-Signature
        //Wechatpay-Serial

        val params = Parameters(
                "noRequest-ID",
                call.request.headers["Wechatpay-Serial"],
                call.request.headers["Wechatpay-Signature"],
                call.request.headers["Wechatpay-Timestamp"],
                call.request.headers["Wechatpay-Nonce"],
                is2String(call) //直接用all.receiveText()导致签名验证错误
        )
        val appId = call.parameters["appId"]
        requireNotNull(appId) {
            "config wxPay notify wrong? no appId in notify path"
        }

        val answer = PayNotifyUtil.handleNotify(appId, params) { payNotifyBean, orderPayDetail, errType ->
            block(appId, payNotifyBean, orderPayDetail, errType)
        }

        call.respond(answer)
    }
}

fun is2String(call: ApplicationCall) = runBlocking {
        withContext(Dispatchers.IO) {
            call.receive<InputStream>().use {
                val result = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var length: Int
                while (it.read(buffer).also { length = it } != -1) {
                    result.write(buffer, 0, length)
                }
                 result.toString(StandardCharsets.UTF_8.name())
            }
        }
}