package com.github.rwsbillyang.wxSDK.officialAccount.outMsg

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeEncoder

//============================= 群发时发送出去的消息 =============================//


/**
 * @property clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
 * */
interface IMassMsg: IMsg {
    val receivers: MsgReceivers
    val clientMsgId: String? //clientmsgid
}



//============================= 群发之 图片消息 =============================//

/**
 *
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
)

/**
 * 群发消息的图片消息，若是客服消息请使用 CustomerImgMsg,二者内容上有区别
 * */
@Serializable(with = ImgMsgSerializer::class)
class ImgMsg(
        val images: ImgContent,
        override val receivers: MsgReceivers,
        override val clientMsgId: String? = null,
        override val msgType: String = IMsg.IMAGE
) : IMassMsg {
    /**
     * convenience constructor
     * @param recommend 推荐语，不填则默认为“分享图片”
     * @param receivers 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(receivers: MsgReceivers, mediaIds: List<String>, recommend: String, needOpenComment: Int = 0, onlyFansCanComment: Int = 0, clientMsgId: String? = null)
            : this(ImgContent(mediaIds, recommend, needOpenComment, onlyFansCanComment), receivers, clientMsgId)
}

object ImgMsgSerializer : MassMsgSerializer<ImgMsg>() {
    override fun serialName() = "ImgMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<ImgContent?>("images", isOptional = true)
    }

    override fun serializeContent(encoder: CompositeEncoder, msg: ImgMsg): Int {
        encoder.encodeSerializableElement(descriptor, 0, ImgContent.serializer(), msg.images)
        return 1
    }
}


