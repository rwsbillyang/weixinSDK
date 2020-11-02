package com.github.rwsbillyang.wxSDK.officialAccount.outMsg

import com.github.rwsbillyang.wxSDK.msg.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



//============================= 客服发送出去的消息 =============================//
/**
 * @property customService 以某个客服帐号来发消息（在微信6.0.2及以上版本中显示自定义头像），则需在JSON数据包的后半部分加入customservice参数
 * */
interface ICustomerMsg : IMsg {
    val customService: KfAccountName?
}

@Serializable
class KfAccountName(
        @SerialName("kf_account")
        val name: String
)
//============================= 客服 之 图片消息 =============================//

/**
 * 客服图片消息
 * @param receiver 消息接收者open id
 * */
class CustomerImgMsg(
        val image: ImageContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg {
    override val msgType: String = IMsg.IMAGE
}



//============================= 客服 之 视频消息 =============================//
@Serializable
class CustomerVideoMsg(
        val video: VideoKfContent,
        val receivers: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg{
    override val msgType: String = InMsgType.VIDEO
}



//============================= 客服 之 图文消息 =============================//
/**
 * 客服图文消息（点击跳转到外链）
 *
 *图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
class CustomerNewsMsg(
        val news: NewsContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg {
    override val msgType: String = IMsg.NEWS
    /**
     * convenience constructor
     * */
    constructor(openId: String, articleItem: ArticleItem)
            : this(NewsContent(listOf(articleItem)), openId)
}


//============================= 客服 之 music消息 =============================//

/**
 * 客服图文消息（点击跳转到外链）
 *
 *图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
@Serializable
class CustomerMusicMsg(
        val news: MusicContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg
{
    override val msgType: String = IMsg.MUSIC
}



//============================= 客服 之 菜单消息 =============================//
/**
 * 客服图文消息（点击跳转到外链）
 *
 *图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
@Serializable
class CustomerMenuMsg(
        @SerialName("msgmenu")
        val msgMenu: MenuContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg{
    override val msgType: String = IMsg.MENU
}


//============================= 客服 之 mini program消息 =============================//
/**
 * 客服图文消息（点击跳转到外链）
 *
 *图文消息条数限制在1条以内，注意，如果图文数超过1，则将会返回错误码45008。
 * */
@Serializable
class CustomerMiniProgramMsg(
        @SerialName("miniprogrampage")
        val miniProgram: MiniProgramContent,
        @SerialName("touser")
        val receiver: String,
        @SerialName("customservice")
        override val customService: KfAccountName? = null
) : ICustomerMsg{
    override val msgType: String = IMsg.MINI_PROGRAM
}