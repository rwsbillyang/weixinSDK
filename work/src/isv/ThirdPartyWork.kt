/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-03-07 20:35
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

package com.github.rwsbillyang.wxSDK.work.isv


import com.github.rwsbillyang.wxSDK.accessToken.*
import com.github.rwsbillyang.wxSDK.security.PemUtil
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkMsgHandler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.security.PrivateKey


/**./gr
 * 每个第三方应用有一个suiteId
 * */
object IsvWork {

    private const val prefix = "/api/wx/work/isv"

    const val msgNotifyPath: String = "$prefix/msg/{suiteId}"

    const val jsSdkSignaturePath: String =  "$prefix/jssdk/signature"

    /**
     * 前端请求该路径，发起应用授权，用于从外部网站发起
     * */
    const val authFromOutsidePath = "$prefix/auth/outside/{suiteId}"

    /**
     * 后端路径：应用由客户授权后腾讯通知到后端临时授权码，处理后再跳转一下通知前端
     * */
    const val authCodeNotifyPath: String = "$prefix/auth/notify/{suiteId}"

    /**
     * 前端路径：获取永久授权信息成功通知前端的路径，默认: /wxwork/isv/authNotify
     */
    var permanentWebNotifyPath: String = "/wxwork/isv/authNotify"

}


object IsvWorkSingle {
    private val log: Logger = LoggerFactory.getLogger("IsvWorkSingle")

    private lateinit var _suiteId: String
    private lateinit var _ctx: SuiteApiContext
    val suiteId: String
        get() = _suiteId
    val ctx: SuiteApiContext
        get() = _ctx

    fun config(
        suiteId: String, secret: String, token: String, encodingAESKey: String?,
        enableJsSdk: Boolean, privateKeyFilePath: String?,
        suiteInfoHandler: ISuiteInfoHandler, msgHandler: IWorkMsgHandler, eventHandler: IWorkEventHandler
    ) {
        Work._isIsv = true
        Work._isMulti = false

        _suiteId = suiteId
        _ctx = SuiteApiContext(
            suiteId,
            secret,
            token,
            encodingAESKey,
            enableJsSdk,
            privateKeyFilePath,
            msgHandler,
            eventHandler,
            suiteInfoHandler
        )
    }

    /**
     * 配置IsvWork之step2：收到suite ticket时配置suiteAccessToken
     * suite_ticket由企业微信后台定时推送给“指令回调URL”，每十分钟更新一次,suite_ticket实际有效期为30分钟
     *
     * 调用情景：每次收到suite ticket推送消息时
     * */
    fun configSuiteToken(suiteId: String, ticket: String, corpListBlock: () -> List<Pair<String, String>>) {
        val ctx = IsvWorkSingle.ctx

        ctx.ticket = ticket
        if (ctx.suiteAccessToken == null) {//第一次推送ticket时为空，需要创建suiteAccessToken，以后无需再创建
            ctx.suiteAccessToken = TimelyRefreshSuiteToken(suiteId, SuiteRefresher(suiteId))

            //查询是否已有授权码，有的话，直接配置accessToken，否则等收到授权通知后再配置accessToken
            corpListBlock().forEach {
                configAccessToken(suiteId, it.first, it.second)
            }
        }
    }

    /**
     * 配置ThirdPartyWork之step3：配置suietId+corpId对应的accessToken
     * 调用情景：1. 收到授权成功通知后获取到永久授权码时；2. 从数据库中读取到永久授权码时
     * 收到授权成功通知后获取到永久授权码时(或从数据库读取待)，配置accessToken
     * @param initialAccessToken 获取永久授权码时，已返回有accessToken，可作为初始有效值
     * */
    fun configAccessToken(suiteId: String, corpId: String, permanentCode: String, initialAccessToken: String? = null) {
        val ctx = IsvWorkSingle.ctx

        val accessToken = ctx.accessTokenMap[corpId]
        if (accessToken == null) {
            ctx.accessTokenMap[corpId] = TimelyRefreshAccessToken3rd(suiteId, corpId, permanentCode)
            if (initialAccessToken != null) {
                ctx.accessTokenMap[corpId]!!.updateTokenInfo(initialAccessToken, System.currentTimeMillis())
            }

            if(ctx.enableJsSdk){
                ctx.jsTicket = TimelyRefreshTicket(suiteId,
                    TicketRefresher{
                        "https://qyapi.weixin.qq.com/cgi-bin/get_jsapi_ticket?access_token=${ctx.accessTokenMap[corpId]!!.get()}"
                    })
            }
        } else
            log.warn("not need config accessToken again")
    }

    /**
     * 取消授权后，将accessToken从map中移除
     * */
    fun removeCorpAuth(corpId: String) {
        ctx.accessTokenMap.remove(corpId)
    }
}

object IsvWorkMulti{
    private val log: Logger = LoggerFactory.getLogger("IsvWorkMulti")

    /**
     * suiteId -> SuiteApiContext
     * 一个服务商，可以有多个应用，这里的map是多个应用
     * */
    val ApiContextMap = hashMapOf<String, SuiteApiContext>()


    /**
     * 配置ThirdPartyWork之step1：系统启动时用suiteId和secret等进行配置
     * 配置map：配置suiteId和secret等，ticket等待腾讯系统推送，然后创建suiteAccessToken
     * accessToken的创建需要等到应用被授权后创建，接着是jsTicket
     * */
    fun config(suiteId: String, secret: String, token: String, encodingAESKey: String?,
               enableJsSdk: Boolean, privateKeyFilePath: String?,
               suiteInfoHandler: ISuiteInfoHandler, msgHandler: IWorkMsgHandler, eventHandler: IWorkEventHandler) {
        Work._isIsv = true
        Work._isMulti = true

        var ctx = ApiContextMap[suiteId]
        if (ctx == null) {//first time
            ctx = SuiteApiContext(suiteId, secret, token, encodingAESKey, enableJsSdk,privateKeyFilePath, msgHandler, eventHandler, suiteInfoHandler)
            ApiContextMap[suiteId] = ctx
        }
    }

    /**
     * 配置ThirdPartyWork之step2：收到suite ticket时配置suiteAccessToken
     * suite_ticket由企业微信后台定时推送给“指令回调URL”，每十分钟更新一次,suite_ticket实际有效期为30分钟
     *
     * 调用情景：每次收到suite ticket推送消息时
     * */
    fun configSuiteToken(suiteId: String, ticket: String, corpListBlock: () -> List<Pair<String, String>>) {
        val ctx = ApiContextMap[suiteId]
        if (ctx == null) {//必须先进行suite的配置，指定好suiteId和secret
            log.warn("please config suite firstly")
            return
        }

        ctx.ticket = ticket
        if (ctx.suiteAccessToken == null) {//第一次推送ticket时为空，需要创建suiteAccessToken，以后无需再创建
            ctx.suiteAccessToken = TimelyRefreshSuiteToken(suiteId, SuiteRefresher(suiteId))

            //查询是否已有授权码，有的话，直接配置accessToken，否则等收到授权通知后再配置accessToken
            corpListBlock().forEach {
                configAccessToken(suiteId, it.first, it.second)
            }
        }
    }

    /**
     * 配置ThirdPartyWork之step3：配置suietId+corpId对应的accessToken
     * 调用情景：1. 收到授权成功通知后获取到永久授权码时；2. 从数据库中读取到永久授权码时
     * 收到授权成功通知后获取到永久授权码时(或从数据库读取待)，配置accessToken
     * @param initialAccessToken 获取永久授权码时，已返回有accessToken，可作为初始有效值
     * */
    fun configAccessToken(suiteId: String, corpId: String, permanentCode: String, initialAccessToken: String? = null) {
        val ctx = ApiContextMap[suiteId]
        if (ctx == null) {//必须先进行suite的配置，指定好suiteId和secret
            log.warn("please config suite firstly")
            return
        }

        val accessToken = ctx.accessTokenMap[corpId]
        if (accessToken == null) {
            ctx.accessTokenMap[corpId] = TimelyRefreshAccessToken3rd(suiteId, corpId, permanentCode)
            if (initialAccessToken != null) {
                ctx.accessTokenMap[corpId]!!.updateTokenInfo(initialAccessToken, System.currentTimeMillis())
            }

            if(ctx.enableJsSdk){
                ctx.jsTicket = TimelyRefreshTicket(suiteId,
                    TicketRefresher{
                        "https://qyapi.weixin.qq.com/cgi-bin/get_jsapi_ticket?access_token=${ctx.accessTokenMap[corpId]!!.get()}"
                    })
            }
        } else
            log.warn("not need config accessToken again")
    }

    fun removeSuite(suiteId: String){
        ApiContextMap.remove(suiteId)
    }
    /**
     * 取消授权后，将accessToken从map中移除
     * */
    fun removeCorpAuth(suiteId: String, corpId: String) {
        val ctx = ApiContextMap[suiteId]
        if (ctx == null) {//必须先进行suite的配置，指定好suiteId和secret
            log.warn("please config suite firstly")
            return
        }
        ctx.accessTokenMap.remove(corpId)
    }

}

class SuiteApiContext(
    id: String, //suiteId
    val secret: String,
    val token: String,
    val encodingAESKey: String? = null,
    val enableJsSdk: Boolean,

    privateKeyFilePath: String? = null,

    msgHandler: IWorkMsgHandler,
    eventHandler: IWorkEventHandler,
    suiteInfoHandler: ISuiteInfoHandler,

    //系统初始化时是空值，等腾讯推送过来后，才有值，才进行suiteAccessToken的创建
    var ticket: String? = null,//微信回调通知，每十分钟更新一次.若开发者想立即获得ticket推送值，可登录服务商平台，在第三方应用详情-回调配置，手动刷新ticket推送。
    var suiteAccessToken: ITimelyRefreshValue? = null,

    //corpId -> accessToken, suite ticket推送过来后，也被授权了，通过得到的永久授权码(第一次授权通知或从数据库中得到)，进行创建
    val accessTokenMap: HashMap<String, TimelyRefreshAccessToken3rd> = hashMapOf(),
    var jsTicket: ITimelyRefreshValue? = null // 由accessToken换取,参见 ThirdPartyWork.configAccessToken
)
{
    private val log = LoggerFactory.getLogger("SuiteApiContext")

    var msgHub: WorkMsgHub3rd? = null
    var wxBizMsgCrypt: WXBizMsgCrypt? = null
    var privateKey: PrivateKey? = null

    init {
        if (!encodingAESKey.isNullOrBlank()) {
            wxBizMsgCrypt = WXBizMsgCrypt(token, encodingAESKey)
            msgHub = WorkMsgHub3rd(msgHandler, eventHandler, wxBizMsgCrypt!!,suiteInfoHandler)
        } else {
            println("enableMsg=true, but not config token and encodingAESKey")
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

/**
 * suite_access_token刷新请求器
 * 注意：需要等suite ticket推送过来后才可创建
 * */
class SuiteRefresher(suiteId: String?) : VariableDataPostRefresher<SuiteParameters>(
    "suite_access_token",
    {
        if(suiteId == null) SuiteParameters(IsvWorkSingle.suiteId, IsvWorkSingle.ctx.secret, IsvWorkSingle.ctx.ticket!!)
        else IsvWorkMulti.ApiContextMap[suiteId]?.let { SuiteParameters(suiteId, it.secret, it.ticket!!) }
    },
    "https://qyapi.weixin.qq.com/cgi-bin/service/get_suite_token"
)

@Serializable
open class SuiteParameters(
    @SerialName("suite_id")
    val id: String, //suiteId
    @SerialName("suite_secret")
    val secret: String,
    @SerialName("suite_ticket")
    val ticket: String
)

/**
 * 动态刷新的suite_access_token
 * 注意：需要等suite ticket推送过来后才可创建
 * @param suiteId      以ww或wx开头应用id（对应于旧的以tj开头的套件id）
 * @param refresher      刷新器
 */
class TimelyRefreshSuiteToken @JvmOverloads constructor(
    suiteId: String,
    refresher: IRefresher = SuiteRefresher(suiteId)
) : TimelyRefreshValue(suiteId, refresher), ITimelyRefreshValue {
    //需要等ticket推送过来之后才加载
    //init { get()  }
    /**
     * 获取suite_access_token，若过期则会自动刷新
     */
    override fun get() = getRefreshedValue(UpdateType.WX_WORK_SUITE_TOKEN)
}


/**
 * 第三方访问企业api的accessToken刷新请求器
 * */
class AccessToken3rdRefresher(corpId: String, permanentCode: String, suiteAccessToken: ITimelyRefreshValue?) :
    PostRefresher<Map<String, String>>(
        "access_token",
        mapOf("auth_corpid" to corpId, "permanent_code" to permanentCode),
        " https://qyapi.weixin.qq.com/cgi-bin/service/get_corp_token?suite_access_token=${suiteAccessToken?.get()}"
    )

/**
 * 会自动刷新的 AccessToken
 * */
class TimelyRefreshAccessToken3rd @JvmOverloads constructor(
    suiteId: String?, corpId: String, permanentCode: String,
    refresher: IRefresher = AccessToken3rdRefresher(
        corpId, permanentCode,
        if(suiteId == null) IsvWorkSingle.ctx.suiteAccessToken
        else IsvWorkMulti.ApiContextMap[suiteId]?.suiteAccessToken
    )
) : TimelyRefreshValue("$suiteId/$corpId", refresher), ITimelyRefreshValue {
    init {
        get() //第一次加载
    }

    /**
     * 获取accessToken，若过期则会自动刷新
     */
    override fun get() = getRefreshedValue(UpdateType.WX_WORK_ACCESS_TOKEN)
}


/**
 * provider_token刷新请求器
 * @param corpid    是	服务商的corpid
 * @param provider_secret    是	服务商的secret，在服务商管理后台可见
 * */
class ProviderTokenRefresher(corpid: String, provider_secret: String) : PostRefresher<Map<String, String>>(
    "provider_access_token",
    mapOf("corpid" to corpid, "provider_secret" to provider_secret),
    "https://qyapi.weixin.qq.com/cgi-bin/service/get_provider_token"
)

/**
 * 服务商的provider_access_token
 * 以corpid、provider_secret（获取方法为：登录服务商管理后台->标准应用服务->通用开发参数，可以看到）
 * 换取provider_access_token，代表的是服务商的身份，而与应用无关。
 * 请求单点登录、注册定制化等接口需要用到该凭证。
 * */
class TimelyRefreshProviderToken @JvmOverloads constructor(
    corpId: String, secret: String,
    refresher: IRefresher = ProviderTokenRefresher(corpId, secret)
) : TimelyRefreshValue(corpId, refresher), ITimelyRefreshValue {
    init {
        get() //第一次加载
    }

    /**
     * 获取accessToken，若过期则会自动刷新
     */
    override fun get() = getRefreshedValue(UpdateType.WX_WORK_PROVIDER_TOKEN)
}