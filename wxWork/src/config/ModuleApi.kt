/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-26 11:35
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

package com.github.rwsbillyang.wxWork.config


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



fun Routing.wxWorkConfigApi() {
    val controller: ConfigController by inject()
    val service: ConfigService by inject()
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


        route("/api/wx/admin/work") {
            route("/corp") {
                get("/list"){
                    val list = service.findCorpList()
                    call.respondBox(DataBox.ok(list))
                }
                post("/save") {
                    val doc = service.saveCorp(call.receive())
                    call.respondBox(DataBox.ok(doc))
                }

                get("/del/{id}"){
                    val id = call.parameters["id"]
                    if(id == null)
                        call.respondBox(DataBox.ko<Int>("invalid parameter: no id"))
                    else
                        call.respondBox(DataBox.ok(service.delCorp(id).deletedCount))
                }
            }

            route("/agentConfig") {
                get("/list"){
                    val corpId = call.request.queryParameters["corpId"]
                    if(corpId == null){
                        call.respondBox(DataBox.ko<Int>("invalid parameters: no corpId"))
                    }else{
                        val list = service.findWxWorkConfigByCorpId(corpId)
                        call.respondBox(DataBox.ok(list))
                    }

                }
                get("/save"){
                    call.respondBox(controller.saveWxWorkConfig(call.receive()))
                }

                get("/get/{corpId}"){
                    val corpId = call.parameters["corpId"]
                    if(corpId == null) call.respondBox(DataBox.ko<Int>("invalid parameter"))
                    else {
                        call.respondBox(DataBox.ok(controller.findWxWorkConfig(corpId)))
                    }
                }
                get("/del/{id}"){
                    val id = call.parameters["id"]
                    if(id == null)
                        call.respondBox(DataBox.ko<Int>("invalid parameter: no id"))
                    else
                        call.respondBox(DataBox.ok(service.delAgentConfig(id).deletedCount))
                }
            }


        }
    }
}
