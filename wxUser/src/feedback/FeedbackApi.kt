/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-28 13:06
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

package com.github.rwsbillyang.wxUser.feedback


import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.server.respondBox
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

import io.ktor.server.routing.*
import org.koin.ktor.ext.inject


fun Routing.feedbackApi(){
    val controller: FeedbackController by inject()
    val jwtHelper: AbstractJwtHelper by inject()


    route("/api/feedback/"){
        post("/save") {
            controller.saveFeedback(call.receive())
            call.respondBox(DataBox.ok<Int>(1))
        }

        authenticate{
            intercept(ApplicationCallPipeline.Call) {
                when(jwtHelper.isAuthorized(call, listOf("admin"))){
                    true -> proceed()//role: admin
                    else ->{
                        call.respond(HttpStatusCode.Forbidden)
                        return@intercept finish()
                    }
                }
            }
            post("/admin/reply") {
                val bean = call.receive<ReplyBean>()
                call.respondBox(DataBox.ok(controller.reply(bean.id, bean.reply).modifiedCount))
            }
        }
    }


}