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

import com.github.rwsbillyang.ktorKit.util.utcSecondsToLocalDateTime
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.time.LocalDateTime

class WxkfEventHandler : DefaultWorkEventHandler(), KoinComponent {
    private val log = LoggerFactory.getLogger("WxkfEventHandler")

    private val service: WxkfService by inject()
    private val contactService: ContactService by inject()

    companion object {
        const val WxkfEnterSession = "enter_session"
        const val WxkfSendFail = "msg_send_fail"
        const val WxkfServicerStatusChange = "servicer_status_change" //接待人员接待状态变更事件
        const val WxkfSessionStatusChange = "session_status_change"//会话状态变更事件
        const val WxkfUserRecallMsg = "user_recall_msg"//用户撤回消息事件
        const val WxkfServicerRecallMsg = "servicer_recall_msg"//接待人员撤回消息事件
    }
    //onWxkfMsgEvent: xml=<xml><ToUserName><![CDATA[wwfc2fead39b1e60dd]]></ToUserName><CreateTime>1673762225</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[kf_msg_or_event]]></Event><Token><![CDATA[ENCCbxHCSFXmjW4MDKD1z2DP2F1cn6XJnWzQEkkfqmXseuQ]]></Token><OpenKfId><![CDATA[wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w]]></OpenKfId></xml>
    override fun onWxkfMsgEvent(appId: String, e: WxkfEvent): ReBaseMSg? {
        log.info("onWxkfMsgEvent: xml=${e.xml}")
        val api = WxKefuApi(appId)
        val openKfId = e.openKfId
        if (openKfId == null) {
            log.warn("no openKfId, do nothing")
            return null
        }

        runBlocking {
            launch {
                var res: WxKfSyncMsgResponse
                var cursor = service.findCursor(openKfId)

                do {
                    //{"errcode":0,"errmsg":"ok","next_cursor":"4gw7MepFLfgF2VC5npd","msg_list":[{"msgid":"4rJQ5Bspe1Ft4FGsbbGiCnSWHHkkX3CQ3DadYwvhXMQC","send_time":1673578055,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"iIShBMyC1CJbonqJx16yjRtKSHzXHbTSJzMoA1Di-VA","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"4WmUMDx6HgwfSXzkmTBZjgPPDsnNN5B7YMHUhMVv3aWC","send_time":1673579617,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"6z6ju2dGucXHJ2WO1ggSv5L7OmG1t72VEq3qURQcUGs","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"7d2uv6UpjKHAGoEGbDk431GN5DsTtqy3mLEjFFLGDZ3J","send_time":1673581267,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"lj8_dNRP-QacJmV3C336mhJBPSGRsNyQVyU-vOg5T8Q","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"MXbuYYJYGqC7TXxWEPWqY4JzF3XKGKkkaQYWMnU8p","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","send_time":1673582683,"origin":3,"msgtype":"text","text":{"content":"几"}},{"msgid":"4rFuqvAWXpKUtviTNnu2afUF9gb1bh6myU3hSeee8U5z","send_time":1673624501,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"DiDoSXaUDrG4UUZtaN1kXqd7l1VXIl05YEylHKEeSjU","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"4SwWz4ZcVW319DYmXyoAEt3BPVgJe4jYzoWcDsNvYs75","send_time":1673628265,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"c9sGj_FMnp_79VnWmMtZfRuQVN7zvcak_8MvJ9KNrWU","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"4SujPPz61jCizLz18rTvQRkTZc1SDcFdvrHaKPGPBrGR","send_time":1673677062,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"E4Bon0mQXOL-bEIPCb62sfrKbYBhpz_BJ4jH2vZR5iI","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"7thDb2qEQjnLNAe1MV5TMYNmkGVdwVqfXEJ5TWZauawJ","send_time":1673677825,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"gCNQdVbdPz-nKuQA20YW3iQ_alkwk8TJgGvIF-7RDZc","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"4KkxQ9BS12bEh6q64RoY3SvTpW7EMS1UyathasBs7upZ","send_time":1673685738,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"Oox5DXRp-YCDZpwiN3gDbS9OujvCPdlnrBJhvPp7GMc","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"4NwwaEkrws3n2ucUixJB2ReskjHi68ep9ZBk8Puvdtoq","send_time":1673686303,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"AbejttHgwols4555DlGE15Gk7tu9OJyShca4E-2Hk4Y","scene_param":"%E5%9C%BA%E6%99%AF3"}},{"msgid":"5XY51m5tZLvyTVSW2AnyMAe8wV7kFfSgi9kVGoRLU2jn","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","send_time":1673746631,"origin":5,"servicer_userid":"YangZhangGang","msgtype":"text","text":{"content":"hi"}},{"msgid":"MXbuYYJYGqC7TXxW3yiDZMzkBUKBvXVFXpXNGNYa7","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","send_time":1673751175,"origin":3,"msgtype":"text","text":{"content":"hi"}},{"msgid":"MXbuYYJYGqC7TXxWnNJZqw2DkjaP29WPCA3s4zpKi","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","send_time":1673752711,"origin":3,"msgtype":"text","text":{"content":"哈哈😄  "}},{"msgid":"MXbuYYJYGqC7TXxW3c4UZkifUxXV7TQPZ9Sy6mbP5","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","send_time":1673754219,"origin":3,"msgtype":"text","text":{"content":"呵呵"}}],"has_more":0}
                    res = api.syncMsg(e.token, e.openKfId, cursor)
                    if (res.isOK()) {
                        if (res.next_cursor != null) {
                            cursor = res.next_cursor!!
                            log.info("upsertCursor: cursor=$cursor")
                            service.upsertCursor(openKfId, cursor)
                            log.info("upsertCursor done")
                        }
                        if (!res.msg_list.isNullOrEmpty()) {
                            handlMsgList(appId, res.msg_list!!, e.openKfId)
                        }
                    }else{
                        log.warn("wxkf syncMsg: ${res.errCode} : ${res.errMsg}")
                    }
                } while (res.isOK() && res.has_more == 1)
            }
        }

        return null
    }

    private fun handlMsgList(appId: String, list: List<JsonObject>, eventKfId: String?) {
        val list2 = list.mapNotNull {
            val msgId = it["msgid"]?.jsonPrimitive?.content
            if (msgId == null) {
                log.warn("msgid is null?")
                null
            } else {
                val msgtype = it["msgtype"]?.jsonPrimitive?.content
                val content = if (msgtype != null) it[msgtype]?.jsonObject else null
                val contentStr = content?.let { Json.encodeToString(it) }
                WxkfMsg(
                    msgId, appId,
                    it["send_time"]?.jsonPrimitive?.long,
                    it["origin"]?.jsonPrimitive?.int,
                    msgtype,
                    content,
                    contentStr,
                    it["open_kfid"]?.jsonPrimitive?.content ?: eventKfId,
                    it["external_userid"]?.jsonPrimitive?.content,
                    it["servicer_userid"]?.jsonPrimitive?.content
                )
            }
        }

        if(list2.isEmpty()){
            log.warn("WxkfMsg list is empty, ignore")
            return
        }

        try {
            log.info("insert WxkfMsg list, size=${list2.size}")
            service.insertMsgList(list2)
            log.info("insert WxkfMsg list done")
        } catch (e: Exception) {
            e.printStackTrace()
            log.warn("fail to insertMsgList: " + e.message)
        }

        val enterMsgs = list2.filter {
            it.msgtype == "event"
                    && WxkfEnterSession == it.json?.get("event_type")?.jsonPrimitive?.content
        }

        if (enterMsgs.isNotEmpty()) {
            getUserDetail(appId, enterMsgs)
        }
    }

    //更新用户信息，并记录下enterSession信息
    fun getUserDetail(corpId: String, enterSessionMsgList: List<WxkfMsg>) {
        val externalIds1 = mutableSetOf<String>()//不存在客户信息的客户ID
        val externalIds2 = mutableSetOf<String>()//客户信息存在的客户ID
        val map = mutableMapOf<String, MutableList<EnterSessionContext>>()
        log.info("insert enterSessionMsgList size=${enterSessionMsgList.size}")
        enterSessionMsgList.forEach {
            //val enter = it.json!! //通过校验WxkfEnterSession必然非空
            val enterSessionContext = json2EnterSessionCtx(it.json!!, it.send_time)

            val externalId = enterSessionContext.external_userid
            if (externalId != null) {
                if(externalIds1.contains(externalId) || externalIds2.contains(externalId))
                {
                    //do nothing
                }else{
                    val customer = contactService.findExternalContact(externalId, corpId)
                    log.info("findExternalContact done, externalId=${externalId}")
                    if (customer == null) {
                        externalIds1.add(externalId)
                    } else {
                        externalIds2.add(externalId)
                    }
                }

                if (map[externalId] == null) {
                    map[externalId] = mutableListOf(enterSessionContext)
                } else {
                    map[externalId]!!.add(enterSessionContext)
                }
            } else {
                log.warn("no externalId in enter_session ctx?")
            }
        }

        if (externalIds1.isNotEmpty()) {
            log.info("getCustomerDetail, externalIds1=${externalIds1.joinToString(",")}")
            //不存在，查询信息，并则只是插入enter_session信息
            val res = WxKefuApi(corpId).getCustomerDetail(externalIds1.toList())
            if (res.isOK()) {
                if (!res.customer_list.isNullOrEmpty()) {
                    res.customer_list!!.map {
                        ExternalContact(
                            ObjectId(), corpId, it.external_userid, it.nickname,
                            it.avatar, it.gender, it.unionid,
                            enterSessions = map[it.external_userid], wxkf = true
                        )
                    }.also {
                        try{
                            log.info("insertExternalContacts, size=${it.size}")
                            contactService.insertExternalContacts(it)
                            log.info("insertExternalContacts done")
                        }catch(e: Exception){
                            log.info("fail to insertExternalContacts: " + e.message)
                        }
                    }
                } else {
                    log.warn("no customer_list, invalid_external_userid: ${res.invalid_external_userid}")
                }
            } else {
                log.warn("fail getCustomerDetail: ${res.errCode}: ${res.errMsg}")
            }
        }

        if (externalIds2.isNotEmpty()) {
            log.info("insertEnterSessionContext...")
            //已存在，则只是插入enter_session信息
            externalIds2.forEach {
                val list = map[it]
                if (!list.isNullOrEmpty()) {
                    try{
                        log.info("insertEnterSessionContext, size=${list.size}")
                        contactService.insertEnterSessionContext(it, corpId, list)
                        log.info("insertEnterSessionContext done")
                    }catch(e: Exception){
                        log.info("Fail to insertEnterSessionContext: " + e.message)
                    }
                }
            }
        }
    }

    //json
    //{"event_type":"enter_session",
    // "scene":"oamenu3",
    // "open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w",
    // "external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog",
    // "welcome_code":"8EW1TOYcpSP2tGD2UEWpXSM3gZS3qDHs7X2fwtk_oTc",
    // "scene_param":"%E5%9C%BA%E6%99%AF3"}
    private fun json2EnterSessionCtx(json: JsonObject, sendTime: Long?): EnterSessionContext {
        //log.info("parse WechatChannels, json=${Json.encodeToString(json)}")
        val channel = json["wechat_channels"]?.jsonObject?.let {
            WechatChannels(
                it["nickname"]?.jsonPrimitive?.content ?: "",
                it["scene"]?.jsonPrimitive?.int,
                it["shop_nickname"]?.jsonPrimitive?.content
            )
        }
        //log.info("parse WechatChannels done, setup EnterSessionContext...")
        val ctx = EnterSessionContext(
            json["scene"]?.jsonPrimitive?.content ?: "",
            json["scene_param"]?.jsonPrimitive?.content?.let {
                URLDecoder.decode(it, "UTF-8")
            },
            json["open_kfid"]?.jsonPrimitive?.content, //客服帐号ID
            json["external_userid"]?.jsonPrimitive?.content, //客户UserID
            json["welcome_code"]?.jsonPrimitive?.content,
            channel, sendTime?.utcSecondsToLocalDateTime() ?: LocalDateTime.now()
        )
        //log.info("parse EnterSessionContext done")
        return ctx
    }
}