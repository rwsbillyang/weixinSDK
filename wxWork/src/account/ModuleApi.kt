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


import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.respondBox
import com.github.rwsbillyang.ktorKit.uId
import com.github.rwsbillyang.wxUser.account.AccountListParams
import com.github.rwsbillyang.wxUser.account.Group
import com.github.rwsbillyang.wxUser.account.GroupListParams
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

    val controller: AccountControllerWork by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    route("/api/wx/work/account") {
        post("/register"){
            //val rcm = call.request.queryParameters["rcm"] //推荐人 系统用户id
            call.respondBox(controller.register(call.receive(), call.request.origin.remoteHost, call.request.userAgent()))
        }

        post("/login"){
            //为1时需要进行注册及同意用户协议，默认不需要
            val needRegister = call.request.queryParameters["needRegister"]?.toInt()?:0
            val scanQrcodeId = call.request.queryParameters["scanQrcodeId"] //非空则为扫码登录

            call.respondBox(controller.login(call.receive(),call.request.origin.remoteHost, call.request.userAgent(), needRegister, scanQrcodeId))
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
            call.respondBox(controller.permittedLevel(call.parameters["uId"], agentId))
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
                post("/updateExt") {
                    val ext = call.receive<String>()
                    val uId = call.uId
                    if(uId == null) call.respondBox(DataBox.ko<Long>("invalid header, no uId"))
                    else
                        call.respondBox(controller.updateExt(uId, ext))
                }
                post("/updateNick") {
                    val nick = call.request.queryParameters["nick"]
                    val uId = call.uId
                    if(uId == null) call.respondBox(DataBox.ko<Long>("invalid header, no uId"))
                    else
                        call.respondBox(controller.updateNick(uId, nick))
                }

                get<AccountListParams>{
                    call.respondBox(controller.findAccountList(it))
                }
                route("/group"){
                    get<GroupListParams>{
                        call.respondBox(DataBox.ok(controller.findGroupList(it)))
                    }
                    get("/get/{id}"){
                        val id = call.parameters["id"]
                        if(id == null)
                            call.respondBox(DataBox.ko<Group>("invalid parameter: no id"))
                        else
                            call.respondBox(DataBox.ok(controller.findGroup(id)))
                    }
                    post("/save") {
                        val doc = controller.saveGroup(call.receive())
                        call.respondBox(DataBox.ok(doc))
                    }

                    get("/del/{id}"){
                        val id = call.parameters["id"]
                        if(id == null)
                            call.respondBox(DataBox.ko<Int>("invalid parameter: no id"))
                        else
                            call.respondBox(DataBox.ok(controller.delGroup(id).deletedCount))
                    }

                    get("/join/{groupId}"){
                        val groupId = call.parameters["groupId"]
                        val uId = call.uId
                        if(groupId == null || uId == null)
                            call.respondBox(DataBox.ko<Int>("invalid parameter: no groupId or uId"))
                        else
                            call.respondBox(DataBox.ok(controller.joinGroup(uId, groupId)))
                    }
                }

            }

        }
    }

}
