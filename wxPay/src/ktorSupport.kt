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
import io.ktor.routing.*


fun Routing.wxPayNotify(pathPrefix: String = WxPay.payNotifyUrlPrefix,
                        block: (appId: String,
                                notifyBean: PayNotifyBean?,
                                orderPayDetail: OrderPayDetail?,
                                errType: WxPayNotifyErrorType
                        ) -> NotifyAnswer) {
    // "/api/sale/wx/payNotify"
    post("$pathPrefix{appId}") {
        val params = RequestParameters(
                call.request.headers["Request-ID"],
                call.request.headers["Wechatpay-Serial"],
                call.request.headers["Wechatpay-Signature"],
                call.request.headers["Wechatpay-Timestamp"],
                call.request.headers["Wechatpay-Nonce"],
                call.receiveText()
        )
        val appId = call.parameters["appId"]
        requireNotNull(appId) {
            "config wxPay notify wrong? no appId in notify path"
        }

        PayNotifyUtil.handleNotify(appId, params) { payNotifyBean, orderPayDetail, errType ->
            block(appId, payNotifyBean, orderPayDetail, errType)
        }
    }
}
