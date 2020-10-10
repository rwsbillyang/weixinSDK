package com.github.rwsbillyang.wxSDK.work


import com.github.rwsbillyang.wxSDK.common.accessToken.*
import com.github.rwsbillyang.wxSDK.common.aes.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.msg.*


lateinit var _WORK: WorkContext

/**
 * 调用API时可能需要用到的配置
 *
 * @property corpId       企业微信等申请的app id
 * @property secret      对应的secret
 * @property token       Token可由开发者可以任意填写，用作生成签名（该Token会和接口URL中包含的Token进行比对，从而验证安全性）
 * @property encodingAESKey  安全模式需要需要  43个字符，EncodingAESKey由开发者手动填写或随机生成，将用作消息体加解密密钥。
 * @property wechatId 比如公众号的微信号，客服系统中需要设置
 * @property wechatName 公众号名称，暂可不设置
 * @property msgHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，只发送"欢迎关注"，需要处理用户消息的话建议提供
 * @property eventHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，即什么都不处理，需要处理事件如用户关注和取消关注的话建议提供
 * @property accessToken 自定义的accessToken刷新器，不提供的话使用默认的，通常情况下无需即可
 * //@property ticket 自定义的jsTicket刷新器，不提供的话使用默认的，通常情况下无需即可

 *
 * 登录微信公众平台官网后，在公众平台官网的开发-基本设置页面，勾选协议成为开发者，点击“修改配置”按钮，
 * 填写服务器地址（URL）、Token和EncodingAESKey，其中URL是开发者用来接收微信消息和事件的接口URL。
 * 。https://work.weixin.qq.com/api/doc/90000/90135/90930
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
 * */
class WorkConfiguration {
    var corpId = "your_app_id"
    var secret = "your_app_secret_key"
    var token = "your_token"

    var encodingAESKey = "your_encodingAESKey"

    var wechatId: String? = null
    var wechatName: String? = null

    var msgHandler: IWorkMsgHandler? = null
    var eventHandler: IWorkEventHandler? = null

    var accessToken: ITimelyRefreshValue? = null
    //var ticket: IRefreshableValue? = null

    var callbackPath = "/api/wx/work"
}

/**
 * 调用API时可能需要用到的配置
 *
 * @param corpId       企业微信等申请的app id
 * @param secret      对应的secret 用于换取accessToken，其它情况下无需使用
 * @param token       Token可由开发者可以任意填写，用作生成签名（该Token会和接口URL中包含的Token进行比对，从而验证安全性）
 * @param encodingAESKey  43个字符，EncodingAESKey由开发者手动填写或随机生成，将用作消息体加解密密钥。
 * @param wechatId 比如公众号的微信号，客服系统中需要设置
 * @param wechatName 公众号名称，暂可不设置
 * @param customMsgHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，只发送"欢迎关注"，需要处理用户消息的话建议提供
 * @param customEventHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，即什么都不处理，需要处理事件如用户关注和取消关注的话建议提供
 * @param customAccessToken 自定义的accessToken刷新器，不提供的话使用默认的，通常情况下无需即可
 * //@param customTicket 自定义的jsTicket刷新器，不提供的话使用默认的，通常情况下无需即可

 *
 * 登录微信公众平台官网后，在公众平台官网的开发-基本设置页面，勾选协议成为开发者，点击“修改配置”按钮，
 * 填写服务器地址（URL）、Token和EncodingAESKey，其中URL是开发者用来接收微信消息和事件的接口URL。
 * 。https://work.weixin.qq.com/api/doc/90000/90135/90930
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
 * */
class WorkContext(
        var corpId: String,
        var secret: String,
        var token: String,
        var encodingAESKey: String,
        var wechatId: String? = null,
        var wechatName: String? = null,
        var callbackPath: String = "/api/wx/work",
        customMsgHandler: IWorkMsgHandler?,
        customEventHandler: IWorkEventHandler?,
        customAccessToken: ITimelyRefreshValue?
        //customTicket: ITimelyRefreshValue?
) {
    var accessToken: ITimelyRefreshValue

    //var ticket: IRefreshableValue
    var wxBizMsgCrypt = WXBizMsgCrypt(token, encodingAESKey, corpId)
    var msgHub: WorkMsgHub


    init {
        val msgHandler = customMsgHandler ?: DefaultWorkMsgHandler()
        val eventHandler = customEventHandler ?: DefaultWorkEventHandler()
        msgHub = WorkMsgHub(msgHandler, eventHandler, wxBizMsgCrypt)

        accessToken = customAccessToken
                ?: TimelyRefreshAccessToken(corpId, AccessTokenRefresher(AccessTokenUrl(corpId, secret)))

        //ticket = customTicket?: RefreshableTicket(appId, TicketRefresher(TicketUrl(accessToken)))
    }
}

/**
 * 非ktor平台可以使用此函数进行配置企业微信参数
 * */
fun configWork(block: WorkConfiguration.() -> Unit) {
    val config = WorkConfiguration().apply(block)
    _WORK = WorkContext(
            config.corpId,
            config.secret,
            config.token,
            config.encodingAESKey,
            config.wechatId,
            config.wechatName,
            config.callbackPath,
            config.msgHandler,
            config.eventHandler,
            config.accessToken
            //config.ticket
    )
}

internal class AccessTokenUrl(private val corpId: String, private val secret: String) : IUrlProvider {
    override fun url() = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=$corpId&corpsecret=$secret"
}

//internal class TicketUrl(private val accessToken: IRefreshableValue): IUrlProvider{
//    override fun url() = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=${accessToken.get()}&type=jsapi"
//
//}

