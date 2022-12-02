/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-10 16:06
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.wxOA.fan


import com.github.rwsbillyang.ktorKit.server.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.wxUser.account.Profile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject

internal val fanModule = module {
    single { FanController() }
    single { FanService(get()) }
}

fun Routing.fanApi() {
    val controller: FanController by inject()
    val fanController: FanController by inject()
    val jwtHelper: AbstractJwtHelper by inject()


    route("/api/wx/oa"){
        get("/fanInfo/uId/{uId}"){
            val uId = call.parameters["uId"]
            if(uId == null){
                call.respondBox(DataBox.ko<Profile>("no uId"))
            }else{
                call.respondBox(DataBox.ok(fanController.getProfileByUId(uId)))
            }
        }
        get("/fanInfo/get/{openId}"){
            val openId = call.parameters["openId"]
            if(openId == null){
                call.respondBox(DataBox.ko<Profile>("no openId"))
            }else{
                call.respondBox(DataBox.ok(fanController.getProfileByUId(openId)))
            }
        }
        get("/isSubscribed/{openId}"){
            val openId = call.parameters["openId"]
            call.respondBox(controller.isSubscribed(openId))
        }
    }

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



        route("/api/wx/admin/oa/fan") {
            get<FanListParams> {
                call.respond(controller.findFanList(it))
            }
            get("/get/{openId}"){
                call.respondBox(controller.findFan(call.parameters["openId"]))
            }
        }
    }
}
