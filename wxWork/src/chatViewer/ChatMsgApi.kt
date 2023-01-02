/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-12-30 16:38
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

package com.github.rwsbillyang.wxWork.chatViewer


import com.github.rwsbillyang.ktorKit.server.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.server.appId

import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.wxWork.agentId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get

import org.koin.ktor.ext.inject


fun Routing.chatViewerApi() {
    val controller: ChatMsgController by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    authenticate {
        intercept(ApplicationCallPipeline.Call) {
            when (jwtHelper.isAuthorized(call)) {
                true -> proceed()
                else -> {
                    call.respond(HttpStatusCode.Forbidden)
                    return@intercept finish()
                }
            }
        }


        //ktor bug: chatViewer大写的话，get<ChatMsgListParam>报400 bad request 错误
        route("/api/wx/admin/work/chatviewer") {
            get("/syncPermitChatArchiveContacts/{refreshType}"){
                call.respondBox(controller.syncPermitChatArchiveContacts(
                    call.appId,
                    call.appId,
                    call.agentId,
                    call.parameters["refreshType"]?.toInt()))
            }

            get("/syncChatMsg"){
                val corpId = call.appId
                val agentId = call.agentId
                call.respondBox(controller.syncChatMsg(corpId, agentId, corpId))
            }

            get<ChatMsgListParam> {
                call.respondBox(controller.getChatMsgList(it))
            }
        }
    }
}
