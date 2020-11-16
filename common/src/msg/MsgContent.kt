package com.github.rwsbillyang.wxSDK.msg


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



/**
 * 消息所使用的场景
 * @property RE 被动回复消息
 * @property MASS 群发消息
 * @property KF 公众号客服消息
 * */
@Serializable
sealed class MsgBody(val flag: Byte) {
    companion object {
        const val RE: Byte = 0b0000_0001 //1
        const val MASS: Byte = 0b0000_0010 //2
        const val KF: Byte = 0b0000_0100 //4

        const val RE_KF: Byte = 0b0000_0101 //5
        const val RE_MASS: Byte = 0b0000_0011 //3
        const val MASS_KF: Byte = 0b0000_0110 //6
        const val ALL: Byte = 0b0000_0111 //7
    }
}

@Serializable
@SerialName(MsgType.TEXT)
data class TextContent(val content: String) : MsgBody(ALL)

@Serializable
@SerialName(MsgType.VOICE)
data class VoiceContent(
        @SerialName("media_id")
        val mediaId: String
) : MsgBody(ALL)



/**
 * 图片消息 （被动回复消息、客服消息）
 * */
@Serializable
@SerialName(MsgType.IMAGE)
data class ImageContent(
        @SerialName("media_id")
        val mediaId: String
) : MsgBody(RE_KF)

/**
 * 多图片消息（群发消息）
 * @param recommend 推荐语，不填则默认为“分享图片”
 * @param  needOpenComment need_open_comment	否	Uint32 是否打开评论，0不打开，1打开
 * @param  onlyFansCanComment only_fans_can_comment	否	Uint32 是否粉丝才可评论，0所有人可评论，1粉丝才可评论
 * */
@Serializable
@SerialName("images")
data class ImagesContent(
        @SerialName("media_ids")
        val mediaIds: List<String>,
        val recommend: String? = null,
        @SerialName("need_open_comment")
        val needOpenComment: Int = 0,
        @SerialName("only_fans_can_comment")
        val onlyFansCanComment: Int = 0
) : MsgBody(MASS)




/**
 * 视频消息（被动回复, 群发）
 * */
@Serializable
@SerialName(MsgType.VIDEO)
data class VideoContent(
        @SerialName("media_id")
        val mediaId: String,
        val title: String? = null,
        val description: String? = null
) : MsgBody(RE_MASS)



/**
 * 视频信息（客服消息）， 多了一个thumb字段
 * @param thumb 缩略图媒体ID
 * */
@Serializable
@SerialName("video_kf")
data class VideoKfContent(
        @SerialName("media_id")
        val mediaId: String,
        @SerialName("thumb_media_id")
        val thumb: String,
        val title: String,
        val description: String
) : MsgBody(KF)



/**
 * 音乐消息（被动回复消息、客服消息，群发无音乐消息类型）
 *
 * Title	否	音乐标题
 * Description	否	音乐描述
 * MusicURL	否	音乐链接
 * HQMusicUrl	否	高质量音乐链接，WIFI环境优先使用该链接播放音乐
 * ThumbMediaId	是	缩略图的媒体id，通过素材管理中的接口上传多媒体文件，得到的id
 * */
@Serializable
@SerialName(MsgType.MUSIC)
data class MusicContent(
        @SerialName("thumb_media_id")
        val thumbMediaId: String,
        @SerialName("musicurl")
        val musicUrl: String? = null,
        @SerialName("hqmusicurl")
        val hqMusicUrl: String? = null,
        val title: String? = null,
        val description: String? = null
) : MsgBody(RE_KF)





/**
 * 图文消息项, 既用于被动回复的图文消息（序列化为xml），也用于客服消息（序列化为JSON）之中
 *
 * @param title Title	是	图文消息标题
 * @param description Description	是	图文消息描述
 * @param picUrl PicUrl	是	图片链接，支持JPG、PNG格式，较好的效果为大图360*200，小图200*200
 * @param url Url	是	点击图文消息跳转链接
 * */
@Serializable
class ArticleItem(
        val title: String,
        val description: String,
        @SerialName("picurl")
        val picUrl: String,
        val url: String
)

/**
 * 被动回复消息：图文消息个数；当用户发送文本、图片、语音、视频、图文、地理位置这六种消息时，开发者只能回复1条图文消息；
 * 其余场景最多可回复8条图文消息
 *
 * 发送图文客服消息（点击跳转到外链） 图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
@Serializable
@SerialName(MsgType.NEWS)
data class NewsContent(
        val articles: List<ArticleItem>
) : MsgBody(RE_KF)


/**
 * 发送图文消息（点击跳转到图文消息页面） 图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
@Serializable
@SerialName(MsgType.MPNEWS)
data class MpNewsContent(
        @SerialName("media_id")
        val mediaId: String
) : MsgBody(MASS_KF)


/**
 * 卡券消息(群发，客服)
 * 特别注意客服消息接口投放卡券仅支持非自定义Code码和导入code模式的卡券的卡券
 * */
@Serializable
@SerialName(MsgType.CARD)
class CardContent(@SerialName("card_id") val cardId: String): MsgBody(MASS_KF)



//============================= 客服 之 菜单消息 =============================//
/**
 * 菜单消息（客服）
 * 用户会看到这样的菜单消息：
 * “您对本次服务是否满意呢？
 * 满意
 * 不满意”
 * 其中，“满意”和“不满意”是可点击的，当用户点击后，微信会发送一条XML消息到开发者服务器
 * */
@Serializable
@SerialName(MsgType.MENU)
class MenuContent(
    @SerialName("head_content")
        val headContent: String,
    val list: List<IdContent>,
    @SerialName("tail_content")
        val tailContent: String
): MsgBody(KF)
/**
 * 诸如 "id": "101", "content": "满意" 和 "id": "102","content": "不满意" 等
 * */
@Serializable
class IdContent(
        val id: String,
        val content: String
)




//============================= 客服 之 mini program消息 =============================//
/**
 * @param title	否	小程序卡片的标题
 * @param thumb 是 缩略图/小程序卡片图片的媒体ID，小程序卡片图片建议大小为520*416
 * @param appId appid	是	小程序的appid，要求小程序的appid需要与公众号有关联关系
 * @param pagePath pagepath	是	小程序的页面路径，跟app.json对齐，支持参数，比如pages/index/index?foo=bar
 * */
@Serializable
@SerialName(MsgType.MINI_PROGRAM)
class MiniProgramContent(
        @SerialName("appid")
        val appId: String,
        @SerialName("pagepath")
        val pagePath: String,
        @SerialName("thumb_media_id")
        val thumb: String,
        val title: String? = null
): MsgBody(KF)