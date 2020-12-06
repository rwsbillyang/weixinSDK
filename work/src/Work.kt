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
    private  var _WORK: WorkContext? = null
    val WORK: WorkContext
    get() {
        requireNotNull(_WORK)
        return _WORK!!
    }
    fun isInit() = _WORK != null

    /**
     * 前端获取api签名信息，重定向到请求腾讯授权页面
     * */
    var oauthInfoPath: String = "/api/wx/work/oauth/info"
    /**
     * 用户授权后的通知路径
     * */
    var notifyPath: String = "/api/wx/work/oauth/notify"
    /**
     * 授权后通知前端的授权结果路径
     * */
    var notifyWebAppUrl: String = "/wx/work/authNotify"

    /**
     * 非ktor平台可以使用此函数进行配置企业微信参数
     * corpid信息在企业微信管理端—我的企业—企业信息查看，
     *
     * 企业微信会对ip地址的访问进行限制。对于使用方而言，需要设置其公网ip，
     * 企业微信后台收到的请求，会校验调用方的ip与管理端填写的ip是否匹配。
     * */
    fun config(block: WorkConfiguration.() -> Unit) {
        val config = WorkConfiguration().apply(block)
        _WORK = WorkContext(config.corpId, config.agentMap)
    }

    /**
     * 当更新配置后，先重置，然后再调用上面的configXXX函数重新配置
     * */
    fun reset(){
        _WORK = null
    }
}


/**
 * 调用API时可能需要用到的配置
 *
 * @property corpId       企业微信等申请的app id
 * //@property accessToken 自定义的accessToken刷新器，不提供的话使用默认的，通常情况下无需即可
 * //@property ticket 自定义的jsTicket刷新器，不提供的话使用默认的，通常情况下无需即可

 * 登录微信公众平台官网后，在公众平台官网的开发-基本设置页面，勾选协议成为开发者，点击“修改配置”按钮，
 * 填写服务器地址（URL）、Token和EncodingAESKey，其中URL是开发者用来接收微信消息和事件的接口URL。
 * 。https://work.weixin.qq.com/api/doc/90000/90135/90930
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
 * */
open class WorkConfiguration {
    var corpId = "your_app_id"

    //var ticket: IRefreshableValue? = null
    /**
     * @param agentMgtName WorkBaseApi.AN_*
     * */
    fun add(agentId: Int?,
            agentMgtName: String,
            secret: String,

            enableMsg: Boolean = false,
            token: String? = null,
            encodingAESKey: String? = null,

            customAccessToken: ITimelyRefreshValue? = null,
            customCallbackPath: String? = null,
            privateKeyFilePath: String? = null,
            ) {
        agentMap[agentId?.let { it.toString() }?:agentMgtName] = WorkAgentContext(corpId, agentMgtName, agentId,secret,enableMsg,
                token, encodingAESKey,customAccessToken, customCallbackPath, privateKeyFilePath)
    }
    internal val agentMap = HashMap<String, WorkAgentContext>()
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
class WorkContext(
        var corpId: String,
        val agentMap: HashMap<String, WorkAgentContext>
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
class WorkAgentContext(
        corpId: String,
        val mgtName: String,
        val agentId: Int?,
        val secret: String,
        val enableMsg: Boolean = false,
        val token: String? = null,
        val encodingAESKey: String? = null,
        customAccessToken: ITimelyRefreshValue? = null,
        customCallbackPath: String? = null,
        privateKeyFilePath: String? = null
){
    val log = LoggerFactory.getLogger("workApi")

    var accessToken: ITimelyRefreshValue = customAccessToken?:TimelyRefreshAccessToken(corpId,
            AccessTokenRefresher(AccessTokenUrl(corpId, secret)),extra = agentId?.toString())

    var callbackPath: String = if(customCallbackPath.isNullOrBlank())
    {
        if(agentId != null){
            "/api/wx/work/${corpId}/${agentId}"
        }else{
            "/api/wx/work/${corpId}/${mgtName}"
        }
    }else
        customCallbackPath

    var msgHandler: IWorkMsgHandler? = null
    var eventHandler: IWorkEventHandler? = null
    var msgHub: WorkMsgHub? = null
    var wxBizMsgCrypt: WXBizMsgCrypt? = null

    var privateKey: PrivateKey? = null

    init {
        if(enableMsg){
            if(!token.isNullOrBlank() && !encodingAESKey.isNullOrBlank())
            {
                wxBizMsgCrypt = WXBizMsgCrypt(token, encodingAESKey, corpId)

                if(msgHandler == null) msgHandler = DefaultWorkMsgHandler()
                if(eventHandler == null) eventHandler = DefaultWorkEventHandler()

                msgHub = WorkMsgHub(msgHandler!!, eventHandler!!, wxBizMsgCrypt!!)
            }else{
                println("enableMsg=true, but not config token and encodingAESKey")
            }
        }

        if(!privateKeyFilePath.isNullOrBlank()){
            val file = File(privateKeyFilePath)
            if(file.exists()){
                privateKey = PemUtil.loadPrivateKey(FileInputStream(privateKeyFilePath))
            }else{
                log.warn("Not exists: $privateKeyFilePath")
            }
        }
    }
}





internal class AccessTokenUrl(private val corpId: String, private val secret: String) : IUrlProvider {
    override fun url() = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=$corpId&corpsecret=$secret"
}

//internal class TicketUrl(private val accessToken: IRefreshableValue): IUrlProvider{
//    override fun url() = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=${accessToken.get()}&type=jsapi"
//
//}

