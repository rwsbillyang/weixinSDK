/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-05 12:20
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

package com.github.rwsbillyang.wxWork.channel

import com.github.rwsbillyang.ktorKit.*
import com.github.rwsbillyang.ktorKit.server.*
import com.github.rwsbillyang.wxWork.agentId
import com.github.rwsbillyang.wxWork.userId


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory



fun Routing.channelApi() {
    val controller: ChannelController by inject()
    val jwtHelper: AbstractJwtHelper by inject()
    val log = LoggerFactory.getLogger("channelApi")


    route("/api/wx/admin/work/channel") {
        authenticate {//在route内，而不是外面
            // verify admin privileges
            intercept(ApplicationCallPipeline.Call) {
                when (jwtHelper.isAuthorized(call)) {
                    false -> {
                        log.warn("Forbidden: jwtHelper.isAuthorized(call), url=${call.request.uri}")
                        call.respond(HttpStatusCode.Forbidden)
                        return@intercept finish()
                    }
                    else -> proceed()
                }
            }

            get<ChannelListParams> {
                call.respond(controller.getChannelList(it, call.isFromAdmin()))
            }
            post("/save") {
                call.respondBox(controller.saveChannel(call.receive()))
            }
            get("/qrcode/{channelId}") {
                call.respondBox(controller.getChannelQrCode(call.parameters["channelId"], call.appId, call.agentId))
            }
            get("/regenerateQrcode/{channelId}") {
                call.respondBox(controller.regenerateQrCode(call.parameters["channelId"], call.appId, call.agentId))
            }
            get("/del/{channelId}") {
                call.respondBox(controller.delChannel(call.parameters["channelId"], call.appId, call.agentId))
            }

        }
    }
}
