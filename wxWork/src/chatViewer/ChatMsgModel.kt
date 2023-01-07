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


@file:UseContextualSerialization(ObjectId::class)
package com.github.rwsbillyang.wxWork.chatViewer

import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
import com.github.rwsbillyang.ktorKit.server.BizException
import com.github.rwsbillyang.ktorKit.util.NginxStaticRootUtil


import com.github.rwsbillyang.wxSDK.work.chatMsg.BaseChatMsgContent
import com.github.rwsbillyang.wxSDK.work.chatMsg.MsgAction
import com.github.rwsbillyang.wxSDK.work.chatMsg.MsgDirection
import io.ktor.resources.*

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


@Serializable
class JoinedMember(
    val id: String,
    val time: Long
)
//企业的内部群信息，包括群名称、群主id、公告、群创建时间以及所有群成员的id与加入时间。
@Serializable
data class ChatRoom(
    val _id: String,
    val name: String? = null,
    val time: Long,
    val creator: String,
    val notice: String? = null,
    val members: List<JoinedMember>,
    val corpId: String
)

/**
 * robot_id	机器人ID
name	机器人名称
creator_userid	机器人创建者的UserID
 * */
@Serializable
data class ChatBot(
    val _id: String,
    val name: String,
    val creator: String,
    val corpId: String
)

@Serializable
data class Seq(
    val _id: String,//corpId
    val seq: Long
)

/**
 * @param seq 企业微信中的seq序列
 * @param id 消息id，即msgid
 * @param action send,recall,switch
 * @param time utc时间变换而来
 * @param from 来自
 * @param to 接收者列表
 * @param room 群聊时的聊天室id
 * @param detail 消息具体内容
 * @param res 是否有资源，即是否下载成功：成功为1，不成功为0，其它情况-1
 *
 * 修改字段类型：db.chatMsgRecord.find({"seq":{$type:"int"}}).forEach(function(x){db.chatMsgRecord.update({'_id':x._id},{$set:{'seq':NumberLong(x.seq)}})})
 * */
@Serializable
data class ChatMsgRecord(
    val seq: Long, //seq
    val _id: String, //msgId
    val action: MsgAction,
    val type: String,
    val time: Long,
    val from: String,
    val to: List<String>?,//当为空时表示switch
    val room: String?,//当为空时表示单聊或switch
    val detail: BaseChatMsgContent?, //当为空时表示switch
    var direction: Int = MsgDirection.DIRECTION_UNKNOWN,
    var res: Int = -1, // 资源下载成功1，否则为0，其它情况-1
    val corpId: String,
    //val _id: ObjectId = ObjectId()
){
    init {
        direction = MsgDirection.direction(from, to)
    }

    companion object{
        fun filename(md5: String, size: Long?, ext: String) = if(size==null) "$md5.$ext" else "$md5-$size.$ext"
        fun myPath(corpId:String, type: String) = "chatviewer/$corpId/$type"
        fun fullPathFileName(corpId:String,type: String, md5: String, size: Long?, ext: String) = NginxStaticRootUtil.getTotalPath(myPath(corpId, type)) +"/"+ filename(md5, size, ext)
        /**
         * 注意： 资源文件路径以"/static/"开头，nginx配置一个静态服务host，root路径为cachedRoot，其下有一个static目录
         * */
        fun resUri(corpId:String, type: String, md5: String, size: Long?, ext: String) = "${NginxStaticRootUtil.getUrlPrefix(myPath(corpId, type))}/${filename(md5, size, ext)}"
    }
}


/**
 * @param userId Contact.userId
 * @param externalId ExternalContact.externalUserId
 * @param type 消息类型
 * @param direction 消息进出方向
 * @param inDays 几天内的消息，最高优先级
 * @param date 某一天的消息，次优先级
 * @param start 起始日期时间 最低优先级 //YYYY-MM-DD HH:mm:ss
 * @param end 截止日期时间
 * */
@Serializable
@Resource("/list")
class ChatMsgListParam(
    override val umi: String? = null,
    val corpId: String,
    val userId: String,
    val externalId: String? = null,
    val direction: Int? = null,
    val type: String? = null,
    val inDays: Int? = null,
    val date: String? = null,
    val start: String? = null,
    val end: String? = null
): IUmiPaginationParams{
    //private val log = LoggerFactory.getLogger("ChatMsgListParam")

    override fun toFilter(): Bson {
        var locatDateTime: LocalDateTime
        var startTime = -1L
        var endTime = -1L
        if (inDays != null) {
            //0天内表示今天零点到现在，1天内表示昨天零点到现在，2天内前天零点到现在,提供1,3,7,30
            val locatDate = LocalDate.now()
            startTime = locatDate.minusDays(inDays.toLong()).atStartOfDay(ZoneOffset.ofHours(8)).toInstant()
                .toEpochMilli() / 1000
        } else if (date != null) {
            val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")
            try {
                //某个日期当天的  2017-02-22   字符串严格按照yyyy-MM-dd
                locatDateTime = LocalDateTime.parse("$date 00:00:00", formatter)
                startTime = locatDateTime.toEpochSecond(ZoneOffset.of("+8"))
                endTime = startTime + 24 * 3600
            } catch (e: DateTimeParseException) {
                println("DateTimeParseException: $date")
                throw BizException("wrong $date, should be yyyy-MM-dd format")
            }
        } else {
            val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")
            try {
                //某个日期当天的  2017-02-22   字符串严格按照yyyy-MM-dd
                if (start != null) {
                    locatDateTime = LocalDateTime.parse("$start 00:00:00", formatter)
                    startTime = locatDateTime.toEpochSecond(ZoneOffset.of("+8"))
                }

                if (end != null) {
                    locatDateTime = LocalDateTime.parse("$end 00:00:00", formatter)
                    endTime = locatDateTime.toEpochSecond(ZoneOffset.of("+8"))
                }
            } catch (e: DateTimeParseException) {
                println("DateTimeParseException: $start-$end")
                throw BizException("wrong date format, should be yyyy-MM-dd format")
            }
        }

        val dateTimeRangeFilter = if (startTime > 0L && endTime > 0L) {
            "{time:{\$gte:$startTime , \$lt:$endTime}}".bson
        } else if (startTime > 0L && endTime < 0L) {
            "{time:{\$gte:$startTime}}".bson
        } else if (startTime < 0L && endTime > 0L) {
            "{time:{\$lt:$endTime}}".bson
        } else null


        val typeFilter = type?.let { ChatMsgRecord::type eq it }
        val ownerFilter = when (direction) {
            MsgDirection.DIRECTION_FROM_IN_TO_OUT,
            MsgDirection.DIRECTION_FROM_IN_TO_BOT -> {
                ChatMsgRecord::from eq userId
            }
            MsgDirection.DIRECTION_FROM_OUT_TO_IN,
            MsgDirection.DIRECTION_FROM_OUT_TO_BOT -> {
                val f1 = ChatMsgRecord::to contains externalId
                if (externalId != null) and(f1, ChatMsgRecord::from eq externalId)
                else f1
            }
            else -> {//完全根据contactUserId和customerId值来决定
                if (externalId != null) {
                    val f1 = ChatMsgRecord::from eq userId
                    val f2 = ChatMsgRecord::to contains externalId

                    val f3 = ChatMsgRecord::from eq externalId
                    val f4 = ChatMsgRecord::to contains userId
                    or(and(f1, f2), and(f3, f4)) //最常用情景，指定了双方，但未指定方向
                } else {
                    or(ChatMsgRecord::from eq userId, ChatMsgRecord::to contains userId)
                }
            }
        }

        return and(ownerFilter, dateTimeRangeFilter,
            typeFilter, ChatMsgRecord::corpId eq corpId)
    }
}
/**
 * 两人单聊信息
 * 群聊消息 为内部的暂不支持
 */
@Serializable
class SingleChatMsg(
    val id: String,//ChatMsgRecord._id
    val action: String,
    val res: Int,// 资源下载成功1，否则为0，其它情况-1
    val type: String,
    val time: Long,
    val from: String,
    val to: String,
    val direction: Int,
    val detail: BaseChatMsgContent?,
    val resUri: String? = null
)

//@Serializable
//class SingleChatMsgListBox(
//    val total: Long,
//    val data: List<SingleChatMsg>? = null
//) : Box()

