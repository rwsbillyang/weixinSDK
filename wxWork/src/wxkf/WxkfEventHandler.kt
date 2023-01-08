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

import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.work.WxKefuApi
import com.github.rwsbillyang.wxSDK.work.WxKfSyncMsgResponse
import com.github.rwsbillyang.wxSDK.work.inMsg.DefaultWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.WxkfEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.json
import org.slf4j.LoggerFactory

class WxkfEventHandler: DefaultWorkEventHandler(), KoinComponent {
    private val log = LoggerFactory.getLogger("WxkfEventHandler")

    private val service:WxkfService by inject()
    companion object{
        const val WxkfEnterSession = "enter_session"
        const val WxkfSendFail = "msg_send_fail"
        const val WxkfServicerStatusChange = "servicer_status_change" //接待人员接待状态变更事件
        const val WxkfSessionStatusChange = "session_status_change"//会话状态变更事件
        const val WxkfUserRecallMsg = "user_recall_msg"//用户撤回消息事件
        const val WxkfServicerRecallMsg = "servicer_recall_msg"//接待人员撤回消息事件
    }

    override fun onWxkfMsgEvent(appId: String, e: WxkfEvent): ReBaseMSg? {
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
                                    it.getValue("msgtype").jsonPrimitive.content,
                                    it.getValue(msgtype).jsonObject.json
                                )
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
}