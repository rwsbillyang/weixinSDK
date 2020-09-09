package com.github.rwsbillyang.wxSDK.officialAccount.outMsg

import com.github.rwsbillyang.wxSDK.common.msg.ReArticleItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//============================= 客服发送出去的消息 =============================//

interface ICustomerMsg : IMsg {
    companion object {
        const val MUSIC = "music"
        const val MENU = "msgmenu"
        const val NEWS = "news"
    }
}

//============================= 客服 之 图片消息 =============================//

@Serializable
class CustomerImgContent(
        @SerialName("media_id")
        val mediaId: String
)

/**
 * 客服图片消息
 * */
class CustomerImgMsg(
        val image: CustomerImgContent,
        @SerialName("touser")
        val receiver: String,
        override val msgType: String = IMsg.IMAGE
) : ICustomerMsg


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
        override val msgType: String = ICustomerMsg.NEWS
) : ICustomerMsg {
    /**
     * convenience constructor
     * */
    constructor(openId: String, articleItem: ReArticleItem)
            : this(CustomerNewsContent(listOf(articleItem)), openId)
}


//============================= 客服 之 music消息 =============================//
@Serializable
class CustomerMusicContent(
        val title: String,
        val description: String,
        @SerialName("musicurl")
        val musicUrl: String? = null,
        @SerialName("hqmusicurl")
        val hqMusicUrl: String? = null,
        @SerialName("thumb_media_id")
        val thumb: String? = null
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
        override val msgType: String = ICustomerMsg.MUSIC
) : ICustomerMsg

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
        override val msgType: String = ICustomerMsg.MENU
) : ICustomerMsg