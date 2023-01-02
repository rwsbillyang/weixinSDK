/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-12-30 16:38
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

package com.github.rwsbillyang.wxWork.chatViewer

import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.wxSDK.msg.MsgType
import com.github.rwsbillyang.wxSDK.work.ContactsApi
import com.github.rwsbillyang.wxSDK.work.ExternalContactsApi
import com.github.rwsbillyang.wxSDK.work.chatMsg.*
import com.github.rwsbillyang.wxWork.contacts.ContactHelper
import com.github.rwsbillyang.wxWork.contacts.ContactService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.json
import org.slf4j.LoggerFactory


/**
 * 暂只支持单应用模式，包括ISV和企业内部的单应用
 * */
class ChatMsgController : KoinComponent {
    private val log = LoggerFactory.getLogger("ChatMsgController")

    private val fetcher: ChatMsgFetcher by inject()
    private val service: ChatMsgService by inject()

    private val contactService: ContactService by inject()
    private val contactHelper: ContactHelper by inject()

    /**
     * 从微信同步，获取企业开启会话内容存档的成员列表
     *
     * @param refreshType
     * 0为增量刷新：获取允许会话的客服userid列表以及他们的客户的userid列表，若本地无用户详情信息则获取其详情,否则不获取
     * 1为全量刷新：获取获取允许会话的客服userid列表以及他们的客户的userid列表，并且全部刷新他们的详情信息，花费时间更长
     * */
    fun syncPermitChatArchiveContacts(suiteId: String?, corpId: String?, agentId:Int?, refreshType: Int?): DataBox<Int> {
        if(corpId == null)
            return DataBox.ko("invalid parameter, no corpId") //TODO: not support isv multi

        val chatMsgApi = ChatMsgApi(corpId, agentId, suiteId)

        //type	否	拉取对应版本的开启成员列表。1表示办公版；2表示服务版；3表示企业版。非必填，不填写的时候返回全量成员列表。
        val res = chatMsgApi.getPermitUserList()//获取会话内容存档开启成员列表
        if(!res.isOK())
            return DataBox.ko(res.errMsg?:"weixin error")

        if(res.ids.isNullOrEmpty())//没有值
            return DataBox.ok(0)


        val contactsApi = ContactsApi(corpId, agentId, suiteId)
        val externalContactsApi = ExternalContactsApi(corpId, agentId, suiteId)

        var fail = 0
        when(refreshType){
            0 -> res.ids.forEach {
                if(contactService.findContact(it, corpId) == null) fail += contactHelper.syncContactDetail(contactsApi, it, corpId)
                contactHelper.syncExternalsOfUser(externalContactsApi, corpId, it, refreshType)
            }
            1 -> res.ids.forEach {
                fail += contactHelper.syncContactDetail(contactsApi, it, corpId)
                contactHelper.syncExternalsOfUser(externalContactsApi, corpId, it, refreshType)
            }
            else -> return DataBox.ko("Not support parameter: $refreshType")
        }

        if(fail > 0)
        {
            log.warn("syncPermitArchiveUserList getDetail fail=$fail")
        }
        return DataBox.ok(res.ids.size - fail)
    }

    /**
     * 同步数据，从腾讯服务器获取聊天记录
     * */
    fun syncChatMsg(corpId: String?, agentId: Int?, suiteId: String?): DataBox<Long> {
        if(corpId == null)
        {
            log.warn("invalid headers: no corpId")
            return DataBox.ko("invalid headers: no corpId")
        }
        val count = fetcher.startFetch(corpId, agentId, suiteId)
        if (count < 0L) {
            return DataBox.ko("something wrong: $count")
        }
        return DataBox.ok(count)
    }

    /**
     * 单聊聊天记录
     * */
    fun getChatMsgList(param: ChatMsgListParam): DataBox<List<SingleChatMsg>> {
        //val total = service.countChatMsgList(filter)
        log.info("param.toFilter().json=${param.toFilter().json}")
        val list = service.findChatMsgList(param).mapNotNull {
            if (it.to.isNullOrEmpty()) null else {
                SingleChatMsg(
                    it._id, it.action.name, it.res, it.type, it.time, it.from, it.to[0],
                    it.direction, it.detail, it.resUri()
                )
            }
        }
        return DataBox.ok(list)
    }

    fun countChatMsg(corpId: String, contactId: String, customerId: String) = service.countChatMsgList(ChatMsgListParam(null,corpId, contactId, customerId).toFilter())



    private fun ChatMsgRecord.resUri() = when (type) {
        MsgType.IMAGE -> {
            val content = detail as ImgContent
            ChatMsgRecord.resUri(corpId, type, content.md5, content.size.toLong(), "jpg")
        }
        MsgType.VOICE -> {
            val content = detail as VoiceContent
            ChatMsgRecord.resUri(corpId, type, content.md5, content.size.toLong(), "amr")
        }
        MsgType.VIDEO -> {
            val content = detail as VideoContent
            ChatMsgRecord.resUri(corpId, type, content.md5, content.size.toLong(), "mp4")
        }
        MsgType.EMOTION -> {
            val content2 = detail as EmotionContent
            //表情类型，png或者gif: 1表示gif 2表示png
            ChatMsgRecord.resUri(
                corpId,
                type, content2.md5, content2.size?.toLong(),
                if (content2.type == 1) "gif" else if (content2.type == 2) "png" else "jpg"
            )
        }
        MsgType.FILE -> {
            val content = detail as FileContent
            ChatMsgRecord.resUri(corpId, type, content.md5, content.size.toLong(), content.ext)
        }
        MsgType.MEETING_VOICE_CALL -> {
            val content = detail as MeetingVoiceCallContent
            //此处md5使用的是voiceId，而不是md5sum
            ChatMsgRecord.resUri(corpId, type, content.voiceId, 0L, "amr")
        }
        MsgType.VOIP_DOC_SHARE -> {
            val content2 = detail as VoipContent
            ChatMsgRecord.resUri(
                corpId,
                type, content2.voipDocShare.md5,
                content2.voipDocShare.size, "amr"
            )
        }
        else -> null
    }
}