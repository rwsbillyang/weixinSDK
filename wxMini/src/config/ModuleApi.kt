/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-02-02 16:16
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

package com.github.rwsbillyang.wxSDK.wxMini.config

import com.github.rwsbillyang.ktorKit.server.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.respondBox
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import org.koin.ktor.ext.inject


fun Routing.wxMiniConfigApi() {
    val service: MiniConfigService by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    route("/api/wx/mini") {
        get("/code2Session"){
            call.respondBox(DataBox.ok(service.saveConfig(call.receive())))
        }

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


            route("/admin") {
                route("/config") {
                    get("/list"){
                        call.respondBox(DataBox.ok(service.findConfigList()))
                    }
                    get("/save"){
                        call.respondBox(DataBox.ok(service.saveConfig(call.receive())))
                    }

                    get("/del/{id}"){
                        val id = call.parameters["id"]
                        if(id == null)
                            call.respondBox(DataBox.ko<Int>("invalid parameter: no id"))
                        else
                            call.respondBox(DataBox.ok(service.delConfig(id).deletedCount))
                    }
                }


            }
        }
    }

}
