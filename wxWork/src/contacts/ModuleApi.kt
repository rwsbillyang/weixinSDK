/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-21 11:37
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

package com.github.rwsbillyang.wxWork.contacts

import com.github.rwsbillyang.ktorKit.server.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.corpId
import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.ktorKit.server.uId
import com.github.rwsbillyang.wxUser.agentId
import com.github.rwsbillyang.wxUser.fakeRpc.FanInfo
import com.github.rwsbillyang.wxUser.suiteId
import com.github.rwsbillyang.wxUser.userId
import com.github.rwsbillyang.wxWork.fakeRpc.FanRpcWork
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val contactModule = module {
    single { ContactController() }
    single { ContactHelper() }
    single { ContactService(get()) }

}


fun Routing.contactApi() {
    val controller: ContactController by inject()
    val jwtHelper: AbstractJwtHelper by inject()
    val fanRpc: FanRpcWork by inject()

    get("/api/wx/admin/work/contact/fanInfo/uId/{uId}"){
        val uId = call.parameters["uId"]
        if(uId == null){
            call.respondBox(DataBox.ko<FanInfo>("no uId"))
        }else{
            call.respondBox(DataBox.ok(fanRpc.getFanInfoByUId(uId)))
        }
    }
    authenticate {
        intercept(ApplicationCallPipeline.Call) {
            when (jwtHelper.isAuthorized(call)) {
                false -> {
                    call.respond(HttpStatusCode.Forbidden)
                    return@intercept finish()
                }
                else -> proceed()
            }
        }


        route("/api/wx/admin/work/contact") {

            get<ContactListParams> {
                call.respondBox(controller.getSupportChatArchiveContactList(it))
            }

            //通过_id获取某个成员的详情
            get("/detail/{id}"){
                call.respondBox(controller.getContactDetail(call.parameters["id"]))
            }

            //通过_id获取某个外部客户的详情
            get("/external/detail/{id}"){
                call.respondBox(controller.getExternalDetail(call.parameters["id"]))
            }
            get("/external/relationChanges/{externalId}"){
                call.respondBox(controller.getRelationChanges(call.parameters["externalId"], call.userId, call.corpId))
            }
            // 同步当前登录用户的所有外部联系人列表
            get("/syncExternalContact/{refreshType}"){
                //val userId = call.request.queryParameters["userId"]
                call.respondBox(controller.syncExternalsOfUser(call.corpId, call.agentId, call.suiteId, call.uId, call.parameters["refreshType"]?.toInt()))
            }

            get<ExternalListParams>{
                call.respondBox(controller.getExternalListByPage(it, call.uId, call.corpId))
            }

            get("/syncDepartment"){
                val agentId = call.request.queryParameters["agentId"]?.toInt()
                call.respondBox(controller.syncDepartment(call.corpId, agentId))
            }
        }
    }
}
