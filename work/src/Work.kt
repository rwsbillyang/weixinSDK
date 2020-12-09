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


object Work {
    /**
     * key -> value: corpId -> WorkApiContext
     * TODO: 大量的corp之后，此map可能很大
     * */
    val ApiContextMap = hashMapOf<String, CorpApiContext>()

    /**
     * 前端获取api签名信息，重定向到请求腾讯授权页面
     * */
    var oauthInfoPath: String = "/api/wx/work/oauth/info"

    /**
     * 用户授权后的通知路径
     * */
    var oauthNotifyPath: String = "/api/wx/work/oauth/notify"

    /**
     * 授权后通知前端的授权结果路径
     * */
    var oauthNotifyWebAppUrl: String = "/wxwork/authNotify"


    /**
     * 当更新配置后，重置
     * */
    fun reset(corpId: String) {
        ApiContextMap.remove(corpId)
    }

    fun reset(corpId: String, agentId: Int) {
        ApiContextMap[corpId]?.agentMap?.remove(agentId)
    }

    fun config(corpId: String,
               agentId: Int,
               secret: String,

               enableMsg: Boolean = false,
               token: String? = null,
               encodingAESKey: String? = null,

               privateKeyFilePath: String? = null,
               customMsgUrl: String? = null,
               customAccessToken: ITimelyRefreshValue? = null

    ) {
        var corpApiCtx = ApiContextMap[corpId]
        if (corpApiCtx == null) {
            corpApiCtx = CorpApiContext(corpId)
            ApiContextMap[corpId] = corpApiCtx
        }

        corpApiCtx.agentMap[agentId] = AgentContext(corpId, agentId, secret, enableMsg,
                token, encodingAESKey, customAccessToken, customMsgUrl, privateKeyFilePath)
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
         * key -> value: agentId -> WorkAgentContext
         * */
        val agentMap: HashMap<Int, AgentContext> = hashMapOf()
)

/**
 * agent的配置
 *
 * @property token       Token可由开发者可以任意填写，用作生成签名（该Token会和接口URL中包含的Token进行比对，从而验证安全性）
 * @property encodingAESKey  安全模式需要需要  43个字符，EncodingAESKey由开发者手动填写或随机生成，将用作消息体加解密密钥。
 * @property msgHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，只发送"欢迎关注"，需要处理用户消息的话建议提供
 * @property eventHandler 自定义的处理微信推送过来的消息处理器，不提供的话使用默认的，即什么都不处理，需要处理事件如用户关注和取消关注的话建议提供
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
class AgentContext(
        corpId: String,
        val agentId: Int,
        val secret: String,
        val enableMsg: Boolean = false,
        val token: String? = null,
        val encodingAESKey: String? = null,
        customAccessToken: ITimelyRefreshValue? = null,
        customMsgNotifyUri: String? = null,
        privateKeyFilePath: String? = null
) {
    private val log = LoggerFactory.getLogger("workApi")

    var accessToken: ITimelyRefreshValue = customAccessToken ?: TimelyRefreshAccessToken(corpId,
            AccessTokenRefresher(accessTokenUrl(corpId, secret)), extra = agentId?.toString())

    /**
     * 微信消息接入， 微信消息通知URI
     * */
    var msgNotifyUri: String = if (customMsgNotifyUri.isNullOrBlank()) {
        "/api/wx/work/${corpId}/${agentId}"
    } else
        customMsgNotifyUri

    var msgHandler: IWorkMsgHandler? = null
    var eventHandler: IWorkEventHandler? = null
    var msgHub: WorkMsgHub? = null
    var wxBizMsgCrypt: WXBizMsgCrypt? = null

    var privateKey: PrivateKey? = null

    init {
        if (enableMsg) {
            if (!token.isNullOrBlank() && !encodingAESKey.isNullOrBlank()) {
                wxBizMsgCrypt = WXBizMsgCrypt(token, encodingAESKey, corpId)

                if (msgHandler == null) msgHandler = DefaultWorkMsgHandler()
                if (eventHandler == null) eventHandler = DefaultWorkEventHandler()

                msgHub = WorkMsgHub(msgHandler!!, eventHandler!!, wxBizMsgCrypt!!)
            } else {
                println("enableMsg=true, but not config token and encodingAESKey")
            }
        }

        if (!privateKeyFilePath.isNullOrBlank()) {
            val file = File(privateKeyFilePath)
            if (file.exists()) {
                privateKey = PemUtil.loadPrivateKey(FileInputStream(privateKeyFilePath))
            } else {
                log.warn("Not exists: $privateKeyFilePath")
            }
        }
    }
}


internal fun accessTokenUrl(corpId: String, secret: String) = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=$corpId&corpsecret=$secret"
//internal class AccessTokenUrl(private val corpId: String, private val secret: String) : IUrlProvider {
//    override fun url() = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=$corpId&corpsecret=$secret"
//}

//internal class TicketUrl(private val accessToken: IRefreshableValue): IUrlProvider{
//    override fun url() = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=${accessToken.get()}&type=jsapi"
//
//}

