/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-16 20:10
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

import com.github.rwsbillyang.wxSDK.msg.ArticleItem
import com.github.rwsbillyang.wxSDK.msg.MsgType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*


/**
 * 消息动作 send(发送消息)/recall(撤回消息)/switch(切换企业日志)三种类型
 * */
@Serializable
enum class MsgAction { send, recall, switch }

/**
 * 器人与外部联系人的账号都是external_userid，其中机器人的external_userid是以”wb”开头，
 * 例如：”wbjc7bDwAAJVylUKpSA3Z5U11tDO4AAA”，外部联系人的external_userid以”wo”或”wm”开头
 * */
object MsgDirection{
    const val FROM_IN = 1
    const val FROM_OUT = 2
    const val FROM_BOT = 3

    const val TO_IN = 1
    const val TO_OUT = 2
    const val TO_BOT = 3


    const val DIRECTION_FROM_IN_TO_IN = 11
    const val DIRECTION_FROM_IN_TO_OUT = 12
    const val DIRECTION_FROM_IN_TO_BOT = 13

    const val DIRECTION_FROM_OUT_TO_IN = 21
    const val DIRECTION_FROM_OUT_TO_OUT = 22
    const val DIRECTION_FROM_OUT_TO_BOT = 23

    const val DIRECTION_FROM_BOT_TO_IN = 31
    const val DIRECTION_FROM_BOT_TO_OUT = 32
    const val DIRECTION_FROM_BOT_TO_BOT = 33

    const val DIRECTION_UNKNOWN = 0
    const val DIRECTION_NO_TO = -1
    const val DIRECTION_NOT_SUPPORT = -100
    const val DIRECTION_NOT_ERROR = -1000

    const val DIRECTION_ROOM = 99

    fun direction(from: String, toList: List<String>?): Int{
        if(toList.isNullOrEmpty()) return DIRECTION_NO_TO
        if(toList.size > 1) return DIRECTION_ROOM
        val fromType = if(from.startsWith("wo")||from.startsWith("wm"))
            FROM_OUT
        else if(from.startsWith("wb"))
            FROM_BOT
        else FROM_IN

        if(toList.isEmpty()) return DIRECTION_UNKNOWN
        val to = toList[0]
        val toType = if(to.startsWith("wo") || to.startsWith("wm"))
            TO_OUT
        else if(to.startsWith("wb"))
            TO_BOT
        else TO_IN

        return direction(fromType, toType)
    }
    fun direction(fromType: Int, toType: Int) = when(fromType){
        FROM_IN ->{
            when(toType){
                TO_IN ->  DIRECTION_FROM_IN_TO_IN
                TO_OUT -> DIRECTION_FROM_IN_TO_OUT
                TO_BOT -> DIRECTION_FROM_IN_TO_BOT
                else -> DIRECTION_UNKNOWN
            }
        }
        FROM_OUT -> {
            when(toType){
                TO_IN ->  DIRECTION_FROM_OUT_TO_IN
                TO_OUT -> DIRECTION_FROM_OUT_TO_OUT
                TO_BOT -> DIRECTION_FROM_OUT_TO_BOT
                else -> DIRECTION_UNKNOWN
            }
        }
        FROM_BOT -> {
            when(toType){
                TO_IN ->  DIRECTION_FROM_BOT_TO_IN
                TO_OUT -> DIRECTION_FROM_BOT_TO_OUT
                TO_BOT -> DIRECTION_FROM_BOT_TO_BOT
                else -> DIRECTION_UNKNOWN
            }
        }
        else -> DIRECTION_UNKNOWN
    }
}


interface IChatMsg {
    var seq: Long
    val msgId: String
    val action: MsgAction
    val type: String
}
//kotlinx.serialization.SerializationException: Class 'TextContent' is not registered for polymorphic serialization in the scope of 'IChatMsgContent'.
//Mark the base class as 'sealed' or register the serializer explicitly.
@Serializable
sealed class BaseChatMsgContent
/**
 * 业务层用于保存消息内容
 * 因为腾讯官方接口中，未将VoipContent中的两个字段放到一个object里面，此处将其放到一起，保存于数据库中。
 * 本应放在业务代码中，但因为sealed类的要求，而放置于同一个文件中
 * */
@Serializable
@SerialName(MsgType.VOIP_DOC_SHARE)
class VoipContent(
        val voipId: String,
        val voipDocShare: VoipDocShare
): BaseChatMsgContent()

/**
 * 业务层用于保存消息内容
 * 因为腾讯官方接口中，未将MeetingVoiceCallContent中的两个字段放到一个object里面，此处将其放到一起，保存于数据库中。
 * 本应放在业务代码中，但因为sealed类的要求，而放置于同一个文件中
 * */
@Serializable
@SerialName(MsgType.MEETING_VOICE_CALL)
class MeetingVoiceCallContent(
        val voiceId: String,
        val voiceCall: VoiceCall
): BaseChatMsgContent()

/**
 *
 * @param msgid    消息id，消息的唯一标识，企业可以使用此字段进行消息去重。String类型
 * @param action 消息动作，目前有send(发送消息)/recall(撤回消息)/switch(切换企业日志)三种类型。String类型
 * @param from    消息发送方id。同一企业内容为userid，非相同企业为external_userid。消息如果是机器人发出，
 * 也为external_userid。String类型. 机器人与外部联系人的账号都是external_userid，其中机器人的external_userid是
 * 以”wb”开头，例如：”wbjc7bDwAAJVylUKpSA3Z5U11tDO4AAA”，外部联系人的external_userid以”wo”或”wm”开头。
 * @param tolist    消息接收方列表，可能是多个，同一个企业内容为userid，非相同企业为external_userid。数组，内容为string类型
 * @param roomid    群聊消息的群id。如果是单聊则为空。String类型
 * @param msgtime    消息发送时间戳，utc时间，ms单位。
 * @param msgtype    文本消息为：text。String类型
 * */
@Serializable
class ChatMsgCommonInfo(
        @SerialName("msgid")
        override val msgId: String,
        override val action: MsgAction,
        val from: String,
        @SerialName("tolist")
        val toList: List<String>,
        @SerialName("roomid")
        val roomId: String?,
        @SerialName("msgtime")
        val time: Long,
        @SerialName("msgtype")
        override val type: String
) : IChatMsg {
    override var seq: Long = 0L
}

/**
 * 目的在于减少子类override太多的字段，只需override一个字段base即可
 * 同时在业务端可方便对base进行转换
 * */
@Serializable
abstract class AbstractChatMsg : IChatMsg {
    abstract val base: ChatMsgCommonInfo
    override var seq: Long = 0L
    override val msgId: String
        get() = base.msgId
    override val action: MsgAction
        get() = base.action
    override val type: String
        get() = base.type
}


@Serializable
@SerialName(MsgType.TEXT)
class TextContent(val content: String) : BaseChatMsgContent()

@Serializable
class TextMsg(override val base: ChatMsgCommonInfo, val text: TextContent) : AbstractChatMsg()


/**
 * sdkfileid	媒体资源的id信息。String类型
 * md5sum	图片资源的md5值，供进行校验。String类型
 * filesize	图片资源的文件大小。Uint32类型
 * */
@Serializable
@SerialName(MsgType.IMAGE)
class ImgContent(
        @SerialName("sdkfileid")
        val sdkFileId: String,
        @SerialName("md5sum")
        val md5: String,
        @SerialName("filesize")
        val size: Int
) : BaseChatMsgContent()

@Serializable
class ImgMsg(override val base: ChatMsgCommonInfo, val image: ImgContent) : AbstractChatMsg()


/**
 * pre_msgid
 * */
@Serializable
@SerialName(MsgType.REVOKE)
class RevokeContent(
        @SerialName("pre_msgid")
        val pre: String
) : BaseChatMsgContent()

@Serializable
class RevokeMsg(override val base: ChatMsgCommonInfo, val revoke: RevokeContent) : AbstractChatMsg()


/**
 * userid	同意/不同意协议者的userid，外部企业默认为external_userid。String类型
 * agree_time	同意/不同意协议的时间，utc时间，ms单位。
 * */
@Serializable
@SerialName(MsgType.AGREE)
class AgreeContent(
        @SerialName("userid")
        val userId: String,
        @SerialName("agree_time")
        val time: Long
) : BaseChatMsgContent()

/**
 * userid	同意/不同意协议者的userid，外部企业默认为external_userid。String类型
 * agree_time	同意/不同意协议的时间，utc时间，ms单位。
 * */
@Serializable
@SerialName(MsgType.DISAGREE)
class DisAgreeContent(
        @SerialName("userid")
        val userId: String,
        @SerialName("disagree_time")
        val time: Long
) : BaseChatMsgContent()

@Serializable
class DisAgreeMsg(override val base: ChatMsgCommonInfo, val disagree: DisAgreeContent) : AbstractChatMsg()

@Serializable
class AgreeMsg(override val base: ChatMsgCommonInfo, val agree: AgreeContent) : AbstractChatMsg()


/**
 * voice_size	语音消息大小。Uint32类型
 * play_length	播放长度。Uint32类型
 * sdkfileid	媒体资源的id信息。String类型
 * md5sum	资源的md5值，供进行校验。String类型
 * */
@Serializable
@SerialName(MsgType.VOICE)
class VoiceContent(
        @SerialName("sdkfileid")
        val sdkFileId: String,
        @SerialName("md5sum")
        val md5: String,
        @SerialName("voice_size")
        val size: Int,
        @SerialName("play_length")
        val length: Int
) : BaseChatMsgContent()

@Serializable
class VoiceMsg(override val base: ChatMsgCommonInfo, val voice: VoiceContent) : AbstractChatMsg()

/**
 * filesize	资源的文件大小。Uint32类型
 * play_length 视频播放长度。Uint32类型
 * sdkfileid	媒体资源的id信息。String类型
 * md5sum	资源的md5值，供进行校验。String类型
 * */
@Serializable
@SerialName(MsgType.VIDEO)
class VideoContent(
        @SerialName("sdkfileid")
        val sdkFileId: String,
        @SerialName("md5sum")
        val md5: String,
        @SerialName("filesize")
        val size: Int,
        @SerialName("play_length")
        val length: Int
) : BaseChatMsgContent()

@Serializable
class VideoMsg(override val base: ChatMsgCommonInfo, val video: VideoContent) : AbstractChatMsg()

/**
 * corpname	名片所有者所在的公司名称。String类型
 * userid	名片所有者的id，同一公司是userid，不同公司是external_userid。String类型
 * */
@Serializable
@SerialName(MsgType.CARD)
class CardContent(
        @SerialName("corpname")
        val name: String,
        @SerialName("userid")
        val userId: String
) : BaseChatMsgContent()

@Serializable
class CardMsg(override val base: ChatMsgCommonInfo, val card: CardContent) : AbstractChatMsg()

/**
 * longitude	经度，单位double
 * latitude	纬度，单位double
 * address	地址信息。String类型
 * title	位置信息的title。String类型
 * zoom	缩放比例。Uint32类型
 * */
@Serializable
@SerialName(MsgType.LOCATION)
class LocationContent(
        val longitude: Double,
        val latitude: Double,
        val title: String,
        val zoom: Int
) : BaseChatMsgContent()

@Serializable
class LocationMsg(override val base: ChatMsgCommonInfo, val location: LocationContent) : AbstractChatMsg()


/**
 * type	表情类型，png或者gif.1表示gif 2表示png。Uint32类型
 * width	表情图片宽度。Uint32类型
 * height	表情图片高度。Uint32类型
 * sdkfileid	媒体资源的id信息。String类型
 * md5sum	资源的md5值，供进行校验。String类型
 * imagesize	资源的文件大小。Uint32类型
 * */
@Serializable
@SerialName(MsgType.EMOTION)
class EmotionContent(
        val type: Int? =null,
        val width: Int? =null,
        val height: Int? =null,
        @SerialName("sdkfileid")
        val sdkFileId: String,
        @SerialName("md5sum")
        val md5: String,
        @SerialName("imagesize")
        val size: Int? =null
) : BaseChatMsgContent()

@Serializable
class EmotionMsg(override val base: ChatMsgCommonInfo, val emotion: EmotionContent) : AbstractChatMsg()

/**
 * sdkfileid	媒体资源的id信息。String类型
md5sum	资源的md5值，供进行校验。String类型
filename	文件名称。String类型
fileext	文件类型后缀。String类型
filesize	文件大小。Uint32类型
 * */
@Serializable
@SerialName(MsgType.FILE)
class FileContent(
        @SerialName("sdkfileid")
        val sdkFileId: String,
        @SerialName("md5sum")
        val md5: String,
        @SerialName("filesize")
        val size: Int,
        @SerialName("filename")
        val name: String,
        @SerialName("fileext")
        val ext: String
) : BaseChatMsgContent()

@Serializable
class FileMsg(override val base: ChatMsgCommonInfo, val file: FileContent) : AbstractChatMsg()

/**
 * title	消息标题。String类型
 * description	消息描述。String类型
 * link_url	链接url地址。String类型
 * image_url	链接图片url。String类型
 * */
@Serializable
@SerialName(MsgType.LINK)
class LinkContent(
        val title: String,
        val description: String,
        @SerialName("link_url")
        val link: String,
        @SerialName("image_url")
        val image: String
) : BaseChatMsgContent()

@Serializable
class LinkMsg(override val base: ChatMsgCommonInfo, val link: LinkContent) : AbstractChatMsg()


/**
 * title	消息标题。String类型
 * description	消息描述。String类型
 * username	用户名称。String类型
 * displayname	小程序名称。String类型
 * */
@Serializable
@SerialName(MsgType.WEAPP)
class WeappContent(
        val title: String,
        val description: String,
        val username: String,
        @SerialName("displayname")
        val name: String
) : BaseChatMsgContent()

@Serializable
class WeappMsg(override val base: ChatMsgCommonInfo, val weapp: WeappContent) : AbstractChatMsg()


/**
 * type	每条聊天记录的具体消息类型：ChatRecordText/ ChatRecordFile/ ChatRecordImage
 * / ChatRecordVideo/ ChatRecordLink/ ChatRecordLocation/ ChatRecordMixed ….
 * msgtime	消息时间，utc时间，ms单位。
 * content	消息内容。Json串，内容为对应类型的json。String类型
 * from_chatroom	是否来自群会话。Bool类型
 * */
@Serializable
class ChatItem(
        val type: String,
        @SerialName("msgtime")
        val time: Long,
        val content: String,
        @SerialName("from_chatroom")
        val fromRoom: Boolean
)

@Serializable
@SerialName(MsgType.CHAT_RECORD)
class ChatRecord(
        val title: String,
        val item: List<ChatItem>
) : BaseChatMsgContent()

@Serializable
class ChatRecordMsg(override val base: ChatMsgCommonInfo, val chatrecord: ChatRecord) : AbstractChatMsg()

/**
 * title	待办的来源文本。String类型
 * content	待办的具体内容。String类型
 */
@Serializable
@SerialName(MsgType.TODO)
class TodoContent(val title: String, val content: String) : BaseChatMsgContent()

@Serializable
class TodoMsg(override val base: ChatMsgCommonInfo, val todo: TodoContent) : AbstractChatMsg()


/**
 * votetitle	投票主题。String类型
 * voteitem	投票选项，可能多个内容。String数组
 * votetype	投票类型.101发起投票、102参与投票。Uint32类型
 * voteid	投票id，方便将参与投票消息与发起投票消息进行前后对照。String类型
 * */
@Serializable
@SerialName(MsgType.VOTE)
class VoteContent(
        @SerialName("votetitle")
        val title: String,
        @SerialName("voteitem")
        val item: String,
        @SerialName("votetype")
        val type: Int,
        @SerialName("voteid")
        val id: String
) : BaseChatMsgContent()

@Serializable
class VoteMsg(override val base: ChatMsgCommonInfo, val vote: VoteContent) : AbstractChatMsg()


/**
 * room_name	填表消息所在的群名称。String类型
 * creator	创建者在群中的名字。String类型
 * create_time	创建的时间。String类型
 * title	表名。String类型
 * details	表内容。json数组类型
 * id	表项id。Uint64类型
 * ques	表项名称。String类型
 * type	表项类型，有Text(文本),Number(数字),Date(日期),Time(时间)。String类型
 * */
@Serializable
@SerialName(MsgType.COLLECT)
class CollectContent(
        @SerialName("room_name")
        val room: String,
        val creator: String,
        @SerialName("create_time")
        val time: String,
        val title: String,
        val details: List<CollectItem>
) : BaseChatMsgContent()

@Serializable
enum class InputType { Text, Number, Date, Time }

/**
 * id	表项id。Uint64类型
 * ques	表项名称。String类型
 * type	表项类型，有Text(文本),Number(数字),Date(日期),Time(时间)。String类型
 * */
@Serializable
class CollectItem(
        val id: Long,
        val ques: String,
        val type: InputType
)

@Serializable
class CollectMsg(override val base: ChatMsgCommonInfo, val collect: CollectContent) : AbstractChatMsg()


/**
 * type	红包消息类型。1 普通红包、2 拼手气群红包、3 激励群红包。Uint32类型
 * wish	红包祝福语。String类型
 * totalcnt	红包总个数。Uint32类型
 * totalamount	红包总金额。Uint32类型，单位为分。
 * */
@Serializable
@SerialName(MsgType.RED_PACKET)
open class RedPacketContent(
        val type: Int,
        val wish: String,
        @SerialName("totalcnt")
        val count: Int,
        @SerialName("totalamount")
        val amount: Int
) : BaseChatMsgContent()

@Serializable
class RedPacketMsg(override val base: ChatMsgCommonInfo, val redpacket: RedPacketContent) : AbstractChatMsg()

/**
 * 互通红包消息 出现在本企业与外部企业群聊发送的红包、或者本企业与微信单聊、群聊发送的红包消息场景下。
 * */
class ExternalRedPacket(override val base: ChatMsgCommonInfo, val redpacket: RedPacketContent) : AbstractChatMsg()

/**
 * topic	会议主题。String类型
 * starttime	会议开始时间。Utc时间
 * endtime	会议结束时间。Utc时间
 * address	会议地址。String类型
 * remarks	会议备注。String类型
 * meetingtype	会议消息类型。101发起会议邀请消息、102处理会议邀请消息。Uint32类型
 * meetingid	会议id。方便将发起、处理消息进行对照。uint64类型
 * status	会议邀请处理状态。1 参加会议、2 拒绝会议、3 待定、4 未被邀请、5 会议已取消、6 会议已过期、
 * 7 不在房间内。Uint32类型。只有meetingtype为102的时候此字段才有内容。
 * */
@Serializable
@SerialName(MsgType.MEETING)
class MeetingContent(
        val topic: String,
        @SerialName("starttime")
        val start: Long,
        @SerialName("endtime")
        val end: Long,
        val address: String,
        val remarks: String,
        @SerialName("meetingtype")
        val type: Int,
        @SerialName("meetingid")
        val id: Long,
        val status: Int? = null
) : BaseChatMsgContent()

@Serializable
class MeetingMsg(override val base: ChatMsgCommonInfo, val meeting: MeetingContent) : AbstractChatMsg()


/**
 * 注：切换企业日志不是真正的消息，与上述消息结构不完全相同。
 * msgid	消息id，消息的唯一标识，企业可以使用此字段进行消息去重。String类型
 * action	消息动作，切换企业为switch。String类型
 * time	消息发送时间戳，utc时间，ms单位。
 * user	具体为切换企业的成员的userid。String类型
 * */
@Serializable
class SwitchMsg(
        @SerialName("msgid")
        override val msgId: String,
        override val action: MsgAction,
        val time: Long,
        val user: String
) : IChatMsg {
    override var seq: Long = 0L
    override val type: String = "switch"
}


/**
 * title	在线文档名称
 * link_url	在线文档链接
 * doc_creator	在线文档创建者。本企业成员创建为userid；外部企业成员创建为external_userid
 * */
@Serializable
@SerialName(MsgType.DOC)
class DocContent(
        val title: String,
        @SerialName("link_url")
        val url: String,
        @SerialName("doc_creator")
        val creator: String
) : BaseChatMsgContent()

@Serializable
class DocMsg(override val base: ChatMsgCommonInfo, val doc: DocContent) : AbstractChatMsg()


@Serializable
class MarkdownMsg(override val base: ChatMsgCommonInfo, val info: TextContent) : AbstractChatMsg()


@Serializable
@SerialName(MsgType.NEWS)
class NewsContent(val item: List<ArticleItem>) : BaseChatMsgContent()

@Serializable
class NewsMsg(override val base: ChatMsgCommonInfo, val info: NewsContent) : AbstractChatMsg()


/**
 * title	日程主题。String类型
 * creatorname	日程组织者。String类型
 * attendeename	日程参与人。数组，内容为String类型
 * starttime	日程开始时间。Utc时间，单位秒
 * endtime	日程结束时间。Utc时间，单位秒
 * place	日程地点。String类型
 * remarks	日程备注。String类型
 * */
@Serializable
@SerialName(MsgType.CALENDAR)
class CalendarContent(
        val title: String,
        @SerialName("creatorname")
        val creator: String,
        @SerialName("attendeename")
        val attendee: String,
        @SerialName("starttime")
        val start: Long,
        @SerialName("endtime")
        val end: Long,
        val place: String,
        val remarks: String? = null
) : BaseChatMsgContent()

@Serializable
class CalendarMsg(override val base: ChatMsgCommonInfo, val calendar: CalendarContent) : AbstractChatMsg()


/**
 * mixed内包含一个item数组，其中每个元素由type与content组成，type和content均为String类型。
 * JSON解析content后即可获取对应type类型的消息内容。
 * */
@Serializable
class MixMsgItem(val type: String, val content: String)

@Serializable
@SerialName(MsgType.MIXED)
class MixContent(val item: List<MixMsgItem>) : BaseChatMsgContent()

@Serializable
class MixMsg(override val base: ChatMsgCommonInfo, val mixed: MixContent) : AbstractChatMsg()


/**
 * filename	文档共享名称。String类型
 * demooperator	文档共享操作用户的id。String类型
 * starttime	文档共享开始时间。Uint32类型
 * endtime	文档共享结束时间。Uint32类型
 * */
@Serializable
class DemoFileData(
        val filename: String,
        @SerialName("demooperator")
        val operator: String,
        @SerialName("starttime")
        val start: Long,
        @SerialName("endtime")
        val end: Long
)

/**
 * share	屏幕共享用户的id。String类型
 * starttime	屏幕共享开始时间。Uint32类型
 * endtime	屏幕共享结束时间。Uint32类型
 * */
@Serializable
class ShareScreenData(
        val share: String,
        @SerialName("starttime")
        val start: Long,
        @SerialName("endtime")
        val end: Long
)

/**
 * 音频消息内容。包括结束时间、fileid，可能包括多个demofiledata、sharescreendata消息，
 * demofiledata表示文档共享信息，sharescreendata表示屏幕共享信息。Object类型
 * @param endtime 音频结束时间。uint32类型
 * @param sdkfileid    sdkfileid。音频媒体下载的id。String类型
 * @param demofiledata    文档分享对象，Object类型
 * */
@Serializable
class VoiceCall(
        val endtime: Int,
        @SerialName("sdkfileid")
        val sdkFileId: String,
        @SerialName("demofiledata")
        val demoFileData: List<DemoFileData>,
        @SerialName("sharescreendata")
        val shareScreenData: List<ShareScreenData>
)

/**
 * voiceid	String类型, 音频id
 * meeting_voice_call	音频消息内容。包括结束时间、fileid，可能包括多个demofiledata、
 * sharescreendata消息，demofiledata表示文档共享信息，sharescreendata表示屏幕共享信息。Object类型
 * */
@Serializable
class MeetingVoiceCallMsg(
        override val base: ChatMsgCommonInfo,
        @SerialName("voiceid")
        val voiceId: String,
        @SerialName("meeting_voice_call")
        val meetingVoiceCall: VoiceCall
) : AbstractChatMsg()


/**
 * voip_doc_share	共享文档消息内容。包括filename、md5sum、filesize、sdkfileid字段。Object类型
 * filename	文档共享文件名称。String类型
 * md5sum	共享文件的md5值。String类型
 * filesize	共享文件的大小。Uint64类型
 * sdkfileid	共享文件的sdkfile，通过此字段进行媒体数据下载。String类型
 * */
@Serializable
class VoipDocShare(
        val filename: String,
        val md5: String,
        @SerialName("filesize")
        val size: Long,
        @SerialName("sdkfileid")
        val sdkFileId: String,
)

@Serializable
class VoipMsg(
        override val base: ChatMsgCommonInfo,
        @SerialName("voipid")
        val voipId: String,
        @SerialName("voip_doc_share")
        val voipDocShare: VoipDocShare
) : AbstractChatMsg()


object ChatMsgSerializer : KSerializer<IChatMsg> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("IChatMsg")
//        {
//                element<ChatMsgCommonInfo>("base")
//                element<JsonElement>("details")
//        }

    override fun deserialize(decoder: Decoder): IChatMsg {
        // Cast to JSON-specific interface
        val jsonDecoder = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        // Read the whole content as JSON
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val action = jsonObject.getValue("action").jsonPrimitive.content
        return if ("switch" == action) {
            SwitchMsg(
                    jsonObject.getValue("msgid").jsonPrimitive.content,
                    MsgAction.valueOf(action),
                    jsonObject.getValue("msgtime").jsonPrimitive.long,
                    jsonObject.getValue("user").jsonPrimitive.content,
            )
        } else {
            val base = deBase(jsonObject)
            val type = base.type
            val json = Json
            when (type) {
                    MsgType.TEXT -> TextMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.IMAGE -> ImgMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.REVOKE -> RevokeMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.AGREE -> AgreeMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.DISAGREE -> DisAgreeMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.VOICE -> VoiceMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.VIDEO -> VideoMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.CARD -> CardMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.LOCATION -> LocationMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.EMOTION -> EmotionMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.FILE -> FileMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.LINK -> LinkMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.WEAPP -> WeappMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.CHAT_RECORD -> ChatRecordMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.TODO -> TodoMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.VOTE -> VoteMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.COLLECT -> CollectMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.RED_PACKET -> RedPacketMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.MEETING -> MeetingMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.DOC -> DocMsg(base, json.decodeFromJsonElement(jsonObject.getValue("doc")))
                    MsgType.MARKDOWN -> MarkdownMsg(base, json.decodeFromJsonElement(jsonObject.getValue("info")))
                    MsgType.NEWS -> NewsMsg(base, json.decodeFromJsonElement(jsonObject.getValue("info")))
                    MsgType.CALENDAR -> CalendarMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.MIXED -> MixMsg(base, json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.MEETING_VOICE_CALL -> MeetingVoiceCallMsg(base, jsonObject.getValue("voiceid").jsonPrimitive.content,
                            json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.VOIP_DOC_SHARE -> VoipMsg(base, jsonObject.getValue("voipid").jsonPrimitive.content,
                            json.decodeFromJsonElement(jsonObject.getValue(type)))
                    MsgType.EXTERNAL_RED_PACKET -> ExternalRedPacket(base, json.decodeFromJsonElement(jsonObject.getValue("redpacket")))
                else -> base
            }
        }
    }

    override fun serialize(encoder: Encoder, value: IChatMsg) {
        error("Serialization is not supported")
    }


    private fun deBase(jsonObject: JsonObject): ChatMsgCommonInfo {
        return ChatMsgCommonInfo(
                jsonObject.getValue("msgid").jsonPrimitive.content,
                MsgAction.valueOf(jsonObject.getValue("action").jsonPrimitive.content),
                jsonObject.getValue("from").jsonPrimitive.content,
                jsonObject.getValue("tolist").jsonArray.map { it.jsonPrimitive.content },
                jsonObject["roomid"]?.jsonPrimitive?.content,
                jsonObject.getValue("msgtime").jsonPrimitive.long,
                jsonObject.getValue("msgtype").jsonPrimitive.content
        )
    }
}