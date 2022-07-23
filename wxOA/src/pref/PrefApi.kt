/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-11 16:58
 *
 */

package com.github.rwsbillyang.wxOA.pref


import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.respondBox
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.routing.get
import org.koin.dsl.module
import org.koin.ktor.ext.inject

internal val prefModule = module {
    single { PrefService(get()) }
}

/**
 * 用于
 * 1. 配置公众号基本信息
 * 2. 获取某公众号的接收事件对应的回复消息配置列表
 * 3. 添加、修改、删除回复消息配置
 * */
fun Routing.prefApi(){
    val controller: PrefController by inject()
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


        //===================公众号基本配置=====================//
        route("/api/wx/admin/oa/pref"){
            get("/basic/list"){
                call.respondBox(controller.findAllOfficialAccount())
            }
            /**
             * 根据appId获取公众号配置
             * */
            get("/basic/detail/{appId}"){
                call.respondBox(controller.findOfficialAccount(call.parameters["appId"]))
            }
            get("/basic/del/{appId}"){
                call.respondBox(controller.delOfficialAccount(call.parameters["appId"]))
            }
            /**
             * 保存公众号配置
             * */
            post("/basic/save"){
                val oa = call.receive<PrefOfficialAccount>()
                call.respondBox(controller.saveOfficialAccount(oa))
            }


            //===================公众号菜单配置=====================//
            get("/menu/list"){
                val appId = call.request.queryParameters["appId"]
                call.respondBox(controller.findPrefMenu(appId))
            }
            post("/menu/save"){
                call.respondBox(controller.savePrefMenu(call.receive()))
            }
            get("/menu/del/{id}"){
                call.respondBox(controller.delPrefMenu(call.parameters["id"]))
            }

            /**
             * cmd: create, del, get
             * */
            get("/menu/execute/{cmd}/{appId}"){
                call.respondBox(controller.executeMenuCmd(call.parameters["cmd"], call.parameters["appId"]))
            }



            //===================被动回复消息配置=====================//
            /**
             * 获取回复消息配置列表，若未在查询参数中指定cat，则表示全部
             * */
            get<PrefReMsgListParams>{
                call.respond(controller.findPrefReMsgList(it))
            }
            //获取回复消息配置列表
            get("/reMsg/list/{cat}/{appId}"){
                call.respond(controller.findPrefReMsgList(call.parameters["appId"], call.parameters["cat"]?.toInt()))
            }
            /**
             * 保存一个配置，可以是添加，可以是更新，区别在于是否有_id值
             * */
            post("/reMsg/save"){
                val param = call.receive<PrefReInMsg>()
                call.respondBox(controller.savePrefReInMsg(param))
            }
            /**
             * del 配置
             * */
            get("/reMsg/del/{id}"){
                call.respondBox(controller.delPrefReInMsg(call.parameters["id"]))
            }
        }
    }

}
