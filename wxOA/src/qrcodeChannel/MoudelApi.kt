/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-11-23 13:47
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

package com.github.rwsbillyang.wxOA.qrcodeChannel


import com.github.rwsbillyang.ktorKit.server.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.server.respondBox
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get


import org.koin.dsl.module
import org.koin.ktor.ext.inject


internal val qrCodeChannelModule = module{
    single { QrCodeChannelController() }
    single { QrCodeChannelService(get()) }

}
fun Routing.qrcodeChannelApi(){
    val controller: QrCodeChannelController by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    authenticate {
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


        route("/api/wx/admin/oa/qrcodeChannel"){
            get<ChannelListParams> {
                call.respondBox(controller.getChannelList(it))
            }
            post("/save") {
                call.respondBox(controller.saveChannel(call.receive()))
            }
            get("/generate/{channelId}") {
                call.respondBox(controller.generateChannelQrCode(call.parameters["channelId"],call.request.queryParameters["appId"]))
            }

            get("/del/{channelId}") {
                call.respondBox(controller.delChannel(call.parameters["channelId"],call.request.queryParameters["appId"]))
            }
        }
    }
}
