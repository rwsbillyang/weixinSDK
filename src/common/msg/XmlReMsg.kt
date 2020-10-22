package com.github.rwsbillyang.wxSDK.common.msg



/**
 * 被动回复消息 文本消息
 * */
class ReTextMsg(
    val content: String,
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis()
) : ReBaseMSg(toUserName, fromUserName, createTime, InMsgType.TEXT) {
    private var sb = StringBuilder(content)

    fun add(text: String) = apply { sb.append(text) }
    fun addln(text: String? = "") = apply { sb.append("$text\n") }
    fun addLink(text: String, url: String) = apply { sb.append("<a href=\"").append(url).append("\">").append(text).append("</a>") }

    override fun addMsgContent(builderXml: XmlMsgBuilder) {
        builderXml.addData("Content", sb.toString().trim { it <= ' ' })
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

) : ReBaseMSg(toUserName, fromUserName, createTime, InMsgType.IMAGE) {
    override fun addMsgContent(builderXml: XmlMsgBuilder) {
        builderXml.append("<Image>\n")
        builderXml.addData("MediaId", mediaId)
        builderXml.append("</Image>\n")
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

) : ReBaseMSg(toUserName, fromUserName, createTime, InMsgType.IMAGE) {
    override fun addMsgContent(builderXml: XmlMsgBuilder) {
        builderXml.append("<Voice>\n")
        builderXml.addData("MediaId", mediaId)
        builderXml.append("</Voice>\n")
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

) : ReBaseMSg(toUserName, fromUserName, createTime, InMsgType.VIDEO) {
    override fun addMsgContent(builderXml: XmlMsgBuilder) {
        builderXml.append("<Video>\n")
        builderXml.addData("MediaId", mediaId)
        builderXml.addData("Title", title)
        builderXml.addData("Description", description)
        builderXml.append("</Video>\n")
    }
}




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
    val articles: List<ArticleItem>,
    val articleCount: Int = articles.size,
    toUserName: String?,
    fromUserName: String?,
    createTime: Long = System.currentTimeMillis()
) : ReBaseMSg(toUserName, fromUserName, createTime, InMsgType.NEWS) {
    init {
        require(articles.size < 8) { "当用户发送文本、图片、语音、视频、图文、地理位置这六种消息时，开发者只能回复1条图文消息； 其余场景最多可回复8条图文消息" }
    }

    override fun addMsgContent(builderXml: XmlMsgBuilder) {
        builderXml.addTag("ArticleCount", articles.size.toString())
        builderXml.append("<Articles>\n")
        for (article in articles) {
            builderXml.append("<item>\n")
            builderXml.addData("Title", article.title)
            builderXml.addData("Description", article.description)
            builderXml.addData("PicUrl", article.picUrl)
            builderXml.addData("Url", article.url)
            builderXml.append("</item>\n")
        }
        builderXml.append("</Articles>\n")
    }
}