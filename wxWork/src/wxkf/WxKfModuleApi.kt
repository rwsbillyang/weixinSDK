/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-05 11:59
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

package com.github.rwsbillyang.wxWork.wxkf

import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.*
import com.github.rwsbillyang.wxSDK.work.WxKefuApi
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject


fun Routing.wxkfApi() {
    val controller: WxkfController by inject()
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

        route("/api/wx/admin/work/wxkf") {
            get("/syncAccountList/{corpId}"){
                val corpId = call.parameters["corpId"]
                if(corpId == null )
                    call.respondBox(DataBox.ko<Unit>("invalid parameter, corpId is null"))
                else
                    call.respondBox(controller.syncWxKfAccountList(corpId))
            }
            get("/syncServicerList/{openKfId}"){
                val openKfId = call.parameters["openKfId"]
                if(openKfId == null )
                    call.respondBox(DataBox.ko<Unit>("invalid parameter, openKfId is null"))
                else {
                    val corpId = call.appId
                    if(corpId == null){
                        call.respondBox(DataBox.ko<Unit>("invalid parameter, no appId in header"))
                    }else{
                        call.respondBoxOK(controller.syncServicesByOpenKfId(WxKefuApi(corpId),openKfId))
                    }
                }
            }

            get("/account/list/{corpId}"){
                val corpId = call.parameters["corpId"]

                if(corpId == null )
                    call.respondBox(DataBox.ko<Unit>("invalid parameter, corpId is null"))
                else
                    call.respondBox(DataBox.ok(controller.getWxkfAccountList(corpId)))
            }

            post("/scene/save"){
                call.respondBox(controller.saveScene(call.receive()))
            }
            get("/scene/list"){
                val corpId = call.request.queryParameters["corpId"]
                val kfId = call.request.queryParameters["kfId"]
                if(corpId == null )
                    call.respondBox(DataBox.ko<Unit>("invalid parameter, corpId is null"))
                else
                    call.respondBox(DataBox.ok(controller.sceneList(corpId, kfId)))
            }
            get("/scene/del/{id}"){
                val id = call.parameters["id"]
                if(id == null){
                    call.respondBoxKO("invalid parameter, id is null")
                }else{
                    call.respondBoxOK(controller.delScene(id))
                }
            }

            get<WxMsgPageParams>{
                call.respondBox(controller.getChatSessionInf(it, call.appId))
            }
            get("/external/list"){
                call.respondBox(controller.getExternalListInfo(call.appId))
            }
        }
    }
}