/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-30 20:19
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

package com.github.rwsbillyang.wxUser.product

import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.respondBox

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

import org.koin.ktor.ext.inject




fun Routing.productApi() {
    val service: ProductService by inject()
    val jwtHelper: AbstractJwtHelper by inject()

    get("/api/sale/price/list") {
        val tag = call.request.queryParameters["tag"]
        val type = call.request.queryParameters["type"]?.toInt() ?: 0
        val status = call.request.queryParameters["status"]?.toInt()
        call.respondBox(DataBox.ok(service.list(tag, type, status)))
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
        route("/api/sale/admin/price") {
            post("/save")
            {
                call.respondBox(DataBox.ok(service.add(call.receive()).insertedId))
            }
            post("/update") {
                call.respondBox(DataBox.ok(service.update(call.receive())?.modifiedCount))
            }
            post("/del/{id}") {
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respondBox(DataBox.ko<Unit>("invalid parameter"))
                } else {
                    call.respondBox(DataBox.ok(service.del(id).deletedCount))
                }
            }
        }

    }


}