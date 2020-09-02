package com.github.rwsbillyang.wxSDK.officialAccount.msg

import com.github.rwsbillyang.wxSDK.common.msg.MsgBuilder
import com.github.rwsbillyang.wxSDK.common.msg.ReBaseMSg
import kotlinx.serialization.Serializable



class ReTextMsg(
    val content: String,
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis()
) : ReBaseMSg(toUserName, fromUserName, createTime, TEXT) {
    private var sb = StringBuilder(content)

    fun add(text: String) = apply { sb.append(text) }
    fun addln(text: String? = "") = apply { sb.append("$text\n") }
    fun addLink(text: String, url: String) = apply { sb.append("<a href=\"").append(url).append("\">").append(text).append("</a>") }

    override fun addMsgContent(builder: MsgBuilder) {
        builder.addData("Content", sb.toString().trim { it <= ' ' })
    }
}


/**
 * 回复图片消息
 * */
class ReImgMsg(
    val mediaId: String,
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis()

) : ReBaseMSg(toUserName, fromUserName, createTime, IMAGE) {
    override fun addMsgContent(builder: MsgBuilder) {
        builder.append("<Image>\n")
        builder.addData("MediaId", mediaId)
        builder.append("</Image>\n")
    }
}

/**
 * 回复语音消息
 * */
class ReVoiceMsg(
    val mediaId: String,
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis()

) : ReBaseMSg(toUserName, fromUserName, createTime, IMAGE) {
    override fun addMsgContent(builder: MsgBuilder) {
        builder.append("<Voice>\n")
        builder.addData("MediaId", mediaId)
        builder.append("</Voice>\n")
    }
}

/**
 * 回复视频消息
 * */
class ReVideoMsg(
    val mediaId: String,
    val title: String? = null,
    val description: String? = null,
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis()

) : ReBaseMSg(toUserName, fromUserName, createTime, VIDEO) {
    override fun addMsgContent(builder: MsgBuilder) {
        builder.append("<Video>\n")
        builder.addData("MediaId", mediaId)
        builder.addData("Title", title)
        builder.addData("Description", description)
        builder.append("</Video>\n")
    }
}


/**
 * 回复音乐消息
 *
 * @param musicUrl 音乐链接
 * @param hqMusicUrl 高质量音乐链接，WIFI环境优先使用该链接播放音乐
 * @param thumbMediaId 缩略图的媒体id，通过素材管理中的接口上传多媒体文件，得到的id
 * @param title 音乐标题
 * @param description 音乐描述
 * */
class ReMusicMsg(
    val musicUrl: String? = null,
    val hqMusicUrl: String? = null,
    val thumbMediaId: String? = null,
    val title: String? = null,
    val description: String? = null,
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis()

) : ReBaseMSg(toUserName, fromUserName, createTime, MUSIC) {
    override fun addMsgContent(builder: MsgBuilder) {
        builder.append("<Music>\n")
        builder.addData("Title", title)
        builder.addData("Description", description)
        builder.addData("MusicUrl", musicUrl)
        builder.addData("HQMusicUrl", hqMusicUrl)
        builder.addData("ThumbMediaId", thumbMediaId)
        builder.append("</Music>\n")
    }
}

/**
 * 回复音乐消息
 *
 * @param title Title	是	图文消息标题
 * @param description Description	是	图文消息描述
 * @param picUrl PicUrl	是	图片链接，支持JPG、PNG格式，较好的效果为大图360*200，小图200*200
 * @param url Url	是	点击图文消息跳转链接
 * */
@Serializable
class Article(
    val title: String,
    val description: String,
    val picUrl: String,
    val url: String
)

/**
 * 回复图文消息
 *
 * @param articleCount ArticleCount	是	图文消息个数；
 * 当用户发送文本、图片、语音、视频、图文、地理位置这六种消息时，开发者只能回复1条图文消息；
 * 其余场景最多可回复8条图文消息
 *
 * @param articles Articles	是	图文消息信息，注意，如果图文数超过限制，则将只发限制内的条数
 * */
class ReNewsMsg(
    val articles: List<Article>,
    val articleCount: Int = articles.size,
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis()
) : ReBaseMSg(toUserName, fromUserName, createTime, NEWS) {
    init {
        require(articles.size < 8) { "当用户发送文本、图片、语音、视频、图文、地理位置这六种消息时，开发者只能回复1条图文消息； 其余场景最多可回复8条图文消息" }
    }

    override fun addMsgContent(builder: MsgBuilder) {
        builder.addTag("ArticleCount", articles.size.toString())
        builder.append("<Articles>\n")
        for (article in articles) {
            builder.append("<item>\n")
            builder.addData("Title", article.title)
            builder.addData("Description", article.description)
            builder.addData("PicUrl", article.picUrl)
            builder.addData("Url", article.url)
            builder.append("</item>\n")
        }
        builder.append("</Articles>\n")
    }
}

/**
 * 消息转发到客服
 *
 * 如果公众号处于开发模式，普通微信用户向公众号发消息时，微信服务器会先将消息POST到开发者填写的url上，
 * 如果希望将消息转发到客服系统，则需要开发者在响应包中返回MsgType为transfer_customer_service的消息，
 * 微信服务器收到响应后会把当次发送的消息转发至客服系统。您也可以在返回transfer_customer_service消息时，
 * 在XML中附上TransInfo信息指定分配给某个客服帐号。
 *
 * 用户被客服接入以后，客服关闭会话以前，处于会话过程中时，用户发送的消息均会被直接转发至客服系统。当会话超过30分钟客服没有关闭时，
 * 微信服务器会自动停止转发至客服，而将消息恢复发送至开发者填写的url上。
 *
 * 用户在等待队列中时，用户发送的消息仍然会被推送至开发者填写的url上。
 *
 * 这里特别要注意，只针对微信用户发来的消息才进行转发，而对于其他任何事件（比如菜单点击、地理位置上报等）都不应该转接，
 * 否则客服在客服系统上就会看到一些无意义的消息了。
 *
 * https://developers.weixin.qq.com/doc/offiaccount/Customer_Service/Forwarding_of_messages_to_service_center.html
 *
 *
 * 消息转发到指定客服
 *
 * 如果您有多个客服人员同时登录了客服并且开启了自动接入在进行接待，每一个客户的消息转发给客服时，多客服系统会将客户分配给其中一个客服人员。
 * 如果您希望将某个客户的消息转给指定的客服来接待，可以在返回transfer_customer_service消息时附上TransInfo信息指定一个客服帐号。
 * 需要注意，如果指定的客服没有接入能力(不在线、没有开启自动接入或者自动接入已满)，该用户仍然会被直接接入到指定客服，不再通知其它客服，
 * 不会被其他客服接待。建议在指定客服时，先查询客服的接入能力（获取在线客服接待信息接口），指定到有能力接入的客服，保证客户能够及时得到服务。
 *
 * @param kfAccount 完整客服帐号，格式为：帐号前缀@公众号微信号，帐号前缀最多10个字符，必须是英文、数字字符或者下划线，后缀为公众号微信号，
 * 长度不超过30个字符.  请注意，必须先在公众平台官网为公众号设置微信号后才能使用该能力。
 * */
class ReTransferMsg(
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis(),
    val kfAccount: String? = null
): ReBaseMSg(toUserName, fromUserName, createTime, TRANSFER_TO_CUSTOMER_SERVICE) {
    override fun addMsgContent(builder: MsgBuilder) {
        if(!kfAccount.isNullOrBlank()){
            builder.append("<TransInfo>\n")
            builder.addData("KfAccount", kfAccount)
            builder.append("</TransInfo>\n")
        }
    }
}