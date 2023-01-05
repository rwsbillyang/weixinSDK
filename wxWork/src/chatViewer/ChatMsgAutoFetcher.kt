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


import com.github.rwsbillyang.ktorKit.util.NginxStaticRootUtil
import com.github.rwsbillyang.wxSDK.accessToken.ITimelyRefreshValue


import com.github.rwsbillyang.wxSDK.msg.MsgType
import com.github.rwsbillyang.wxSDK.work.SysAgentKey
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.chatMsg.*
import com.github.rwsbillyang.wxWork.config.ConfigService
import com.github.rwsbillyang.wxWork.isv.CorpInfo
import com.github.rwsbillyang.wxWork.isv.IsvCorpService

import com.tencent.wework.Finance
import it.justwrote.kjob.InMem
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.kjob
import it.justwrote.kjob.kron.Kron
import it.justwrote.kjob.kron.KronModule
import org.apache.commons.codec.digest.DigestUtils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

/**
 * 暂只支持单应用模式，包括ISV和企业内部的单应用
 * 将企业微信会话存档so放置在系统路径：LD_LIBRARY_PATH指定的路径内，或者将so所在的目录加入到LD_LIBRARY_PATH的路径范围内。
 * lib.so download: https://developer.work.weixin.qq.com/document/path/91774
 * */
class ChatMsgScheduleTask : KoinComponent {
    private val log = LoggerFactory.getLogger("ChatMsgScheduleTask")
    private val configService: ConfigService by inject()
    private val isvCorpService: IsvCorpService by inject()
    private val fetcher: ChatMsgFetcher by inject()

    //https://github.com/justwrote/kjob
    //https://github.com/justwrote/kjob/blob/master/kjob-example/src/main/kotlin/Example_Kron.kt
    //0 0 2 * * ?   每天UTC时间18点(北京时间2点) 触发  https://www.cnblogs.com/javahr/p/8318728.html
    object Fetcher : KronJob("ChatRecordFetcher", "0 0 18 * * ?")

    private val job = kjob(InMem) {
        extension(KronModule) // enable the Kron extension
    }.start()

    init {
        job(Kron).kron(Fetcher) {
            //maxRetries = 3 // and you can access the already familiar settings you are used to
            execute {
                log.info("execute schedule task...")
                if(Work.isIsv){
                    isvCorpService.findCorpInfoListOnlySuiteIdCorpId(CorpInfo.STATUS_ENABLED).forEach {
                        fetcher.startFetch(it.corpId, null, it.suiteId)
                    }
                }else{
                    configService.findAgentConfigList().forEach {
                        if (it.enable) {
                            fetcher.startFetch(it.corpId, it.agentId, null)
                        }
                    }
                }
            }
        }
    }

    fun onShutdown() {
        job.shutdown()
        Finance.DestroySdk(fetcher.sdk)
    }
}

/**
 * 后台开启会话内容存档，开启前需设置开启范围、IP地址（122.114.172.130）及消息加密公钥（注：员工的企业微信需升级到2.8.9版本及以上）
 * 建议使用openssl 来生成rsa pkcs1 2048格式的公私钥。例如可使用：openssl genrsa -out private.pem 2048 来生成私钥，
 * 并使用openssl rsa -in private.pem -pubout -out public.pem从私钥来产生公钥。
 *  corpId 企业微信管理端--我的企业--企业信息查看 聊天内容存档的Secret，可以在企业微信管理端--管理工具--聊天内容存档查看
 * */
class ChatMsgFetcher : KoinComponent {
    private val log = LoggerFactory.getLogger("ChatMsgFetcher")

    //private val fetchingFlag: AtomicBoolean = AtomicBoolean(false)

    private val chatMsgService: ChatMsgService by inject()

    val sdk: Long = Finance.NewSdk()


    fun startFetch(corpId: String, agentId: Int?, suiteId: String?): Long {
        log.info("startFetch...corpId=$corpId")

        val lock: ITimelyRefreshValue ? = ChatMsgApi.timelyRefreshAccessToken(SysAgentKey.ChatArchive.name, corpId, agentId, suiteId)
        if(lock == null){
            log.warn("no ITimelyRefreshValue accessToken, return -1")
            return -1L
        }

        //上锁的目的：对于同一个企业来说，最多只有一次从服务器获取，目的在于避免seq交叉更新导致的错误，另也无必要同时更新获取
        synchronized(lock){
            val chatMsgApi = ChatMsgApi(null, agentId, suiteId)

            val secret = chatMsgApi.secret
            if(secret == null)
            {
                log.warn("no secret, return -2")
                return -2L
            }
            val ret = Finance.Init(sdk, corpId, secret)
            if (ret != 0) {
                log.warn("Finance.Init ret=$ret")
                return -3L
            }
            val seqInstance = chatMsgService.findSeq(corpId)
            val lastSeq = seqInstance?.seq ?: 0

            return fetchLoop(chatMsgApi, corpId, lastSeq)
        }
    }

//    fun startFetchNonIsvMulti(corpId: String): Long {
//        log.info("startFetchNonIsvMulti...corpId=$corpId")
//        WorkMulti.ApiContextMap[corpId]?.let {
//            synchronized(it) {
//                val chatAgents = it.agentMap.filter { it.value.privateKey != null }.values
//                if (chatAgents.isNullOrEmpty()) {
//                    log.warn("no chat agent")
//                    return -2L
//                } else if (chatAgents.size > 1) {
//                    log.warn("only one chatArchive is supported, now size=${chatAgents.size}, corpId=$corpId")
//                }
//
//                val chatAgent = chatAgents.first()
//
//                val ret = Finance.Init(sdk, corpId, chatAgent.secret)
//                if (ret != 0) {
//                    log.warn("Finance.Init ret=$ret")
//                    return -3L
//                }
//                val seqInstance = chatMsgService.findSeq(corpId)
//                val lastSeq = seqInstance?.seq ?: 0
//
//                return fetchLoop(corpId, chatAgent.agentId, lastSeq)
//            }
//        }
//
//        log.warn("not config ApiContextMap for corpId=$corpId")
//        return -4L
//    }

    private fun fetchLoop(chatMsgApi: ChatMsgApi, corpId: String, lastSeq: Long, onlyOnce: Boolean = false): Long {
        var max = lastSeq
        var loop = true

        while (loop) {
            // 从指定的seq开始拉取消息，注意的是返回的消息从seq+1开始返回，seq为之前接口返回的最大seq值。首次使用请使用seq:0
            val list = chatMsgApi.getChatMsgList(sdk, lastSeq)?.map { it.toRecord(corpId) }
            if (list == null) {
                log.warn("some error when GetChatData in SDK")
                return -5L
            }


            list.forEach {
                val seq = handleChatMsg(chatMsgApi, it, corpId)
                if (seq == null) {
                    log.warn("save msg fail, break;")
                    if (max > lastSeq) chatMsgService.updateSeq(corpId, max)
                    return -6L
                }
                if (seq > max) max = seq
            }

            if (list.size < ChatMsgApi.CHAT_MSG_MAX_LIMIT || onlyOnce) {
                loop = false
                if (max > lastSeq) {
                    log.info("updateSeq: corpId=${corpId}, max=${max}")
                    chatMsgService.updateSeq(corpId, max)
                }
            }
        }
        return max - lastSeq
    }


    /**
     * 保存记录，并下载相关文件
     * 成功则返回seq，否则返回null
     * */
    private fun handleChatMsg(chatMsgApi: ChatMsgApi, msg: ChatMsgRecord, corpId: String): Long? {
        val ret = when (val type = msg.type) {
            MsgType.IMAGE -> {
                val content = msg.detail as ImgContent
                downMedia(chatMsgApi, corpId, type, content.sdkFileId, content.md5, content.size.toLong(), "jpg")
            }
            MsgType.VOICE -> {
                val content = msg.detail as VoiceContent
                downMedia(chatMsgApi, corpId, type, content.sdkFileId, content.md5, content.size.toLong(), "amr")
            }
            MsgType.VIDEO -> {
                val content = msg.detail as VideoContent
                downMedia(chatMsgApi, corpId, type, content.sdkFileId, content.md5, content.size.toLong(), "mp4")
            }
            MsgType.EMOTION -> {
                val content = msg.detail as EmotionContent
                //表情类型，png或者gif: 1表示gif 2表示png
                downMedia(
                    chatMsgApi, corpId, type, content.sdkFileId, content.md5, content.size?.toLong(),
                    if (content.type == 1) "gif" else if (content.type == 2) "png" else "jpg"
                )
            }
            MsgType.FILE -> {
                val content = msg.detail as FileContent
                downMedia(chatMsgApi, corpId, type, content.sdkFileId, content.md5, content.size.toLong(), content.ext)
            }
            MsgType.MEETING_VOICE_CALL -> {
                val content = msg.detail as MeetingVoiceCallContent
                //此处md5使用的是voiceId，而不是md5sum
                downMedia(chatMsgApi, corpId, type, content.voiceCall.sdkFileId, content.voiceId, 0L, "amr")
            }
            MsgType.VOIP_DOC_SHARE -> {
                val content = msg.detail as VoipContent
                downMedia(
                    chatMsgApi, corpId, type, content.voipDocShare.sdkFileId, content.voipDocShare.md5,
                    content.voipDocShare.size, "amr"
                )
            }
            else -> -1
        }
        msg.res = ret

        val count = if (chatMsgService.saveMsg(msg) == null) null else msg.seq

        if (!msg.room.isNullOrBlank()) {
            val res = chatMsgApi.getRoomInfo(msg.room)
            if (res.isOK()) {
                val room = ChatRoom(msg.room, res.roomName, res.createTime, res.creator,
                    res.notice, res.members.map { JoinedMember(it.memberid, it.jointime) }, corpId
                )
                chatMsgService.upsertChatRoom(room)
            }
        }

        return count
    }


    /**
     * 下载聊天记录中的图片、音频、视频、文件等
     * 图片是jpg格式、语音是amr格式、视频是mp4格式、文件格式类型包括在消息体内，表情分为动图与静态图，在消息体内定义。
     * */
    private fun downMedia(
        chatMsgApi: ChatMsgApi,
        corpId: String,
        type: String,
        fileId: String,
        md5: String,
        size: Long?,
        ext: String
    ): Int {
        val saveFilePath = NginxStaticRootUtil.getTotalPath(ChatMsgRecord.myPath(corpId, type))
        var file = File(saveFilePath)
        if (!file.exists()) {
            file.mkdirs()
        }

        val fileFullPath = ChatMsgRecord.fullPathFileName(corpId, type, md5, size, ext)
        file = File(fileFullPath)
        if (file.exists()) {
            if (size != null && size > 0) {
                if (file.length() == size && DigestUtils.md5Hex(FileInputStream(file)) == md5) {
                    log.info("$fileFullPath exists, ignore download")
                    return 1
                }
            } else {
                //size为0值或负值时，只要文件存在并且size>0，就认为已下载成功
                return if (file.length() > 0)
                    1
                else
                    0
            }
        }

        chatMsgApi.downMedia(sdk, fileId, fileFullPath)
        file = File(fileFullPath)
        if (file.exists()) {
            if (size != null && size > 0) {
                if (file.length() == size && DigestUtils.md5Hex(FileInputStream(file)) == md5) {
                    log.info("Successfully download: $fileFullPath")
                    return 1
                }
            } else {
                //size为0值或负值时，只要文件存在并且size>0，就认为已下载成功
                return if (file.length() > 0)
                    1
                else
                    0
            }
        }
        return 0
    }


    fun IChatMsg.toRecord(corpId: String) = when (this) {
        is SwitchMsg -> ChatMsgRecord(seq, msgId, action, type, time, user, null, null, null, corpId = corpId)
        is TextMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            text,
            corpId = corpId
        )
        is ImgMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            image,
            corpId = corpId
        )
        is RevokeMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            revoke,
            corpId = corpId
        )
        is AgreeMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            agree,
            corpId = corpId
        )
        is DisAgreeMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            disagree,
            corpId = corpId
        )
        is VoiceMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            voice,
            corpId = corpId
        )
        is VideoMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            video,
            corpId = corpId
        )
        is CardMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            card,
            corpId = corpId
        )
        is LocationMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            location,
            corpId = corpId
        )
        is EmotionMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            emotion,
            corpId = corpId
        )
        is FileMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            file,
            corpId = corpId
        )
        is LinkMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            link,
            corpId = corpId
        )
        is WeappMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            weapp,
            corpId = corpId
        )
        is ChatRecordMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            chatrecord, corpId = corpId
        )
        is TodoMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            todo,
            corpId = corpId
        )
        is VoteMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            vote,
            corpId = corpId
        )
        is CollectMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            collect,
            corpId = corpId
        )
        is RedPacketMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            redpacket, corpId = corpId
        )
        is MeetingMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            meeting,
            corpId = corpId
        )
        is DocMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            doc,
            corpId = corpId
        )
        is MarkdownMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            info,
            corpId = corpId
        )
        is NewsMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            info,
            corpId = corpId
        )
        is CalendarMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            calendar,
            corpId = corpId
        )
        is MixMsg -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            mixed,
            corpId = corpId
        )
        is MeetingVoiceCallMsg -> ChatMsgRecord(
            seq, msgId, action, type, base.time, base.from, base.toList, base.roomId, MeetingVoiceCallContent(
                voiceId,
                meetingVoiceCall
            ), corpId = corpId
        )
        is VoipMsg -> ChatMsgRecord(
            seq, msgId, action, type, base.time, base.from, base.toList, base.roomId, VoipContent(
                voipId,
                voipDocShare
            ), corpId = corpId
        )
        is ExternalRedPacket -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            base.time,
            base.from,
            base.toList,
            base.roomId,
            redpacket, corpId = corpId
        )
        is ChatMsgCommonInfo -> ChatMsgRecord(
            seq,
            msgId,
            action,
            type,
            time,
            from,
            toList,
            roomId,
            null,
            corpId = corpId
        )
        else -> {
            println("unknown msg: seq=$seq, msgId=$msgId, action=$action, $type=$type")
            ChatMsgRecord(seq, msgId, action, type, 0L, "", null, null, null, corpId = corpId)
        }
    }

}
