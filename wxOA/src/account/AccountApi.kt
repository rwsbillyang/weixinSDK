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


import com.github.rwsbillyang.ktorKit.server.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.wxUser.account.LoginParamBean
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject


internal fun Routing.oaUserApi() {
    val controller: WxOaAccountController by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    route("/api/wx/oa/account"){
        post("/register"){
            val loginType = call.request.queryParameters["loginType"]?: LoginParamBean.WECHAT
            val scanQrcodeId = call.request.queryParameters["scanQrcodeId"]
            val rcm = call.request.queryParameters["rcm"] //推荐人 系统用户id
            val registerType = 2// 1: 明确要求新用户需注册为系统用户； 2 自动注册为系统用户； 其它值：无需注册系统用户（无系统账号）
            call.respondBox(controller.login(call.receive<WxOaGuest>(), loginType,
                scanQrcodeId, registerType, rcm,
                call.request.origin.remoteHost, call.request.userAgent()))
        }
//        post("/isUser"){
//            call.respondBox(controller.isUser(call.receive()))
//        }

        post("/login"){
            val loginType = call.request.queryParameters["loginType"]?: LoginParamBean.WECHAT
            val scanQrcodeId = call.request.queryParameters["scanQrcodeId"]
            val rcm = call.request.queryParameters["rcm"] //推荐人
            val registerType = call.request.queryParameters["registerType"]?.toInt()
            call.respondBox(controller.login(call.receive<WxOaGuest>(),loginType,
                scanQrcodeId, registerType, rcm,
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

                get<WxOaAccountListParams>{
                    call.respondBox(controller.findAccountList(it))
                }
            }

        }
    }
}


