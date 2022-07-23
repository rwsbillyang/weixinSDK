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

package com.github.rwsbillyang.wxWork.agent


import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.respondBox
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val agentModule = module {
    single { AgentController() }
    single { AgentService(get()) }

}


fun Routing.agentApi() {
    val controller: AgentController by inject()
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

        route("/api/wx/work/agent") {
            get("/sync/{corpId}/{agentId}"){
                val corpId = call.parameters["corpId"]
                val agentId = call.parameters["agentId"]?.toInt()
                if(corpId == null || agentId == null)
                    call.respondBox(DataBox.ko<Unit>("invalid parameter, corpId or agentId is null"))
                else
                    call.respondBox(DataBox.ok(controller.syncAgent(corpId, agentId)))
            }

            
        }
    }
}
