/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-10-08 14:06
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

package com.github.rwsbillyang.wxUser.account.webAdmin

import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*


import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException

import org.koin.dsl.module

import org.koin.ktor.ext.inject


val wxWebAdminAppModule = AppModule(
    listOf(
        module {
            single { AccountControllerWebAdmin(get(), get()) }
            single { AccountServiceWebAdmin(get()) }
            single { WsSessions() }
        }
    ), "user")
{
    webUserAdminApi()
}
/**
 * 传统web网页用户登录以及系统管理后台的api
 * */
internal fun Routing.webUserAdminApi() {
    //val log = LoggerFactory.getLogger("userApi")
    val wsSessions: WsSessions by inject()
    val controller: AccountControllerWebAdmin by inject()
    val jwtHelper: AbstractJwtHelper by inject()
    route("/api/u"){

        post("/register"){
            val rcm = call.request.queryParameters["rcm"] //推荐人 系统用户id
            call.respondBox(controller.register(call.receive(),rcm,call.request.origin.remoteHost, call.request.userAgent()))
        }
        post("/isUser"){
            call.respondBox(controller.isUser(call.receive()))
        }

        post("/login"){
            call.respondBox(controller.login(call.receive(),call.request.origin.remoteHost, call.request.userAgent()))
        }
        get("/code"){
            call.respondBox(controller.code(call.request.queryParameters["phone"]))
        }
        get("resetPwd"){
            call.respondBox(controller.code(call.request.queryParameters["email"]))
        }

        //图片验证
        get("/captcha"){
            call.respondBox(DataBox.ko<Unit>("not implement"))
        }

        //扫码登录时，用户取消了登录
        get("/cancelQrcodeLogin"){
            val id = call.request.queryParameters["id"]
            if(id == null)
                call.respondBox(DataBox.ko<Int>("invalid parameter"))
            else{
                wsSessions.session(id)?.send("cancelQrcodeLogin")
                wsSessions.removeSession(id)
                call.respondBox(DataBox.ok(1))
            }
        }
        //PC端建立wsSocket连接，开始扫码登录，PC端首先发送"getId"，后端回复 "id=$id"，超时时前端发送"bye, id=$id"
        //登录成功后发送认证JSON字符串
        webSocket("/scanQrcodeLogin") {
            try {
                for (frame in incoming){
                    val text = (frame as Frame.Text).readText()
                    if(text == "getId"){
                        val id = wsSessions.addSession(this)
                        send("id=$id") //outgoing.send(Frame.Text(text))
                    }else if(text.startsWith("bye")){
                        val id = text.substringAfter("=")
                        wsSessions.removeSession(id)
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                println("onClose ${closeReason.await()}")
            } catch (e: Throwable) {
                println("onError ${closeReason.await()}")
                e.printStackTrace()
            }
        }
    }
        authenticate {
            intercept(ApplicationCallPipeline.Call) {
                //log.info("intercept admin save: ${call.request.uri}")
                when(jwtHelper.isAuthorized(call)){
                    false ->{
                        call.respond(HttpStatusCode.Forbidden)
                        return@intercept finish()
                    }
                    else -> proceed()
                }
            }

            route("/api/u/admin"){
                get("/outLogin") {
                    call.respondBox(DataBox.ok<Unit>(null))
                }

                //antd admin 后台需要profile信息，返回为空的话则导致重新登录
                get("/profile") {
                    call.respondBox(controller.findProfile(call.uId))
                }

                get("/list"){
                    call.respondBox(DataBox.ko<Unit>("not implement"))
                }

                get("/notices"){
                    call.respondBox(DataBox.ko<Unit>( "not implement"))
                }
            }
        }


}
