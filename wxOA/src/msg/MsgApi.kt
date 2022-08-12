/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-11 16:51
 *
 */

package com.github.rwsbillyang.wxOA.msg


import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.respondBox
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject

internal val msgModule = module {
    single { MsgController() }
    single { MsgService(get()) }
}

/**
 * 用于被动回复消息、群发消息等的管理（增删改查）
 * */
fun Routing.msgApi(){
    val controller: MsgController by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    authenticate {
        // verify admin privileges
        intercept(ApplicationCallPipeline.Call) {
            //log.info("intercept admin save: ${call.request.uri}")
            when (jwtHelper.isAuthorized(call)) {
                false -> {
                    call.respond(HttpStatusCode.Forbidden)
                    return@intercept finish()
                }
                true -> proceed()
            }
        }


        route("/api/wx/admin/oa/msg"){
            get<MyMsgListParams>{
                call.respond(controller.findMyMsgList(it))
            }

            get("/listBy/{appId}/{flag}"){
                call.respondBox(controller.findMyMsgList(call.parameters["appId"], call.parameters["flag"]?.toLong()))
            }
            post("/save"){
                call.respondBox(controller.saveMyMsg(call.receive()))
            }

            get("/del/{id}"){
                call.respondBox(controller.delMyReMsg(call.parameters["id"]))
            }
        }
    }
}
