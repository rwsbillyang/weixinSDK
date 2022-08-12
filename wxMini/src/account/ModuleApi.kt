/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-02-02 16:39
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

package com.github.rwsbillyang.wxSDK.wxMini.account

import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.respondBox
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

import org.koin.ktor.ext.inject


fun Routing.account() {
    val controller: AccountController by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    route("/api/wx/mini") {
        get("/code2Session") {
            val appId = call.request.queryParameters["appId"]
            val jsCode = call.request.queryParameters["jsCode"]
            val needRegister = call.request.queryParameters["needRegister"]?.toInt()?:0
            call.respondBox(DataBox.ok(controller.code2Session(appId, jsCode, needRegister, call.request.origin.remoteHost, call.request.userAgent())))
        }
    }
}