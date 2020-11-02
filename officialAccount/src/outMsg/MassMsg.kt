package com.github.rwsbillyang.wxSDK.officialAccount.outMsg



import com.github.rwsbillyang.wxSDK.msg.IMsg
import com.github.rwsbillyang.wxSDK.msg.ImagesContent
import com.github.rwsbillyang.wxSDK.msg.VideoContent
import kotlinx.serialization.KSerializer
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

//============================= 群发时发送出去的消息 =============================//


/**
 * @property receivers 消息接收者
 * @property clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
 * 使用 clientmsgid 参数，避免重复推送 群发时，微信后台将对 24 小时内的群发记录进行检查，如果该 clientmsgid 已经存在一条群发记录，
 * 则会拒绝本次群发请求，返回已存在的群发msgid，开发者可以调用“查询群发消息发送状态”接口查看该条群发的状态。
 * */
interface IMassMsg: IMsg {
    val receivers: MsgReceivers
    val clientMsgId: String? //clientmsgid
}



//============================= 群发之 图片消息 =============================//

/**
 * 群发消息的图片消息，若是客服消息请使用 CustomerImgMsg,二者内容上有区别
 * */
@Serializable(with = ImgMsgSerializer::class)
class ImgsMsg(
    val images: ImagesContent,
    override val receivers: MsgReceivers,
    override val clientMsgId: String? = null
) : IMassMsg {
    override val msgType: String = IMsg.IMAGE
    /**
     * convenience constructor
     * @param recommend 推荐语，不填则默认为“分享图片”
     * @param receivers 消息接收者
     * @param clientMsgId 开发者侧群发消息id，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid
     * */
    constructor(receivers: MsgReceivers, mediaIds: List<String>, recommend: String, needOpenComment: Int = 0, onlyFansCanComment: Int = 0, clientMsgId: String? = null)
            : this(ImagesContent(mediaIds, recommend, needOpenComment, onlyFansCanComment), receivers, clientMsgId)
}

object ImgMsgSerializer : MassMsgSerializer<ImgsMsg>() {
    override fun serialName() = "ImgMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<ImagesContent?>("images", isOptional = true)
    }

    override fun serializeContent(encoder: CompositeEncoder, msg: ImgsMsg): Int {
        encoder.encodeSerializableElement(descriptor, 0, ImagesContent.serializer(), msg.images)
        return 1
    }
}




//============================= 群发 之 视频消息 =============================//
@Serializable(with = VideoMsgSerializer::class)
class VideoMsg(
    val mpvideo: VideoContent,
    override val receivers: MsgReceivers,
    override val clientMsgId: String? = null
) : IMassMsg{
    override val msgType: String = IMsg.MPVIDEO
}

object VideoMsgSerializer : MassMsgSerializer<VideoMsg>() {
    override fun serialName() = "VideoMsg"
    override fun addContentElement(builder: ClassSerialDescriptorBuilder) {
        builder.element<VideoContent?>("mpvideo", isOptional = true)
    }

    override fun serializeContent(encoder: CompositeEncoder, msg: VideoMsg): Int {
        encoder.encodeSerializableElement(descriptor, 0, VideoContent.serializer(), msg.mpvideo)
        return 1
    }
}


abstract class MassMsgSerializer<T : IMassMsg> : KSerializer<T> {
    override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor(serialName()) {
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
                encodeStringElement(descriptor, count, value.msgType)
                if (!value.clientMsgId.isNullOrBlank()) encodeStringElement(descriptor, count + 1, value.clientMsgId!!)
                when (value.receivers.type) {
                    ReceiverType.Tag -> encodeSerializableElement(descriptor, count + 2, TagFilter.serializer(), value.receivers.tags!!)
                    ReceiverType.OpenIds -> encodeSerializableElement(descriptor, count + 3, ListSerializer(String.serializer()), value.receivers.openIds!!)
                    ReceiverType.OpenId -> encodeStringElement(descriptor, count + 3, value.receivers.openId!!)
                    ReceiverType.WxName -> encodeStringElement(descriptor, count + 4, value.receivers.wxName!!)
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

