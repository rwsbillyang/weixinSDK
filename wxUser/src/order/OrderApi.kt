/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-30 22:21
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

package com.github.rwsbillyang.wxUser.order

import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.respondBox
import com.github.rwsbillyang.wxSDK.wxPay.WxPay

import com.github.rwsbillyang.wxUser.agentId

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject


fun Routing.accountOrderApi(){
    val controller: AccountOrderController by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    route("/api/sale/order") {
        get("/wx/prepay")
        {
            val appId = call.request.queryParameters["appId"]?: WxPay.ApiContextMap.values.firstOrNull()?.appId
            val pId = call.request.queryParameters["pId"]
            val oId = call.request.queryParameters["oId"]
            val uId = call.request.queryParameters["uId"]
            //val uId = call.uId

            call.respondBox(controller.wxPrepay(appId,pId, uId, oId, call.request.origin.remoteHost, call.agentId))
        }



        authenticate {
            intercept(ApplicationCallPipeline.Call) {
                when (jwtHelper.isAuthorized(call)) {
                    true, null -> proceed()
                    else -> {
                        call.respond(HttpStatusCode.Forbidden)
                        return@intercept finish()
                    }
                }
            }
            get<ListParams>  {
                call.respond(controller.list(it))
            }
        }

    }
}