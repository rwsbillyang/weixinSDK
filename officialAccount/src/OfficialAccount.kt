/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:37
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.wxSDK.officialAccount


import com.github.rwsbillyang.wxSDK.accessToken.*
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.*



object OfficialAccount {
    /**
     * 微信消息接入点"/api/wx/oa/app/{appId}"
     * */
    var msgUri = "/api/wx/oa/app"
    /**
     * 前端获取api签名信息，重定向到请求腾讯授权页面
     * */
    var oauthInfoPath: String = "/api/wx/oa/oauth/info"
    /**
     * 微信服务器通知到后端的路径（无需用户授权）, 最后面的参数为{appId}，用于通知openId
     * */
    var oauthNotifyPath1: String = "/api/wx/oa/oauth/notify1"
    /**
     * 获取用户信息用户授权后，微信服务器通知到后端的路径，最后面的参数为{appId}
     * */
    var oauthNotifyPath2: String = "/api/wx/oa/oauth/notify2"

    /**
     * 一些前端SPA应用路径通常使用"#!"进行隔离，若为正常路径，可赋值为""
     * */
    var browserHistorySeparator = "#!"

    /**
     * 授权后经过后端再跳转到前端，前端记录下授权结果路径
     * */
    var oauthNotifyWebAppUrl: String = "/wxoa/authNotify"


    var jsSdkSignaturePath: String =  "/api/wx/oa/jssdk/signature"

    /**
     * 配置公众号参数
     * */
    fun config(block: OAConfiguration.() -> Unit) {
        val config = OAConfiguration().apply(block)
        val old = ApiContextMap[config.appId]
        if(old == null){
            ApiContextMap[config.appId] = ApiContext(
                config.appId,
                config.secret,
                config.token,
                config.encodingAESKey,
                config.wechatId,
                config.wechatName,
                config.msgHandler,
                config.eventHandler,
                config.accessToken,
                config.ticket
            )
        }else{
            old.onChange(config)
        }
    }

    val ApiContextMap = hashMapOf<String, ApiContext>()

}
/**
 * 调用API时可能需要用到的配置
 *
 * @property appId       公众号或企业微信等申请的app id
 * @property secret      对应的secret，用于换取accessToken包括网页授权登录拉取的accessToken，其它情况下无需使用
 * @property token       Token可由开发者可以任意填写，用作生成签名（该Token会和接口URL中包含的Token进行比对，从而验证安全性）
 * @property encodingAESKey  安全模式需要需要  43个字符，EncodingAESKey由开发者手动填写或随机生成，将用作消息体加解密密钥。
 * @property wechatId 比如公众号的微信号，客服系统中需要设置
 * @property wechatName 公众号名称，暂可不设置
 * @property msgHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，只发送"欢迎关注"，需要处理用户消息的话建议提供
 * @property eventHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，即什么都不处理，需要处理事件如用户关注和取消关注的话建议提供
 * @property accessToken 自定义的accessToken刷新器，不提供的话使用默认的，通常情况下无需即可
 * @property ticket 自定义的jsTicket刷新器，不提供的话使用默认的，通常情况下无需即可

 *
 * 登录微信公众平台官网后，在公众平台官网的开发-基本设置页面，勾选协议成为开发者，点击“修改配置”按钮，
 * 填写服务器地址（URL）、Token和EncodingAESKey，其中URL是开发者用来接收微信消息和事件的接口URL。
 * 。https://work.weixin.qq.com/api/doc/90000/90135/90930
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
 * */
class OAConfiguration {
    var appId : String = "your_app_id"
    var secret: String = "your_app_secret_key"
    var token: String = "your_token"
    var encodingAESKey: String? = null
    var wechatId: String? = null
    var wechatName: String? = null

    var msgHandler: IOAMsgHandler?  = null
    var eventHandler: IOAEventHandler? = null
    var accessToken: ITimelyRefreshValue? = null
    var ticket: ITimelyRefreshValue? = null
}

/**
 * 调用API时可能需要用到的配置
 *
 * @param appId       公众号或企业微信等申请的app id
 * @param secret      对应的secret
 * @param token       Token可由开发者可以任意填写，用作生成签名（该Token会和接口URL中包含的Token进行比对，从而验证安全性）
 * @param encodingAESKey  安全模式需要需要  43个字符，EncodingAESKey由开发者手动填写或随机生成，将用作消息体加解密密钥。
 * @param wechatId 比如公众号的微信号，客服系统中需要设置
 * @param wechatName 公众号名称，暂可不设置
 * @param customMsgHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，只发送"欢迎关注"，需要处理用户消息的话建议提供
 * @param customEventHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，即什么都不处理，需要处理事件如用户关注和取消关注的话建议提供
 * @param customAccessToken 自定义的accessToken刷新器，不提供的话使用默认的，通常情况下无需即可
 * @param customTicket 自定义的jsTicket刷新器，不提供的话使用默认的，通常情况下无需即可

 *
 * 登录微信公众平台官网后，在公众平台官网的开发-基本设置页面，勾选协议成为开发者，点击“修改配置”按钮，
 * 填写服务器地址（URL）、Token和EncodingAESKey，其中URL是开发者用来接收微信消息和事件的接口URL。
 * 。https://work.weixin.qq.com/api/doc/90000/90135/90930
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
 * */
class ApiContext(
        var appId: String,
        var secret: String,
        var token: String,
        var encodingAESKey: String? = null,
        var wechatId: String? = null,
        var wechatName: String? = null,
        customMsgHandler: IOAMsgHandler?,
        customEventHandler: IOAEventHandler?,
        customAccessToken: ITimelyRefreshValue?,
        customTicket: ITimelyRefreshValue?
) {
    var accessToken: ITimelyRefreshValue
    var ticket: ITimelyRefreshValue
    var wxBizMsgCrypt = encodingAESKey?.let { WXBizMsgCrypt(token, it) }
    var msgHub: OAMsgHub

    init {
        msgHub = OAMsgHub(customMsgHandler, customEventHandler, wxBizMsgCrypt)

        accessToken = customAccessToken
                ?: TimelyRefreshAccessToken(appId, AccessTokenRefresher(accessTokenUrl(appId, secret)))

        ticket = customTicket ?: TimelyRefreshTicket(appId, TicketRefresher{
            "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=${accessToken.get()}&type=jsapi"
        })
    }
    fun onChange(config: OAConfiguration){
        var dirty1 = false
        var dirty2 = false
        if(config.appId != appId){
            appId = config.appId
            dirty1 = true
        }
        if(config.secret != secret){
            secret = config.secret
            dirty2 = true
        }
        if(config.token != token){
            token = config.token
            dirty1 = true
        }
        if(config.encodingAESKey != encodingAESKey){
            encodingAESKey = config.encodingAESKey
            dirty1 = true
        }
        if(config.wechatId != wechatId){
            wechatId = config.wechatId
        }
        if(config.wechatName != wechatName){
            wechatName = config.wechatName
        }

        var dirty3 = false
        val msgHandler = if(config.msgHandler != null){
            dirty3 = true
            config.msgHandler!!
        }else msgHub.msgHandler

        val eventHandler = if(config.eventHandler != null){
            dirty3 = true
            config.eventHandler!!
        }else msgHub.eventHandler

        if(dirty1) {
            wxBizMsgCrypt = encodingAESKey?.let { WXBizMsgCrypt(token, it) }
            dirty3 = true
        }
        if(dirty3){
            msgHub = OAMsgHub(msgHandler, eventHandler, wxBizMsgCrypt)
        }

       if(config.accessToken != null)
           accessToken = config.accessToken!!
        else if(dirty1 || dirty2){
           accessToken = TimelyRefreshAccessToken(appId, AccessTokenRefresher(accessTokenUrl(appId, secret)))
       }

       if(config.ticket != null)
           ticket = config.ticket!!
        else if(dirty1 || dirty2){
           ticket = TimelyRefreshTicket(appId, TicketRefresher{
               "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=${accessToken.get()}&type=jsapi"
           })
       }

    }
}


internal fun accessTokenUrl(appId: String, secret: String) = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=$appId&secret=$secret"
//internal class AccessTokenUrl(private val appId: String, private val secret: String) : IUrlProvider {
//    override fun url() = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=$appId&secret=$secret"
//}

//internal fun ticketUrl(accessToken: ITimelyRefreshValue) = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=${accessToken.get()}&type=jsapi"
//internal class TicketUrl(private val accessToken: ITimelyRefreshValue) : IUrlProvider {
//    override fun url() = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=${accessToken.get()}&type=jsapi"
//}

