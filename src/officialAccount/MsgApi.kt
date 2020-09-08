package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.common.IBase
import com.github.rwsbillyang.wxSDK.common.Response
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

/**
 * 群发消息及模板消息接口，实现更灵活的群发能力
 *
 * 群发图文消息的过程如下：
 *
 * 首先，预先将图文消息中需要用到的图片，使用上传图文消息内图片接口，上传成功并获得图片 URL；
 * 上传图文消息素材，需要用到图片时，请使用上一步获取的图片 URL；
 * 使用对用户标签的群发，或对 OpenID 列表的群发，将图文消息群发出去，群发时微信会进行原创校验，并返回群发操作结果；
 * 在上述过程中，如果需要，还可以预览图文消息、查询群发状态，或删除已群发的消息等。
 * 群发图片、文本等其他消息类型的过程如下：
 *
 * 如果是群发文本消息，则直接根据下面的接口说明进行群发即可；
 * 如果是群发图片、视频等消息，则需要预先通过素材管理接口准备好 mediaID。
 * 关于群发时使用is_to_all为true使其进入公众号在微信客户端的历史消息列表：
 *
 * 使用is_to_all为true且成功群发，会使得此次群发进入历史消息列表。
 * 为防止异常，认证订阅号在一天内，只能使用is_to_all为true进行群发一次，或者在公众平台官网群发（不管本次群发是对全体还是对某个分组）一次。以避免一天内有2条群发进入历史消息列表。
 * 类似地，服务号在一个月内，使用is_to_all为true群发的次数，加上公众平台官网群发（不管本次群发是对全体还是对某个分组）的次数，最多只能是4次。
 * 设置is_to_all为false时是可以多次群发的，但每个用户只会收到最多4条，且这些群发不会进入历史消息列表。
 * 另外，请开发者注意，本接口中所有使用到media_id的地方，现在都可以使用素材管理中的永久素材media_id了。请但注意，使用同一个素材群发出去的链接是一样的，这意味着，删除某一次群发，会导致整个链接失效。
 * */
class MsgApi : OABaseApi() {
    override val group: String = "message"

    /**
     * 发送模板消息
     * */
    fun sendTemplateMsg(msg: TemplateMsg): ResponseSendTemplateMsg = doPost2("template/send", msg)

    fun massSendByTag(msg: IMsg): ResponseMassSend = doPost2("mass/sendall", msg)

    fun massSendByOpenIds(msg: IMsg):ResponseMassSend = doPost2("mass/send", msg)

    /**
     * 预览接口【订阅号与服务号认证后均可用】
     * 在保留对openID预览能力的同时，增加了对指定微信号发送预览的能力，但该能力每日调用次数有限制（100次）
     * */
    fun preview(msg: IMsg):ResponseMassSend = doPost2("mass/preview", msg)

    /**
     * @param msgId	是	msg_id 发送出去的消息ID，来自massSendByTag 或 massSendByOpenIds
     * @param articleId article_idx	否	要删除的文章在图文消息中的位置，第一篇编号为1，该字段不填或填0会删除全部文章
     * */
    fun deleteMas(msgId: Long, articleId: Int? = null): Response = doPost2("mass/delete", mapOf("msg_id" to msgId, "article_idx" to articleId))

    /**
     * @param msgId msg_id	群发消息后返回的消息id
     * @return 	消息发送后的状态，SEND_SUCCESS表示发送成功，SENDING表示发送中，SEND_FAIL表示发送失败，DELETE表示已删除
     * */
    fun getStatus(msgId: Long):String = doPost("mass/get", mapOf("msg_id" to msgId))["msg_status"].toString()


    /**
     * 群发速度的级别，是一个0到4的整数，数字越大表示群发速度越慢。
     * 0	80w/分钟
     * 1	60w/分钟
     * 2	45w/分钟
     * 3	30w/分钟
     * 4	10w/分钟
     * */
    fun setSpeed(speed: Int): String = doPost2("speed/set", mapOf("speed" to speed))

    fun getSpeed():ResponseSpeed = doPost2("speed/get", null)


    /**
     * 通过API推送订阅模板消息给到授权微信用户
     * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/One-time_subscription_info.html
     * TODO: 跳转重定向entry point的处理
     * */
    fun sendSubscribeMsg(msg: OneTimeTemplateMsg): Response = doPost2("template/subscribe", msg)
}



//============================= 模版消息 =============================//
/**
 * 待发送的模版消息
 * url和miniprogram都是非必填字段，若都不传则模板无跳转；若都传，会优先跳转至小程序。
 * 开发者可根据实际需要选择其中一种跳转方式即可。当用户的微信客户端版本不支持跳小程序时，将会跳转至url。
 *
 * @param toUser    是	接收者openid
 * @param templateId    是	模板ID
 * @param data    是	模板数据
 * @param url    否	模板跳转链接（海外帐号没有跳转能力）
 * @param mini    否	跳小程序所需数据，不需跳小程序可不用传该数据
 *
 */
@Serializable
class TemplateMsg(
        @SerialName("touser")
        val toUser: String,
        @SerialName("template_id")
        val templateId: String,
        val data: Map<String, ValueColor>,
        val url: String? = null,
        @SerialName("miniprogram")
        val mini: MiniProgram? = null
)

/**
 *
 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/One-time_subscription_info.html
 * @param toUser    是	接收者openid
 * @param templateId    是	订阅消息模板ID
 * @param data    是	模板数据
 * @param url    否	模板跳转链接（海外帐号没有跳转能力）
 * @param mini    否	跳小程序所需数据，不需跳小程序可不用传该数据
 * @param scene	是	订阅场景值
 * @param title	是	消息标题，15字以内
 * @param data	是	消息正文，value为消息内容文本（200字以内），没有固定格式，可用\n换行，
 * color为整段消息内容的字体颜色（目前仅支持整段消息为一种颜色）
 *
 * 注：url和miniprogram都是非必填字段，若都不传则模板无跳转；若都传，会优先跳转至小程序。开发者可根据实际需要选择其中一种跳转方式即可。
 * 当用户的微信客户端版本不支持跳小程序时，将会跳转至url。
 * */
@Serializable
class OneTimeTemplateMsg(
        @SerialName("touser")
        val toUser: String,
        @SerialName("template_id")
        val templateId: String,
        val scene: Int,
        val title: String,
        val data: Map<String, ValueColor>,
        val url: String? = null,
        @SerialName("miniprogram")
        val mini: MiniProgram? = null
){
    constructor(
                 toUser: String,
                 templateId: String,
                 scene: Int,
                 title: String,
                 content: String,
                 url: String? = null,
                 mini: MiniProgram? = null,
                 color: String  = "173177"):
            this(toUser, templateId, scene, title, mapOf("content" to ValueColor(content, color)),url, mini)
}

/**
 * 发送模板消息返回的结果
 * */
@Serializable
class ResponseSendTemplateMsg(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("msgid")
        val msgId: Long) : IBase


/**
 * 模板消息中的小程序
 * @param appid    是	所需跳转到的小程序appid（该小程序appid必须与发模板消息的公众号是绑定关联关系，暂不支持小游戏）
 * @param pagepath    否	所需跳转到小程序的具体页面路径，支持带参数,（示例index?foo=bar），要求该小程序已发布，暂不支持小游戏
 * */
@Serializable
class MiniProgram(
        @SerialName("appid")
        val appId: String,
        @SerialName("pagepath")
        val pagePath: String
)

/**
 * 模板消息中带颜色的值对
 * @param value 传递过去的值
 * @param color    否	模板内容字体颜色，不填默认为黑色
 * */
@Serializable
class ValueColor(
        val value: String,
        val color: String = "#173177",
)


//============================= 群发消息 =============================//

/**
 * @param msgId    消息发送任务的ID
 * @param msgDataId 消息的数据ID，该字段只有在群发图文消息时，才会出现。
 * 可以用于在图文分析数据接口中，获取到对应的图文消息的数据，是图文分析数据接口中的msgid字段中的前半部分，
 * 详见图文分析数据接口中的msgid字段的介绍。
 * */
@Serializable
class ResponseMassSend(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("msgid")
        val msgId: Long,
        @SerialName("msg_data_id")
        val msgDataId: Long? = null
) : IBase

/**
 *
 * @param speed	是	群发速度的级别
 * @param realSpeed realspeed	是	群发速度的真实值 单位：万/分钟
 * */
@Serializable
class ResponseSpeed(
      val speed: Int,
      @SerialName("realspeed")
      val realSpeed: Int? = null
)


/**
 * 发送目标，适用于各种类型的群发，只有其中一个不为空，其它须均为空
 * */
@Serializable
class Target(
        val tags: TagFilter? = null,
        val openIds: List<String>? = null,
        val openId: String? = null,
        val wxName: String? = null,
        var type: TargetType
){
    init {
        type = when{
            tags != null -> TargetType.Tag
            openIds != null -> TargetType.OpenIds
            openId != null -> TargetType.OpenId
            wxName != null -> TargetType.WxName
            else -> error("One of properties should have value")
        }
    }
}
/**
 * 不同的Filter type
 * @property Tag 用于tag过滤或所有人
 * @property OpenIds 用于massSendByOpenIds中的发送openId列表
 * @property OpenId 用于preview中的发送某个openId
 * @property WxName 用于preview中的发送某个微信号
 * */
enum class TargetType{
    Tag, OpenIds, OpenId, WxName
}



/**
 * 群发时过滤用户
 * @param isToAll    否	用于设定是否向全部用户发送，true: 发给所有用户，false:根据tag_id发送给指定群组的用户
 * @param tagId    否	群发到的标签的tag_id，参见用户管理中用户分组接口，若is_to_all值为true，可不填写tag_id
 * */
@Serializable
class TagFilter(
        @SerialName("is_to_all")
        val isToAll: Boolean = false,

        @SerialName("tagId")
        val tagId: Int? = null
)


/**
 * 此处是用户群发消息，而不是回复用户的消息，有所区别
 * @property target 消息接收者
 * @property msgType 消息类型
 * @property clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
 * */
interface IMsg{
    val target: Target
    val msgType: String
    val clientMsgId: String? //clientmsgid
    companion object {
        const val TEXT = "text"
        const val IMAGE = "image"
        const val VOICE = "voice"
        const val VIDEO = "mpvideo"
        const val MPNEWS = "mpnews"
        const val CARD = "wxcard"
    }
}
interface IContent

@Serializable
class TextContent(val content: String): IContent

@Serializable(with = TextMsgSerializer::class)
class TextMsg(
        val text: TextContent,
        override val target: Target,
        override val clientMsgId: String? = null,
        override val msgType: String = IMsg.TEXT
): IMsg{
    /**
     * convenience constructor
     * @param content  文本内容
     * @param target 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(content: String, target: Target, clientMsgId: String? = null): this(TextContent(content), target, clientMsgId)
}

object TextMsgSerializer : MsgSerializer<TextMsg>() {
    override fun serialName() = "TextMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<TextContent?>("text", isOptional = true)
    }
    override fun serializeContent(encoder: CompositeEncoder, msg: TextMsg):Int {
        encoder.encodeSerializableElement(descriptor, 0,TextContent.serializer(), msg.text)
        return 1
    }
}




/**
 * @param recommend 推荐语，不填则默认为“分享图片”
 * */
@Serializable
class ImgContent(
        @SerialName("media_ids")
        val mediaIds: List<String>,
        val recommend: String? = null,
        @SerialName("need_open_comment")
         val needOpenComment: Int = 0,
        @SerialName("only_fans_can_comment")
        val onlyFansCanComment: Int = 0
): IContent

@Serializable(with = ImgMsgSerializer::class)
class ImgMsg(
        val images: ImgContent,
        override val target: Target,
        override val clientMsgId: String? = null,
        override val msgType: String = IMsg.IMAGE
) : IMsg
{
    /**
     * convenience constructor
     * @param recommend 推荐语，不填则默认为“分享图片”
     * @param target 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(target: Target, mediaIds: List<String>, recommend: String,needOpenComment: Int = 0,onlyFansCanComment: Int = 0, clientMsgId: String? = null)
            : this(ImgContent(mediaIds,recommend,needOpenComment,onlyFansCanComment), target,clientMsgId)
}

object ImgMsgSerializer : MsgSerializer<ImgMsg>() {
    override fun serialName() = "ImgMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<ImgContent?>("images", isOptional = true)
    }
    override fun serializeContent(encoder: CompositeEncoder, msg: ImgMsg):Int {
        encoder.encodeSerializableElement(descriptor, 0, ImgContent.serializer(), msg.images)
        return 1
    }
}



@Serializable(with = VoiceMsgSerializer::class)
class VoiceContent(@SerialName("media_id")val mediaId: String): IContent

@Serializable
class VoiceMsg(
        val voice: VoiceContent,
        override val target: Target,
        override val clientMsgId: String? = null,
        override val msgType: String = IMsg.VOICE
) : IMsg
{
    /**
     * convenience constructor
     * @param target 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(target: Target, mediaId: String, clientMsgId: String? = null): this(VoiceContent(mediaId), target,clientMsgId)
}

object VoiceMsgSerializer : MsgSerializer<VoiceMsg>() {
    override fun serialName() = "VoiceMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<VoiceContent?>("voice", isOptional = true)
    }
    override fun serializeContent(encoder: CompositeEncoder, msg: VoiceMsg):Int {
        encoder.encodeSerializableElement(descriptor, 0,VoiceContent.serializer(), msg.voice)
        return 1
    }
}


@Serializable
class VideoContent(
        @SerialName("media_id")
        val mediaId: String,
        val title: String?,
        val description: String?
): IContent

@Serializable(with = VideoMsgSerializer::class)
class VideoMsg(
        val mpvideo: VideoContent,
        override val target: Target,
        override val clientMsgId: String? = null,
        override val msgType: String = IMsg.VIDEO
) : IMsg
object VideoMsgSerializer : MsgSerializer<VideoMsg>() {
    override fun serialName() = "VideoMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<VideoContent?>("mpvideo", isOptional = true)
    }
    override fun serializeContent(encoder: CompositeEncoder, msg: VideoMsg):Int {
        encoder.encodeSerializableElement(descriptor, 0,VideoContent.serializer(), msg.mpvideo)
        return 1
    }
}



@Serializable
class CardContent(@SerialName("card_id")val cardId: String): IContent

@Serializable(with = CardMsgSerializer::class)
class CardMsg(
        val wxcard: CardContent,
        override val target: Target,
        override val clientMsgId: String? = null,
        override val msgType: String = IMsg.CARD
) : IMsg
{
    /**
     * convenience constructor
     * @param target 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(target: Target, cardId: String, clientMsgId: String? = null): this(CardContent(cardId), target,clientMsgId)
}
object CardMsgSerializer : MsgSerializer<CardMsg>() {
    override fun serialName() = "CardMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<CardContent?>("wxcard", isOptional = true)
    }
    override fun serializeContent(encoder: CompositeEncoder, msg: CardMsg):Int {
        encoder.encodeSerializableElement(descriptor, 0, CardContent.serializer(), msg.wxcard)
        return 1
    }
}

@Serializable
class MpNewsContent(@SerialName("media_id")val mediaId: String)

@Serializable(with = MpNewsMsgSerializer::class)
class MpNewsMsg(
        val mpnews: MpNewsContent,
        val ignoreReprint: Int = 0,
        override val target: Target,
        override val clientMsgId: String? = null,
        override val msgType: String = IMsg.MPNEWS
) : IMsg
{
    /**
     * @param target 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * @param ignoreReprint 指定待群发的文章被判定为转载时，是否继续群发。
     * 设置为1时，文章被判定为转载时，且原创文允许转载时，将继续进行群发操作。
     * 设置为0时，文章被判定为转载时，将停止群发操作。默认为0
     * */
    constructor(target: Target, mediaId: String, ignoreReprint: Int = 0, clientMsgId: String? = null)
            : this( MpNewsContent(mediaId),ignoreReprint,target,clientMsgId)
}
object MpNewsMsgSerializer : MsgSerializer<MpNewsMsg>() {
    override fun serialName() = "MpNewsMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<MpNewsContent?>("mpnews", isOptional = true)
        builder.element<Int?>("send_ignore_reprint", isOptional = true)
    }
    override fun serializeContent(encoder: CompositeEncoder, msg: MpNewsMsg):Int {
        encoder.encodeSerializableElement(descriptor, 0, MpNewsContent.serializer(), msg.mpnews)
        encoder.encodeIntElement(descriptor, 1, msg.ignoreReprint)
        return 2
    }
}


abstract class MsgSerializer<T: IMsg>: KSerializer<T>{
    override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor(serialName()) {
                //element<Int?>("text", isOptional = true)
                addContentElement(this)
                element<String>("msgtype", isOptional = true)
                element<String?>("clientmsgid", isOptional = true)
                element<String>("filter", isOptional = true)
                element<String>("touser", isOptional = true)
                element<String>("towxname", isOptional = true)
            }
    override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeStructure(descriptor) {
                val count = serializeContent(this, value)
                //encodeSerializableElement(descriptor, 0,TextContent.serializer(), value.text)
                encodeStringElement(TextMsgSerializer.descriptor, count, value.msgType)
                if(!value.clientMsgId.isNullOrBlank())encodeStringElement(TextMsgSerializer.descriptor, count+1, value.clientMsgId!!)
                when (value.target.type) {
                    TargetType.Tag -> encodeSerializableElement(TextMsgSerializer.descriptor, count+2, TagFilter.serializer(), value.target.tags!!)
                    TargetType.OpenIds -> encodeSerializableElement(TextMsgSerializer.descriptor, count+3, ListSerializer(String.serializer()), value.target.openIds!!)
                    TargetType.OpenId -> encodeStringElement(TextMsgSerializer.descriptor, count+3, value.target.openId!!)
                    TargetType.WxName -> encodeStringElement(TextMsgSerializer.descriptor, count+4, value.target.wxName!!)
                }
            }

    override fun deserialize(decoder: Decoder): T {
        TODO("Not implement")
    }

    /**
     * 名称
     * */
    abstract fun serialName(): String

    /**
     * 添加content的element索引
     * */
    abstract fun addContentElement(builder: ClassSerialDescriptorBuilder)

    /**
     * 序列化content正文，返回addContentElement添加的element数量
     * */
    abstract fun serializeContent(encoder: CompositeEncoder, msg: T): Int
}
