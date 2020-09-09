package com.github.rwsbillyang.wxSDK.common.msg

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * 被动回复消息 文本消息
 * */
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
 * 被动回复消息 图片消息
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
 * 被动回复消息 语音消息
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
 * 被动回复消息 视频消息
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
 * 图文消息项, 既用于被动回复的图文消息（序列化为xml），也用于客服消息（序列化为JSON）之中
 *
 * @param title Title	是	图文消息标题
 * @param description Description	是	图文消息描述
 * @param picUrl PicUrl	是	图片链接，支持JPG、PNG格式，较好的效果为大图360*200，小图200*200
 * @param url Url	是	点击图文消息跳转链接
 * */
@Serializable
class ReArticleItem(
    val title: String,
    val description: String,
    @SerialName("picurl")
    val picUrl: String,
    val url: String
)

/**
 * 被动回复消息 图文消息
 *
 * @param articleCount ArticleCount	是	图文消息个数；
 * 当用户发送文本、图片、语音、视频、图文、地理位置这六种消息时，开发者只能回复1条图文消息；
 * 其余场景最多可回复8条图文消息
 *
 * @param articles Articles	是	图文消息信息，注意，如果图文数超过限制，则将只发限制内的条数
 * */
class ReNewsMsg(
    val articles: List<ReArticleItem>,
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