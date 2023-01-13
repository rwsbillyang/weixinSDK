/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-07 22:53
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

import com.github.rwsbillyang.ktorKit.util.utcToLocalDateTime
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.work.EnterSessionContext
import com.github.rwsbillyang.wxSDK.work.WechatChannels
import com.github.rwsbillyang.wxSDK.work.WxKefuApi
import com.github.rwsbillyang.wxSDK.work.WxKfSyncMsgResponse
import com.github.rwsbillyang.wxSDK.work.inMsg.DefaultWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.WxkfEvent
import com.github.rwsbillyang.wxWork.contacts.ContactService
import com.github.rwsbillyang.wxWork.contacts.ExternalContact
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class WxkfEventHandler: DefaultWorkEventHandler(), KoinComponent {
    private val log = LoggerFactory.getLogger("WxkfEventHandler")

    private val service:WxkfService by inject()
    private val contactService: ContactService by inject()
    companion object{
        const val WxkfEnterSession = "enter_session"
        const val WxkfSendFail = "msg_send_fail"
        const val WxkfServicerStatusChange = "servicer_status_change" //接待人员接待状态变更事件
        const val WxkfSessionStatusChange = "session_status_change"//会话状态变更事件
        const val WxkfUserRecallMsg = "user_recall_msg"//用户撤回消息事件
        const val WxkfServicerRecallMsg = "servicer_recall_msg"//接待人员撤回消息事件
    }

    override fun onWxkfMsgEvent(appId: String, e: WxkfEvent): ReBaseMSg? {
        log.info("onWxkfMsgEvent: msgType=${e.msgType}")
        val api = WxKefuApi(appId)
        val openKfId = e.openKfId
        if(openKfId == null){
            log.warn("no openKfId, do nothing")
            return null
        }

        runBlocking {
            launch {
                var res: WxKfSyncMsgResponse
                var cursor = service.findCursor(openKfId)

                do{
                    res = api.syncMsg(e.token, e.openKfId, cursor)
                    if(res.isOK()){
                        if(res.has_more == 1 && res.next_cursor != null){
                            cursor = res.next_cursor!!
                            service.upsertCursor(openKfId, cursor)
                        }
                        if(!res.msg_list.isNullOrEmpty()){
                            val list = res.msg_list!!.map {
                                val msgtype = it.getValue("msgtype").jsonPrimitive.content
                                WxkfMsg(
                                    it.getValue("msgid").jsonPrimitive.content,
                                    appId,
                                    it.getValue("open_kfid").jsonPrimitive.content,
                                    it.getValue("external_userid").jsonPrimitive.content,
                                    it.getValue("send_time").jsonPrimitive.long,
                                    it.getValue("origin").jsonPrimitive.int,
                                    it.getValue("servicer_userid").jsonPrimitive.content,
                                    msgtype,
                                    it.getValue(msgtype).jsonObject
                                )
                            }
                            launch {
                                val enters = list.filter { it.msgtype == "event" && it.content.get("event_type")?.jsonPrimitive?.content == "enter_session" } //"event_type": "enter_session",
                                if(enters.isNotEmpty()){
                                    getUserDetail(appId, enters)
                                }
                            }

                            service.upsertMsgList(list)
                        }
                    }
                }while (res.isOK() && res.has_more == 1)

                if(!res.isOK()){
                    log.warn("wxkf syncMsg: ${res.errCode} : ${res.errMsg}")
                }
            }
        }

        return null
    }

    fun getUserDetail(corpId: String, enterSessionEventList: List<WxkfMsg>){
        val externalIds1 = mutableSetOf<String>()//不存在客户信息的客户ID
        val externalIds2 = mutableSetOf<String>()//客户信息存在的客户ID
        val map = mutableMapOf<String, MutableList<EnterSessionContext>>()
        enterSessionEventList.forEach {
            val externalId = it.external_userid
            val customer = contactService.findExternalContact(externalId, corpId)
            if(customer == null){
                externalIds1.add(externalId)
            }else{
                externalIds2.add(externalId)
            }

            val enter = it.content
            val channel = enter.get("wechat_channels")?.jsonObject?.let {
                WechatChannels(
                    it.get("nickname")?.jsonPrimitive?.content?:"",
                    it.get("scene")?.jsonPrimitive?.int,
                    it.get("shop_nickname")?.jsonPrimitive?.content
                )
            }
            val enterSessionContext = EnterSessionContext(
                enter.get("scene")?.jsonPrimitive?.content?:"",
                enter.get("scene_param")?.jsonPrimitive?.content,
                channel, it.send_time.utcToLocalDateTime()
            )
            if(map[externalId] == null)
            {
                map[externalId] = mutableListOf(enterSessionContext)
            }else{
                map[externalId]!!.add(enterSessionContext)
            }
        }

        if(externalIds1.isNotEmpty()){
            //不存在，查询信息，并则只是插入enter_session信息
            val res = WxKefuApi(corpId).getCustomerDetail(externalIds1.toList())
            if(res.isOK()){
                if(!res.customer_list.isNullOrEmpty()){
                    res.customer_list!!.map {
                        ExternalContact(ObjectId(), corpId, it.external_userid, it.nickname,
                            it.avatar, it.gender, it.unionid,
                            enterSessions = map[it.external_userid], wxkf = true)
                    }.also {
                        contactService.insertExternalContacts(it)
                    }
                }else{
                    log.warn("no customer_list, invalid_external_userid: ${res.invalid_external_userid}")
                }
            }else{
                log.warn("fail getCustomerDetail: ${res.errCode}: ${res.errMsg}")
            }
        }

        if(externalIds2.isNotEmpty()){
            //已存在，则只是插入enter_session信息
            externalIds2.forEach {
                val list = map[it]
                if(!list.isNullOrEmpty()){
                    contactService.insertEnterSessionContext(it, corpId, list)
                }
            }
        }
    }
}