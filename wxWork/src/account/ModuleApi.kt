/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-04 15:03
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

package com.github.rwsbillyang.wxWork.account


import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.ktorKit.server.uId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject


fun Routing.workAccountApi() {

    val controller: WxWorkAccountController by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    route("/api/wx/work/account") {
        post("/register"){
            val rcm = call.request.queryParameters["rcm"] //推荐人 系统用户id
            call.respondBox(controller.login(call.receive(),2, null, rcm, call.request.origin.remoteHost, call.request.userAgent()))
        }

        post("/login"){
            //1: 明确要求新用户需注册为系统用户； 2 自动注册为系统用户； 其它值：无需注册系统用户（无系统账号），
            val registerType = call.request.queryParameters["registerType"]?.toInt()
            val scanQrcodeId = call.request.queryParameters["scanQrcodeId"] //非空则为扫码登录
            val rcm = call.request.queryParameters["rcm"] //推荐人
            call.respondBox(controller.login(call.receive(),registerType, scanQrcodeId, null,
                call.request.origin.remoteHost, call.request.userAgent()))
        }

        post("/bind/{uId}"){
            call.respondBox(controller.bindAccount(call.parameters["uId"], call.receive()))
        }

        /**
         * 前端需要根据用户的level进行显示控制，如文章详情页中的名片，
         * 过期后的用户统一归集到免费用户
         * */
        get("/level/{uId}"){
            val agentId = call.request.queryParameters["agentId"]?.toInt()
            call.respondBox(controller.permitLevel(call.parameters["uId"]))
        }


        authenticate {
            intercept(ApplicationCallPipeline.Call) {
                when (jwtHelper.isAuthorized(call)) {
                    false -> {
                        call.respond(HttpStatusCode.Forbidden)
                        return@intercept finish()
                    }
                    else -> proceed()
                }
            }
            route("/admin"){
                post("/updateNick") {
                    val nick = call.request.queryParameters["nick"]
                    val uId = call.uId
                    if(uId == null) call.respondBox(DataBox.ko<Long>("invalid header, no uId"))
                    else
                        call.respondBox(controller.updateNick(uId, nick))
                }

                get<WxAccountListParams>{
                    call.respondBox(controller.findWxAccountList(it))
                }


            }

        }
    }

}
