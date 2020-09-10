package com.github.rwsbillyang.wxSDK.officialAccount.outMsg

import com.github.rwsbillyang.wxSDK.common.msg.ReArticleItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//============================= 客服发送出去的消息 =============================//
/**
 * @property customService 以某个客服帐号来发消息（在微信6.0.2及以上版本中显示自定义头像），则需在JSON数据包的后半部分加入customservice参数
 * */
interface ICustomerMsg : IMsg {
    companion object {
        const val MUSIC = "music"
        const val MENU = "msgmenu"
        const val NEWS = "news"
        const val MINI_PROGRAM = "miniprogrampage"
    }
    val customService: KfAccountName?
}
@Serializable
class KfAccountName(
        @SerialName("kf_account")
        val name: String
)
//============================= 客服 之 图片消息 =============================//
/**
 * @param mediaId 发送的图片/语音/视频/图文消息（点击跳转到图文消息页）的媒体ID
 * */
@Serializable
class CustomerImgContent(
        @SerialName("media_id")
        val mediaId: String
)

/**
 * 客服图片消息
 * @param receiver 消息接收者open id
 * */
class CustomerImgMsg(
        val image: CustomerImgContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg {
    override val msgType: String = IMsg.IMAGE
}


//============================= 客服 之 图文消息 =============================//
@Serializable
class CustomerNewsContent(
        val articles: List<ReArticleItem>,
)

/**
 * 客服图文消息（点击跳转到外链）
 *
 *图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
class CustomerNewsMsg(
        val news: CustomerNewsContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg {
    override val msgType: String = ICustomerMsg.NEWS
    /**
     * convenience constructor
     * */
    constructor(openId: String, articleItem: ReArticleItem)
            : this(CustomerNewsContent(listOf(articleItem)), openId)
}


//============================= 客服 之 music消息 =============================//
/**
 * @param description	否	图文消息/视频消息/音乐消息的描述
 * @param musicUrl musicurl	是	音乐链接
 * @param hqMusicUrl hqmusicurl	是	高品质音乐链接，wifi环境优先使用该链接播放音乐
 * @param thumb 缩略图/小程序卡片图片的媒体ID，小程序卡片图片建议大小为520*416
 * */
@Serializable
class CustomerMusicContent(
        @SerialName("musicurl")
        val musicUrl: String,
        @SerialName("hqmusicurl")
        val hqMusicUrl: String,
        @SerialName("thumb_media_id")
        val thumb: String,
        val title: String? = null,
        val description: String? = null,
)

/**
 * 客服图文消息（点击跳转到外链）
 *
 *图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
@Serializable
class CustomerMusicMsg(
        val news: CustomerMusicContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg
{
    override val msgType: String = ICustomerMsg.MUSIC
}

//============================= 客服 之 菜单消息 =============================//
/**
 * 用户会看到这样的菜单消息：
 * “您对本次服务是否满意呢？
 * 满意
 * 不满意”
 * 其中，“满意”和“不满意”是可点击的，当用户点击后，微信会发送一条XML消息到开发者服务器
 * */
@Serializable
class CustomerMenuContent(
        @SerialName("head_content")
        val headContent: String,
        val list: List<IdContent>,
        @SerialName("tail_content")
        val tailContent: String
)
/**
 * 诸如 "id": "101", "content": "满意" 和 "id": "102","content": "不满意" 等
 * */
@Serializable
class IdContent(
        val id: String,
        val content: String
)
/**
 * 客服图文消息（点击跳转到外链）
 *
 *图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
@Serializable
class CustomerMenuMsg(
        @SerialName("msgmenu")
        val msgMenu: CustomerMenuContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg{
    override val msgType: String = ICustomerMsg.MENU
}


//============================= 客服 之 mini program消息 =============================//
/**
 * @param title	否	图文消息/视频消息/音乐消息/小程序卡片的标题
 * @param thumb 缩略图/小程序卡片图片的媒体ID，小程序卡片图片建议大小为520*416
 * */
@Serializable
class CustomerMiniProgramContent(
        val title: String? = null,
        @SerialName("appid")
        val appId: String,
        @SerialName("pagepath")
        val pagePath: String,
        @SerialName("thumb_media_id")
        val thumb: String?
)

/**
 * 客服图文消息（点击跳转到外链）
 *
 *图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
@Serializable
class CustomerMiniProgramMsg(
        @SerialName("miniprogrampage")
        val miniProgram: CustomerMiniProgramContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg{
    override val msgType: String = ICustomerMsg.MINI_PROGRAM
}