package com.github.rwsbillyang.wxSDK.officialAccount.outMsg

import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.officialAccount.outMsg.ReceiverType.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

//============================= 群发、客服 共用发送出去的消息 =============================//



interface ICustomerAndMassMsg: IMassMsg, ICustomerMsg


/**
 * 不同的Filter type
 * @property Tag 用于tag过滤或所有人
 * @property OpenIds 用于massSendByOpenIds中的发送openId列表
 * @property OpenId 用于preview中的发送某个openId
 * @property WxName 用于preview中的发送某个微信号
 * */
enum class ReceiverType {
    Tag, OpenIds, OpenId, WxName
}
/**
 * 发送目标，适用于各种类型的群发，只有其中一个不为空，其它须均为空
 * */
@Serializable
class MsgReceivers(
        var type: ReceiverType,
        val tags: TagFilter? = null,
        val openIds: List<String>? = null,
        val openId: String? = null,
        val wxName: String? = null
) {
    init {
        type = when {
            tags != null -> ReceiverType.Tag
            openIds != null -> ReceiverType.OpenIds
            openId != null -> ReceiverType.OpenId
            wxName != null -> ReceiverType.WxName
            else -> error("One of properties should have value")
        }
    }
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





//============================= 群发和客服 之 文本消息 =============================//


@Serializable(with = TextMsgSerializer::class)
class TextMsg(
        val text: TextContent,
        override val receivers: MsgReceivers,
        @SerialName("customservice")
        override val customService: KfAccountName? = null,
        override val clientMsgId: String? = null
) : ICustomerAndMassMsg {
    override val msgType: String = IMsg.TEXT
    /**
     * convenience constructor
     * @param content  文本内容
     * @param receivers 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(content: String, receivers: MsgReceivers, clientMsgId: String? = null) : this(TextContent(content), receivers,null, clientMsgId)
    companion object{
        /**
         * 客服消息中可以有小程序链接（小程序必须与公众号有绑定关系），群发消息官方中没有明确说明
         *
         * 构建插入小程序链接的文本
         * @param appId 小程序appid，则表示该链接跳小程序
         * @param pagePath 小程序路径，路径与app.json中保持一致，可带参数
         * @param text 展示的超链接文字
         * @param href 不支持小程序的客户端版本，如果有herf项，则仍然保持跳href中的网页链接
         * */
        fun insertMiniProgram(appId: String, pagePath: String, text: String, href: String)
                = "<a href=\"$href\" data-miniprogram-appid=\"$appId\" data-miniprogram-path=\"$pagePath\">$text</a>"
    }
}

object TextMsgSerializer : MsgSerializer<TextMsg>() {
    override fun serialName() = "TextMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<TextContent?>("text", isOptional = true)
    }

    override fun serializeContent(encoder: CompositeEncoder, msg: TextMsg): Int {
        encoder.encodeSerializableElement(descriptor, 0, String.serializer(), msg.text.content)
        return 1
    }
}

//============================= 群发和客服 之 语音消息 =============================//

@Serializable(with = VoiceMsgSerializer::class)
class VoiceMsg(
        val voice: VoiceContent,
        override val receivers: MsgReceivers,
        @SerialName("customservice")
        override val customService: KfAccountName? = null,
        override val clientMsgId: String? = null
) : ICustomerAndMassMsg {
    override val msgType: String = IMsg.VOICE
    /**
     * convenience constructor
     * @param receivers 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(receivers: MsgReceivers, mediaId: String, clientMsgId: String? = null) : this(VoiceContent(mediaId), receivers, null,clientMsgId)
}

object VoiceMsgSerializer : MsgSerializer<VoiceMsg>() {
    override fun serialName() = "VoiceMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<VoiceContent?>("voice", isOptional = true)
    }

    override fun serializeContent(encoder: CompositeEncoder, msg: VoiceMsg): Int {
        encoder.encodeSerializableElement(descriptor, 0, String.serializer(), msg.voice.mediaId)
        return 1
    }
}


//============================= 群发和客服 之 卡券消息 =============================//
/**
 * 特别注意客服消息接口投放卡券仅支持非自定义Code码和导入code模式的卡券的卡券
 * */
@Serializable(with = CardMsgSerializer::class)
class CardMsg(
        val wxcard: CardContent,
        override val receivers: MsgReceivers,
        @SerialName("customservice")
        override val customService: KfAccountName? = null,
        override val clientMsgId: String? = null
) : ICustomerAndMassMsg {
    override val msgType: String = IMsg.CARD
    /**
     * convenience constructor
     * @param receivers 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(receivers: MsgReceivers, cardId: String, clientMsgId: String? = null) : this(CardContent(cardId), receivers,null, clientMsgId)
}

object CardMsgSerializer : MsgSerializer<CardMsg>() {
    override fun serialName() = "CardMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<CardContent?>("wxcard", isOptional = true)
    }

    override fun serializeContent(encoder: CompositeEncoder, msg: CardMsg): Int {
        encoder.encodeSerializableElement(descriptor, 0, String.serializer(), msg.wxcard.cardId)
        return 1
    }
}

//============================= 群发和客服 之 图文消息 =============================//
/**
 * 发送图文消息（点击跳转到图文消息页面）
 * */
@Serializable(with = MpNewsMsgSerializer::class)
class MpNewsMsg(
        val mpnews: MpNewsContent,
        val ignoreReprint: Int = 0,
        override val receivers: MsgReceivers,
        @SerialName("customservice")
        override val customService: KfAccountName? = null,
        override val clientMsgId: String? = null,
        override val msgType: String = IMsg.MPNEWS
) : ICustomerAndMassMsg {
    /**
     * 群发消息
     * @param receivers 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * @param ignoreReprint 指定待群发的文章被判定为转载时，是否继续群发。
     * 设置为1时，文章被判定为转载时，且原创文允许转载时，将继续进行群发操作。
     * 设置为0时，文章被判定为转载时，将停止群发操作。默认为0
     * */
    constructor(receivers: MsgReceivers, mediaId: String, ignoreReprint: Int = 0, clientMsgId: String? = null)
            : this(MpNewsContent(mediaId),ignoreReprint, receivers,null, clientMsgId)

    /**
     * 客服消息
     * @param openId 消息接收者
     * @param mediaId 消息内容 图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
     * */
    constructor(openId: String, mediaId: String): this(MpNewsContent(mediaId),receivers = MsgReceivers(ReceiverType.OpenId, openId = openId))
}

object MpNewsMsgSerializer : MsgSerializer<MpNewsMsg>() {
    override fun serialName() = "MpNewsMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<MpNewsContent?>("mpnews", isOptional = true)
        builder.element<Int?>("send_ignore_reprint", isOptional = true)
    }

    override fun serializeContent(encoder: CompositeEncoder, msg: MpNewsMsg): Int {
        encoder.encodeSerializableElement(descriptor, 0, MpNewsContent.serializer(), msg.mpnews)
        encoder.encodeIntElement(descriptor, 1, msg.ignoreReprint)
        return 2
    }
}

//============================= MsgSerializer =============================//

abstract class MsgSerializer<T : ICustomerAndMassMsg> : MassMsgSerializer<T>() {
    override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor(serialName()) {
                addContentElement(this)
                element<String>("msgtype", isOptional = true)
                element<String?>("clientmsgid", isOptional = true)
                element<String>("filter", isOptional = true)
                element<String>("touser", isOptional = true)
                element<String>("towxname", isOptional = true)
                element<String>("customservice", isOptional = true)
            }

    override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeStructure(descriptor) {
                val count = serializeContent(this, value)
                encodeStringElement(descriptor, count, value.msgType)
                if (!value.clientMsgId.isNullOrBlank()) encodeStringElement(descriptor, count + 1, value.clientMsgId!!)
                when (value.receivers.type) {
                    ReceiverType.Tag -> encodeSerializableElement(descriptor, count + 2, TagFilter.serializer(), value.receivers.tags!!)
                    ReceiverType.OpenIds -> encodeSerializableElement(descriptor, count + 3, ListSerializer(String.serializer()), value.receivers.openIds!!)
                    ReceiverType.OpenId -> encodeStringElement(descriptor, count + 3, value.receivers.openId!!)
                    ReceiverType.WxName -> encodeStringElement(descriptor, count + 4, value.receivers.wxName!!)
                }
                if(value.customService != null) encodeSerializableElement(descriptor, count+5, KfAccountName.serializer(), value.customService!!)
            }

}

