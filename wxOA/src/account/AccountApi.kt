/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-09-23 10:57
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.wxOA.account


import com.github.rwsbillyang.ktorKit.*
import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.wxUser.account.AccountListParams
import com.github.rwsbillyang.wxUser.account.Group
import com.github.rwsbillyang.wxUser.account.GroupListParams

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.routing.get

import org.koin.ktor.ext.inject



internal fun Routing.oaUserApi() {
    val controller: AccountControllerOA by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    route("/api/wx/oa/account"){
        post("/register"){
            //val rcm = call.request.queryParameters["rcm"] //推荐人 系统用户id
            call.respondBox(controller.register(call.receive(),call.request.origin.remoteHost, call.request.userAgent()))
        }
        post("/isUser"){
            call.respondBox(controller.isUser(call.receive()))
        }

        post("/login"){
            call.respondBox(controller.login(call.receive(),call.request.origin.remoteHost, call.request.userAgent()))
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


