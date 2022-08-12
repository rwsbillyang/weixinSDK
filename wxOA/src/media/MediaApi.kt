/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-21 21:07
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

package com.github.rwsbillyang.wxOA.media

import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.respondBox
import com.github.rwsbillyang.wxSDK.officialAccount.MediaType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import org.koin.dsl.module
import org.koin.ktor.ext.inject


internal val mediaModule = module {
    single { MediaController() }
    single { MediaService(get()) }
}

/**
 * 用于被动回复消息、群发消息等的管理（增删改查）
 * */
fun Routing.mediaApi() {
    val controller: MediaController by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    authenticate {
        intercept(ApplicationCallPipeline.Call) {
            when (jwtHelper.isAuthorized(call)) {
                false -> {
                    call.respond(HttpStatusCode.Forbidden)
                    return@intercept finish()
                }
                true -> proceed()
            }
        }


        route("/api/wx/admin/oa/material") {
            get<MaterialNewsListParams> {
                call.respond(controller.findMaterialNewsList(it))
            }
            get<MaterialVideoListParams> {
                call.respond(controller.findMaterialVideoList(it))
            }
            get<MaterialMediaListParams> {
                call.respond(controller.findMaterialMediaList(it))
            }

            get("/all/{type}") {
                val type = call.parameters["type"]
                if (type == null) {
                    call.respondBox(DataBox.ko<Unit>("invalid parameter: no type"))
                } else {
                    when (type) {
                        MediaType.NEWS.value -> call.respondBox(controller.findAllNews())
                        MediaType.VIDEO.value -> call.respondBox(controller.findAllVideo())
                        else -> call.respondBox(controller.findAllByType(type))
                    }
                }
            }

            get("/sync/{type}") {
                val appId = call.request.queryParameters["appId"]
                val type = call.parameters["type"]
                if (type == null) {
                    call.respondBox(DataBox.ko<Unit>("invalid parameter: no type"))
                } else
                    call.respondBox(controller.syncMaterials(appId, type))
            }

            post("/del/{type}/{id}/{delType}"){
                val type = call.parameters["type"]
                val id = call.parameters["id"]
                val delType = call.parameters["delType"]?.toInt()?:0
                val appId = call.request.queryParameters["appId"]
                if (type == null || id == null) {
                    call.respondBox(DataBox.ko<Unit>("invalid parameter: no type or no id"))
                } else
                    call.respondBox(controller.del(appId,type,id, delType))
            }


        }
    }
}
