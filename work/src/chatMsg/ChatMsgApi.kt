/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-17 17:47
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

package com.github.rwsbillyang.wxSDK.work.chatMsg

import com.github.rwsbillyang.wxSDK.IBase
import com.github.rwsbillyang.wxSDK.security.RsaCryptoUtil
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.WorkBaseApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream


object ChatMsgApi : WorkBaseApi(AN_ChatArchive) {
    override val group = "msgaudit"

    /**
     * 获取会话内容存档开启成员列表 企业可通过此接口，获取企业开启会话内容存档的成员列表
     * https://work.weixin.qq.com/api/doc/90000/90135/91614
     * @param type    否	拉取对应版本的开启成员列表。1表示办公版；2表示服务版；3表示企业版。
     * 非必填，不填写的时候返回全量成员列表。
     * */
    fun getPermitUserList(type: Int? = null): ResponsePermitUserList = doPost("get_permit_user_list",
            if (type != null) mapOf("type" to type) else null)


    /**
     * 获取会话同意情况: 获取会话中外部成员的同意情况
     * https://work.weixin.qq.com/api/doc/90000/90135/91782
     * */
    fun checkSingleAgree(sessions: ChatPairs): ResponseAgreeStatus = doPost("check_single_agree", sessions)


    /**
     * 查询对应roomid里面所有外企业的外部联系人的同意情况
     * https://work.weixin.qq.com/api/doc/90000/90135/91782
     * */
    fun checkRoomAgree(roomId: String): ResponseAgreeStatus = doPost("check_room_agree", mapOf("roomid" to roomId))


    /**
     * 本企业的内部群信息，包括群名称、群主id、公告、群创建时间以及所有群成员的id与加入时间。
     * */
    fun getRoomInfo(roomId: String): ResponseRoomInfo = doPost("groupchat/get", mapOf("roomid" to roomId))

    /**
     * 通过robot_id获取机器人的名称和创建者
     * */
    fun getBotInfo(robot: String):ResponseBotInfo = doGet("get_robot_info", mapOf("robot_id" to robot))



    const val CHAT_MSG_MAX_LIMIT = 1000
    /**
     * 获取聊天记录
     * 调用者首先负责newSDK和init，之后负责释放
     * */
    fun getChatMsgList(sdk: Long, seq: Long = 0L, limit: Int = CHAT_MSG_MAX_LIMIT, proxy: String? = null, pwd: String? = null, timeout: Long = 30L): List<IChatMsg>?
    {
        val slice: Long = ChatMsgSdk.NewSlice()
        val ret = ChatMsgSdk.GetChatData(sdk, seq, limit, proxy, pwd, timeout, slice)
        if (ret != 0) {
            println("ChatMsgSdk.GetChatData ret=$ret")
            return null
        }
        val list = mutableListOf<IChatMsg>()
        val jsonStr = ChatMsgSdk.GetContentFromSlice(slice)
        if(jsonStr != null){
            val json = Json.decodeFromString(ResponseChatList.serializer(), jsonStr)
            if(json.isOK()){
                //encrypt_random_key内容解密说明：
                //  encrypt_random_key是使用企业在管理端填写的公钥（使用模值为2048bit的秘钥），采用RSA加密算法进
                //  行加密处理后base64 encode的内容，加密内容为企业微信产生。RSA使用PKCS1。
                //
                // 企业通过GetChatData获取到会话数据后：
                //    a) 需首先对每条消息的encrypt_random_key内容进行base64 decode,得到字符串str1.
                //    b) 使用publickey_ver指定版本的私钥，使用RSA PKCS1算法对str1进行解密，得到解密内容str2.
                //    c) 得到str2与对应消息的encrypt_chat_msg，调用下方描述的DecryptData接口，即可获得消息明文。
                val privateKey = Work.WORK.agentMap[agentName]?.privateKey
                if(privateKey == null){
                    println("ChatMsgSdk.GetChatData privateKey is null, please config it first")
                    return null
                }
                json.chatList?.forEach {
                    val str2 = RsaCryptoUtil.decryptPKCS1(it.encryptRandomKey, privateKey)

                    val msg: Long = ChatMsgSdk.NewSlice()
                    val ret2 = ChatMsgSdk.DecryptData(sdk, str2, it.encryptChatMsg,msg)
                    if (ret2 != 0) {
                        println("ChatMsgSdk.DecryptData=$ret2, msgId=${it.msgId}, seq=${it.seq}")
                    }else{
                        val chatRecordJsonStr = ChatMsgSdk.GetContentFromSlice(msg)
                        if(!chatRecordJsonStr.isNullOrBlank()){
                            list.add(Json.decodeFromString(ChatMsgSerializer,chatRecordJsonStr).apply { this.seq = it.seq })
                        }else{
                            println("chatRecordJsonStr isNullOrBlank, msgId=${it.msgId}, seq=${it.seq}")
                        }
                    }
                    ChatMsgSdk.FreeSlice(msg)
                }
            }else{
                println("Fail: jsonStr=$jsonStr")
            }
        }

        ChatMsgSdk.FreeSlice(slice)
        return list
    }


    /**
     * @param sdk 由调用者维护，
     * @param fileId 为消息中的sdkfileid，即待下载的文件标识
     * @param saveFile 保存到本地的文件
     * */
    fun downMedia(sdk: Long, fileId: String, saveFile: String, proxy: String? = null, pwd: String? = null, timeout: Long = 30L)
    {
        val outputStream = FileOutputStream(File(saveFile))
        var indexBuf = ""
        while (true) {
            val mediaData: Long = ChatMsgSdk.NewMediaData()
            val ret = ChatMsgSdk.GetMediaData(sdk, indexBuf, fileId, proxy, pwd, timeout, mediaData)
            if (ret != 0) {
                println("ChatMsgSdk.GetMediaData ret=$ret")
                return
            }
            try {
                outputStream.write(ChatMsgSdk.GetData(mediaData))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (ChatMsgSdk.IsMediaDataFinish(mediaData) == 1) {
                outputStream.close()
                ChatMsgSdk.FreeMediaData(mediaData)
                break
            } else {
                indexBuf = ChatMsgSdk.GetOutIndexBuf(mediaData)
                ChatMsgSdk.FreeMediaData(mediaData)
            }
        }
    }
}

@Serializable
class ResponsePermitUserList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        val ids: List<String>
) : IBase

@Serializable
class ResponseChatList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("chatdata")
        val chatList: List<ChatData>? = null
): IBase

@Serializable
class ChatData(
        val seq: Long,
        @SerialName("msgid")
        val msgId: String,
        @SerialName("publickey_ver")
        val publicKeyVer: String,
        @SerialName("encrypt_random_key")
        val encryptRandomKey: String,
        @SerialName("encrypt_chat_msg")
        val encryptChatMsg: String
)

/**
 * 目前一次请求只支持最多100个查询条目，超过此限制的请求会被拦截，请调用方减少单次请求的查询个数。
 * */
@Serializable
class ChatPairs(
        val info: MutableList<ChatPair> = mutableListOf()
) {
    fun add(userId: String, customerId: String) {
        require(info.size < 100) { "目前一次请求只支持最多100个查询条目" }
        info.add(ChatPair(userId, customerId))
    }
}

@Serializable
class ChatPair(
        @SerialName("userid")
        val userId: String,
        @SerialName("exteranalopenid")
        val customerId: String
)

@Serializable
class ResponseAgreeStatus(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("agreeinfo")
        val agreeInfo: List<ChatSessionAgreeStatus>
) : IBase

@Serializable
enum class AgreeStatus {
    Agree, Disagree, Default_Agree
}

@Serializable
class ChatSessionAgreeStatus(
        @SerialName("status_change_time")
        val changeTime: Long,
        @SerialName("userid")
        val userId: String? = null, //当为room时，此字段为空
        @SerialName("exteranalopenid")
        val customerId: String,
        @SerialName("agree_status")
        val status: AgreeStatus
)

/**
 * roomname	roomid对应的群名称
 * creator	roomid对应的群创建者，userid
 * room_create_time	roomid对应的群创建时间
 * notice	roomid对应的群公告
 * members	roomid对应的群成员列表
 * memberid	roomid群成员的id，userid
 * jointime	roomid群成员的入群时间
 * */
@Serializable
class ResponseRoomInfo(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,

        @SerialName("roomname")
        val roomName: String,
        val creator: String,
        @SerialName("room_create_time")
        val createTime: Long,
        val notice: String,
        val members: List<RoomMember>
) : IBase

@Serializable
class RoomMember(
        val memberid: String,
        val jointime: Long
)


@Serializable
class ResponseBotInfo(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        val data: BotInfo
) : IBase

/**
 * robot_id	机器人ID
 * name	机器人名称
 * creator_userid	机器人创建者的UserID
 * */
@Serializable
class BotInfo(
        @SerialName("robot_id")
        val robot: String,
        val name: String,
        @SerialName("creator_userid")
        val creator: String
)