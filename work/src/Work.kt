/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:38
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

package com.github.rwsbillyang.wxSDK.work


import com.github.rwsbillyang.wxSDK.accessToken.*
import com.github.rwsbillyang.wxSDK.security.PemUtil
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.inMsg.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.security.PrivateKey
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates


/**
 * 微信系统基础应用的secret，可使api具备特殊权限，如通讯录，外部联系人，聊天会话存档
 * */
enum class SysAgentKey{
    Contact, ExternalContact, ChatArchive, WxKeFu
}


/**
 * 内部应用使用
 * */
object Work {
    /**
     * config后变为true，否则为false
     * */
    //var initial = false

    /**
     * 单个agent应用支持部署到多个企业微信中，由app在配置sdk参数时自动指定
     * */
    //var isMulti by Delegates.notNull<Boolean>()

    /**
     * 是否是isv模式，即第三方应用模式，在配置sdk参数时自动指定
     * */
    var isIsv by Delegates.notNull<Boolean>()




    const val prefix = "/api/wx/work"

    /**
     * 接收消息
     * */
    var msgNotifyUri = "${prefix}/msg"
    /**
     * 前端获取api签名信息，重定向到请求腾讯授权页面
     * */
    var oauthInfoPath: String = "$prefix/oauth/info"

    /**
     * 用户授权后的通知微信服务器通知到后端的路径
     * */
    var oauthNotifyPath: String = "$prefix/oauth/notify"

    /**
     * 一些前端SPA应用路径通常使用"#!"进行隔离，若为正常路径，可赋值为""
     * */
    var browserHistorySeparator = "#!"
    /**
     * 授权后腾讯通知到后端，处理后再跳转一下通知前端，前端记录下授权结果路径
     * */
    var oauthNotifyWebAppUrl: String = "/wxwork/authNotify"

    var jsSdkSignaturePath: String =  "$prefix/jssdk/signature"

    //var agentJsSdkSignaturePath: String =  "$prefix/jssdk/signature/agent"

}

/**
 * 一个运行的应用（通常是一个进程）中，存在多个corp的多个agent。
 * 亦即多个agent打包到一个jar中，然后运行在一个应用进程中
 * */
object WorkMulti{
    /**
     * corpId -> WorkApiContext
     * 大量的corp之后，此map可能很大
     * */
    val ApiContextMap = hashMapOf<String, CorpApiContext>()

    var defaultWorkEventHandler: IWorkEventHandler? = null
    var defaultWorkMsgHandler: IWorkMsgHandler? = null
    var eventHandlerCount: AtomicInteger = AtomicInteger()
    var msgHandlerCount: AtomicInteger = AtomicInteger()
    /**
     * 当更新配置后，重置
     * */
    fun reset(corpId: String) {
        ApiContextMap.remove(corpId)
    }

    fun reset(corpId: String, agentIdOrKey: String) {
        ApiContextMap[corpId]?.agentMap?.remove(agentIdOrKey)
    }

    fun config(corpId: String,
               agentIdOrKey: String,
               secret: String,
               token: String? = null,
               encodingAESKey: String? = null,
               privateKeyFilePath: String? = null,
               enableJsSdk: Boolean = false,
               enableMsg: Boolean = true, //是否激活：消息解析、分发、处理
               customMsgHandler: IWorkMsgHandler? = null,
               customEventHandler: IWorkEventHandler? = null,
               customAccessToken: ITimelyRefreshValue? = null
    ) {
        var corpApiCtx = ApiContextMap[corpId]
        if (corpApiCtx == null) {
            corpApiCtx = CorpApiContext(corpId)
            ApiContextMap[corpId] = corpApiCtx
        }

        corpApiCtx.agentMap[agentIdOrKey] = setupApiCtx(corpId, agentIdOrKey, secret,
            token, encodingAESKey, privateKeyFilePath, enableJsSdk,enableMsg,
            customMsgHandler,customEventHandler,customAccessToken)
    }




    fun setupApiCtx(corpId: String, agentIdOrKey: String,
              secret: String,
              token: String? = null,
              encodingAESKey: String? = null,
              privateKeyFilePath: String? = null,
              enableJsSdk: Boolean = false,
              enableMsg: Boolean = true, //是否激活：消息解析、分发、处理
              customMsgHandler: IWorkMsgHandler?,
              customEventHandler: IWorkEventHandler?,
              customAccessToken: ITimelyRefreshValue? = null):ApiCtx
    {
        val accessToken: ITimelyRefreshValue = customAccessToken ?: TimelyRefreshAccessToken(corpId,
            AccessTokenRefresher(accessTokenUrl(corpId, secret)), extra = agentIdOrKey)

        var msgHub: WorkMsgHub? = null
        var wXBizMsgCrypt: WXBizMsgCrypt? = null
        if (enableMsg) {
            if (!token.isNullOrBlank() && !encodingAESKey.isNullOrBlank()) {
                wXBizMsgCrypt = WXBizMsgCrypt(token, encodingAESKey)
                msgHub = WorkMsgHub(wXBizMsgCrypt, customMsgHandler, customEventHandler)
            } else {
                println("enableMsg=true, but not config token and encodingAESKey")
            }
        }
        var agentJsTicket:TimelyRefreshTicket? =  null
        var corpJsTicket: TimelyRefreshTicket? =  null
        if(enableJsSdk){
            val agentJsTicket = TimelyRefreshTicket(corpId,
                TicketRefresher{
                    "https://qyapi.weixin.qq.com/cgi-bin/ticket/get?access_token=${accessToken.get()}&type=agent_config"
                }, extra = agentIdOrKey)
            val corpJsTicket = TimelyRefreshTicket(corpId,
                TicketRefresher{
                    "https://qyapi.weixin.qq.com/cgi-bin/get_jsapi_ticket?access_token=${accessToken.get()}"
                })
        }
        var privateKey: PrivateKey? = null //会话存档里用到
        if (!privateKeyFilePath.isNullOrBlank()) {
            val file = File(privateKeyFilePath)
            if (file.exists()) {
                privateKey = PemUtil.loadPrivateKey(FileInputStream(privateKeyFilePath))
            } else {
                val log = LoggerFactory.getLogger("setupApiCtx")
                log.warn("Not exists: $privateKeyFilePath")
            }
        }

       return ApiCtx(secret, token, msgHub, wXBizMsgCrypt, accessToken, agentJsTicket, corpJsTicket, privateKey)
    }
}



/**
 * 调用API时可能需要用到的配置
 *
 * @param corpId       企业微信等申请的app id
 * @param agentMap secret map,  key refer to WorkBaseApi.AN_*
 * 登录微信公众平台官网后，在公众平台官网的开发-基本设置页面，勾选协议成为开发者，点击“修改配置”按钮，
 * 填写服务器地址（URL）、Token和EncodingAESKey，其中URL是开发者用来接收微信消息和事件的接口URL。
 * 。https://work.weixin.qq.com/api/doc/90000/90135/90930
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
 * */
class CorpApiContext(
        var corpId: String,
        /**
         * key -> value: agentId -> ApiCtx
         * */
        val agentMap: HashMap<String, ApiCtx> = hashMapOf()
)

class ApiCtx(
    val secret: String,
    val token: String? = null,
    val msgHub: WorkMsgHub? = null,
    val wxBizMsgCrypt: WXBizMsgCrypt? = null,
    val accessToken: ITimelyRefreshValue? = null,
    var agentJsTicket: ITimelyRefreshValue? = null,
    var corpJsTicket:ITimelyRefreshValue? = null,
    var privateKey: PrivateKey? = null
)

internal fun accessTokenUrl(corpId: String, secret: String) = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=$corpId&corpsecret=$secret"
//internal class AccessTokenUrl(private val corpId: String, private val secret: String) : IUrlProvider {
//    override fun url() = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=$corpId&corpsecret=$secret"
//}


/**
 * agent的配置
 *
 * @property token       Token可由开发者可以任意填写，用作生成签名（该Token会和接口URL中包含的Token进行比对，从而验证安全性）
 * @property encodingAESKey  安全模式需要需要  43个字符，EncodingAESKey由开发者手动填写或随机生成，将用作消息体加解密密钥。
 * @param msgHandler 自定义的处理微信推送过来的消息处理器，不提供的话则不处理消息，只发送"欢迎关注"，需要处理用户消息的话建议提供
 * @param eventHandler 自定义的处理微信推送过来的消息处理器，不提供的话则不处理事件，即什么都不处理，需要处理事件如用户关注和取消关注的话建议提供
 * //@property ticket 自定义的jsTicket刷新器，不提供的话使用默认的，通常情况下无需即可

 * @param privateKeyFilePath 调用会话存档时用的配置：私钥配置
 * encrypt_random_key内容解密说明：encrypt_random_key是使用企业在管理端填写的公钥（使用模值为2048bit的秘钥），
 * 采用RSA加密算法进行加密处理后base64 encode的内容，加密内容为企业微信产生。RSA使用PKCS1。
 * openssl genrsa -out app_private_key.pem 2048 # 私钥的生成
 * 利用私钥生成公钥：openssl rsa -in app_private_key.pem -pubout -out app_public_key.pem #导出公钥
 * 登录微信公众平台官网后，在公众平台官网的开发-基本设置页面，勾选协议成为开发者，点击“修改配置”按钮，
 * 填写服务器地址（URL）、Token和EncodingAESKey，其中URL是开发者用来接收微信消息和事件的接口URL。
 * 。https://work.weixin.qq.com/api/doc/90000/90135/90930
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
 * */
//class AgentContext(
//    corpId: String,
//    val agentId: Int,
//    val secret: String,
//    val enableMsg: Boolean = true, //是否激活：消息解析、分发、处理
//    val token: String? = null,
//    val encodingAESKey: String? = null,
//    privateKeyFilePath: String? = null,
//    enableJsSdk: Boolean,
//    msgHandler: IWorkMsgHandler?,
//    eventHandler: IWorkEventHandler?,
//    customAccessToken: ITimelyRefreshValue? = null,
//    var agentJsTicket: ITimelyRefreshValue? = null,
//    var corpJsTicket:ITimelyRefreshValue? = null
//) {
//    private val log = LoggerFactory.getLogger("AgentContext")
//
//    var accessToken: ITimelyRefreshValue = customAccessToken ?: TimelyRefreshAccessToken(corpId,
//            AccessTokenRefresher(accessTokenUrl(corpId, secret)), extra = agentId.toString())
//
//
//    var msgHub: WorkMsgHub? = null
//    var wxBizMsgCrypt: WXBizMsgCrypt? = null
//
//    var privateKey: PrivateKey? = null
//
//    init {
//        if (enableMsg) {
//            if (!token.isNullOrBlank() && !encodingAESKey.isNullOrBlank()) {
//                wxBizMsgCrypt = WXBizMsgCrypt(token, encodingAESKey)
//                msgHub = WorkMsgHub(msgHandler, eventHandler, wxBizMsgCrypt!!)
//            } else {
//                println("enableMsg=true, but not config token and encodingAESKey")
//            }
//        }
//        if(enableJsSdk){
//            agentJsTicket = TimelyRefreshTicket(corpId,
//                    TicketRefresher{
//                        "https://qyapi.weixin.qq.com/cgi-bin/ticket/get?access_token=${accessToken.get()}&type=agent_config"
//                    }, extra = agentId.toString())
//            corpJsTicket = TimelyRefreshTicket(corpId,
//                TicketRefresher{
//                    "https://qyapi.weixin.qq.com/cgi-bin/get_jsapi_ticket?access_token=${accessToken.get()}"
//                })
//        }
//
//        if (!privateKeyFilePath.isNullOrBlank()) {
//            val file = File(privateKeyFilePath)
//            if (file.exists()) {
//                privateKey = PemUtil.loadPrivateKey(FileInputStream(privateKeyFilePath))
//            } else {
//                log.warn("Not exists: $privateKeyFilePath")
//            }
//        }
//    }
//}



//https://qyapi.weixin.qq.com/cgi-bin/get_jsapi_ticket?access_token=ACCESS_TOKEN
//internal class TicketUrl(private val accessToken: ITimelyRefreshValue): IUrlProvider{
//    override fun url() = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=${accessToken.get()}&type=jsapi"
//
//}


/**
 * 一个进程中就存在一个特定的corp的agent
 * */
//object WorkSingle{
// //   private lateinit var ctx: CorpApiContext
//    private lateinit var _corpId: String
//    //private var _agentId by Delegates.notNull<Int>()
//   // private lateinit var _agentContext: AgentContext
//   val agentMap: HashMap<Int, AgentContext> = hashMapOf()
//
//    val corpId: String
//        get() = _corpId
//
//    //key为SysAccessToken中的KeyXxxx
//
//    val sysAccessTokenMap = hashMapOf<String, TimelyRefreshAccessToken>()
//    internal val sysSecretMap = hashMapOf<String, String>()
//
//    /**
//     * 配置高权限secret
//     *
//     * @param key 高权限accessToken对应的字符串key，可任意指定，同时需要将其赋值给对应的api的SysAgentKey字段
//     * 如通讯录应用 ContactsApi.SysAgentKey = ${key}
//     *
//     * sdk中已内置了几个默认字段：SysAgentKey.Contact, SysAgentKey.ExternalContact, SysAgentKey.ChatArchive
//     * 无需再给对应的api赋值
//     *
//     * @param secret 高权限api的secret
//     * */
//    fun config(key: String, secret: String){
//        sysSecretMap[key] = secret
//        sysAccessTokenMap[key] = TimelyRefreshAccessToken(corpId,
//            AccessTokenRefresher(accessTokenUrl(corpId, secret)), extra = key)
//    }
//    fun reset(key: String){
//        sysAccessTokenMap.remove(key)
//        sysSecretMap.remove(key)
//    }
//    fun reset(agentId: Int){
//        agentMap.remove(agentId)
//    }
//
//    fun config(corpId: String,
//               agentId: Int,
//               secret: String,
//               token: String? = null,
//               encodingAESKey: String? = null,
//               privateKeyFilePath: String? = null,
//               enableJsSdk: Boolean = false,
//               enableMsg: Boolean = true, //是否激活：消息解析、分发、处理
//               customMsgHandler: IWorkMsgHandler?,
//               customEventHandler: IWorkEventHandler?,
//               customAccessToken: ITimelyRefreshValue? = null
//    ) {
//        _corpId = corpId
//
//        agentMap[agentId] = AgentContext(corpId, agentId, secret, enableMsg,
//            token, encodingAESKey, privateKeyFilePath, enableJsSdk,
//            customMsgHandler,customEventHandler,customAccessToken)
//    }
//}

