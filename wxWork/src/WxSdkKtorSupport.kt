/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 15:09
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

package com.github.rwsbillyang.wxWork


import com.github.benmanes.caffeine.cache.Caffeine
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.wxSDK.bean.OAuthInfo
import com.github.rwsbillyang.wxSDK.security.AesException
import com.github.rwsbillyang.wxSDK.security.JsAPI
import com.github.rwsbillyang.wxSDK.work.*
import com.github.rwsbillyang.wxSDK.work.isv.IsvWork
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkMulti
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkSingle
import io.ktor.http.*

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


/**
 * 接收企业微信消息和事件, 然后分发处理。
 * 只适用于企业内部部署时使用
 *
 * 当为多应用模式时，提供corpId、agentId
 * */
fun Routing.dispatchAgentMsgApi() {
    val log = LoggerFactory.getLogger("dispatchAgentMsgApi")

    route(Work.msgNotifyUri+"/{corpId}/{agentId}") {
        /**
         *
         *
         * https://work.weixin.qq.com/api/doc/90000/90135/90238
         *
         * 为了能够让自建应用和企业微信进行双向通信，企业可以在应用的管理后台开启接收消息模式。
         * 开启接收消息模式的企业，需要提供可用的接收消息服务器URL（建议使用https）。
         * 开启接收消息模式后，用户在应用里发送的消息会推送给企业后台。此外，还可配置地理位置上报等事件消息，当事件触发时企业微信会把相应的数据推送到企业的后台。
         * 企业后台接收到消息后，可在回复该消息请求的响应包里带上新消息，企业微信会将该被动回复消息推送给用户。
         *
         *
         * 验证URL有效性
         * 当点击“保存”提交以上信息时，企业微信会发送一条验证消息到填写的URL，发送方法为GET。
         * 企业的接收消息服务器接收到验证请求后，需要作出正确的响应才能通过URL验证。
         *
         * 企业在获取请求时需要做Urldecode处理，否则可能会验证不成功
         * 你可以访问接口调试工具进行调试，依次选择 建立连接 > 接收消息。
         *
         * 假设接收消息地址设置为：http://api.3dept.com/，企业微信将向该地址发送如下验证请求：
         *
         * 请求方式：GET
         * 请求地址：http://api.3dept.com/?msg_signature=ASDFQWEXZCVAQFASDFASDFSS&timestamp=13500001234&nonce=123412323&echostr=ENCRYPT_STR
         * 参数说明
         * 参数	必须	说明
         * msg_signature	是	企业微信加密签名，msg_signature结合了企业填写的token、请求中的timestamp、nonce参数、加密的消息体
         * timestamp	是	时间戳
         * nonce	是	随机数
         * echostr	是	加密的字符串。需要解密得到消息内容明文，解密后有random、msg_len、msg、receiveid四个字段，其中msg即为消息内容明文
         * 企业后台收到请求后，需要做如下操作：
         *
         * 对收到的请求做Urldecode处理
         * 通过参数msg_signature对请求进行校验，确认调用者的合法性。
         * 解密echostr参数得到消息内容(即msg字段)
         * 在1秒内响应GET请求，响应内容为上一步得到的明文消息内容(不能加引号，不能带bom头，不能带换行符)
         * 以上2~3步骤可以直接使用验证URL函数一步到位。
         * 之后接入验证生效，接收消息开启成功。
         * */
        get {
            val signature = call.request.queryParameters["msg_signature"]
            val timestamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val echostr = call.request.queryParameters["echostr"]

            val corpId = call.parameters["corpId"]?:"NoCorpId"
            val agentId = call.parameters["agentId"]?.toInt()?:0
            val ctx = if(Work.isMulti)
            {
                WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)
            }else{
                WorkSingle.agentContext
            }

            val token = ctx?.token

            //if (StringUtils.isAnyBlank(token, signature, timestamp, nonce,echostr)) {
            if (token.isNullOrBlank() || signature.isNullOrBlank() || timestamp.isNullOrBlank()
                || nonce.isNullOrBlank() || echostr.isNullOrBlank()
            ) {
                log.warn("invalid parameters: token=$token, signature=$signature, timestamp=$timestamp, nonce=$nonce, echostr=$echostr")
                call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
            } else {
                try {
                    val str = ctx.wxBizMsgCrypt?.verifyUrl(corpId, signature, timestamp, nonce, echostr)
                    call.respondText(str?:"", ContentType.Text.Plain, HttpStatusCode.OK)
                } catch (e: AesException) {
                    log.warn("AesException: ${e.message}")
                    log.warn("Rx: token=$token, timestamp=$timestamp, nonce=$nonce, encryptEcho=$echostr")
                    call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
                }
            }
        }

        /**
         * 接收分发处理各类消息、事件、回调通知等
         *
         * 开启接收消息模式后，企业微信会将消息发送给企业填写的URL，企业后台需要做正确的响应。
         *
         * https://work.weixin.qq.com/api/doc/90000/90135/90238
         *
         * 接收消息协议的说明
         * 企业微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。如果企业在调试中，发现成员无法收到被动回复的消息，可以检查是否消息处理超时。
         * 当接收成功后，http头部返回200表示接收ok，其他错误码企业微信后台会一律当做失败并发起重试。
         * 关于重试的消息排重，有msgid的消息推荐使用msgid排重。事件类型消息推荐使用FromUserName + CreateTime排重。
         * 假如企业无法保证在五秒内处理并回复，或者不想回复任何内容，可以直接返回200（即以空串为返回包）。企业后续可以使用主动发消息接口进行异步回复。
         *
         * 接收消息请求的说明
         * 假设企业的接收消息的URL设置为http://api.3dept.com。
         * 请求方式：POST
         * 请求地址 ：http://api.3dept.com/?msg_signature=ASDFQWEXZCVAQFASDFASDFSS&timestamp=13500001234&nonce=123412323
         * 接收数据格式 ：
         * <xml>
         * <ToUserName><![CDATA[toUser]]></ToUserName>
         * <AgentID><![CDATA[toAgentID]]></AgentID>
         * <Encrypt><![CDATA[msg_encrypt]]></Encrypt>
         * </xml>
         * */
        post {
            val body: String = call.receiveText()

            val msgSignature = call.request.queryParameters["msg_signature"]
            val timestamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val encryptType = call.request.queryParameters["encrypt_type"] ?: "aes"

            val corpId = call.parameters["corpId"]
            val agentId = call.parameters["agentId"]?.toInt()

            if(msgSignature == null || timestamp == null || nonce == null || corpId == null || agentId == null)
            {
                log.warn("Should not null: msgSignature=$msgSignature, timestamp=$timestamp, nonce=$nonce,corpId=$corpId, agentId=$agentId")
                call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
            }else{
                val ctx = if(Work.isMulti)
                {
                    WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)
                }else{
                    WorkSingle.agentContext
                }

                val msgHub = ctx?.msgHub
                if(msgHub == null)
                {
                    log.error("no agentContext or msgHub, corpId=$corpId, agentId=$agentId, msgHub=$msgHub, config it?")
                }

                log.info("handle post: uri=${call.request.uri}, body=$body")
                val reXml = msgHub?.handleXmlMsg(corpId, agentId, body, msgSignature, timestamp, nonce, encryptType)

                if (reXml.isNullOrBlank())
                    call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
                else
                    call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)
            }


        }
    }
}


internal val stateCache = Caffeine.newBuilder()
    .maximumSize(Long.MAX_VALUE)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .expireAfterAccess(1, TimeUnit.SECONDS)
    .build<String, String>()

private val DefaultSnsApiScope = SnsApiScope.PrivateInfo

class OAuthResult(
    val corpId: String?,
    val userId: String?,
    val externalUserId: String?,
    val openId: String?,
    val deviceId: String?,
    val agentId: Int?, //内建应用才有值，ISV第三方应用则为空
    val suiteId: String?
)



/**
 * 企业微信oauth用户认证登录的api
 * */
fun Routing.wxWorkOAuthApi(
    onResponseOauthUserDetail3rd: ((res: ResponseOauthUserDetail3rd) -> Unit)? = null //第三方应用需要获取用户敏感信息（头像和二维码）时提供，一般情况下没必要
) {
    val log = LoggerFactory.getLogger("wxWorkOAuthApi")

    /**
     * 请求地址："/api/wx/work/oauth/info?scope=2&corpId=CORPID&agentId=AGENTID&suiteId=SUITEID&host=HOST" scope可选默认为2
     * 前端webapp请求该api获取appid，state等信息，然后重定向到腾讯的授权页面，用户授权之后将重定向到下面的notify
     * scope： 0， 1， 2 分别对应：snsapi_base, snsapi_userinfo, snsapi_privateinfo
     * host 跳转host，如："www.example.com"
     * 前端重定向地址：https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect" 然后重定向到该url
     * "/api/wx/work/oauth/info"
     * */
    get(Work.oauthInfoPath) {//默认路径： /api/wx/work/oauth/info?scope=2&corpId=CORPID&agentId=AGENTID
        val scope = when (call.request.queryParameters["scope"]) {
            "0" -> SnsApiScope.Base
            "1" -> SnsApiScope.UserInfo
            "2" -> SnsApiScope.PrivateInfo
            else -> DefaultSnsApiScope
        }
        //call.request.origin.scheme总得到是https,故通过指定的方式强行使用http or https
        val host = call.request.queryParameters["host"] ?: ( OAuthInfo.schema + "://"+ call.request.host())
        val suiteId = call.request.queryParameters["suiteId"]
        val corpId = call.request.queryParameters["corpId"]
        val agentId = call.request.queryParameters["agentId"]?.trim()?.toInt()

        val redirect: String
        try{
            val api = if (Work.isIsv) {
                if (Work.isMulti) {
                    redirect = "$host${IsvWork.oauthNotifyPath}/$suiteId"
                    OAuthApi(corpId?:"", agentId, suiteId)
                } else {
                    redirect = "$host${IsvWork.oauthNotifyPath}"
                    OAuthApi(corpId, agentId, suiteId)
                }
            } else {
                if (Work.isMulti) {
                    redirect = "$host${Work.oauthNotifyPath}/$corpId/$agentId"
                    OAuthApi(corpId, agentId, suiteId)
                } else {
                    redirect = "$host${Work.oauthNotifyPath}"
                    OAuthApi(corpId, agentId, suiteId)
                }
            }

            //log.info("wxwork oauth: notify url=$redirect")
            val oAuthInfo = api.prepareOAuthInfo(redirect, scope)
            //log.info("oAuthInfo=${oAuthInfo.toString()}")
            stateCache.put(oAuthInfo.state, scope.name)
            call.respond(oAuthInfo)
        }catch (e: Exception){
            call.respond(HttpStatusCode.BadRequest, "invalid parameter: "+ e.message)
        }
    }

    /**
     * 腾讯在用户授权之后，将调用下面的api通知code，并附带上原state。
     *
     * 第二步：通过code换取网页授权access_token，然后必要的话获取用户信息。
     * 然后将一些登录信息通知到前端（调用前端提供的url）
     *
     * 用户同意授权后, 如果用户同意授权，页面将跳转至此处的redirect_uri/?code=CODE&state=STATE。
     * code作为换取access_token的票据，每次用户授权带上的code将不一样，code只能使用一次，5分钟未被使用自动过期。
     * "/api/wx/work/oauth/notify/{corpId?}/{agentId?}" or /api/wx/work/isv/oauth/notify/{suiteId?}
     * */
    get(if(Work.isIsv) IsvWork.oauthNotifyPath + "/{suiteId?}"
        else Work.oauthNotifyPath + "/{corpId?}/{agentId?}")
    { //默认路径： /api/wx/work/oauth/notify/{corpId}/{agentId} or /api/wx/work/isv/oauth/notify/{suiteId}
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        //log.info("get oauth notify from tencent: schema=${call.request.origin.scheme},path=${call.request.origin.uri}")
        //val host = call.request.origin.scheme +"://"+ call.request.host()
        //前端若是SPA，通知路径可能需要添加browserHistorySeparator: /wxwork/authNotify  or /#!/wxwork/authNotify
        var url = if (Work.browserHistorySeparator.isEmpty()) Work.oauthNotifyWebAppUrl
                else "/${Work.browserHistorySeparator}${Work.oauthNotifyWebAppUrl}"

        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            log.warn("code or state is null, code=$code, state=$state")
            //val box = DataBox<OAuthResult>("KO", "nullCodeOrState")
            url = "$url?code=KO&msg=nullCodeOrState"
            call.respondRedirect(url, permanent = false)
        } else {
            val scope = stateCache.getIfPresent(state)?.let { SnsApiScope.valueOf(it) } ?: DefaultSnsApiScope
            stateCache.invalidate(state)

            val suiteId = call.parameters["suiteId"]
            val corpId = call.parameters["corpId"]
            val agentId = call.parameters["agentId"]?.toInt()

            try {
                val api = OAuthApi(corpId, agentId, suiteId)
                val res = if(Work.isIsv){
                    api.getUserInfo3rd(code)
                }else{
                    api.getUserInfo(code)
                }

                if (Work.isIsv && onResponseOauthUserDetail3rd != null && scope == SnsApiScope.PrivateInfo && res.userTicket != null) {
                    launch { onResponseOauthUserDetail3rd(api.getUserDetail3rd(res.userTicket!!)) }
                }

                val params = listOf(
                    Pair("code", "OK"),
                    Pair("state", state),
                    Pair("corpId", res.corpId?:corpId),
                    Pair("userId", res.userId),
                    Pair("externalUserId", res.externalUserId),
                    Pair("openId", res.openId),
                    Pair("deviceId", res.deviceId),
                    Pair("agentId", agentId?.toString()),
                    Pair("suiteId", suiteId),
                )
                    .filter{ !it.second.isNullOrBlank() }
                    .joinToString("&"){
                        "${it.first}=${it.second}"
                    }

                url = "$url?$params"
            }catch(e: IllegalArgumentException) {
                url = "$url?code=KO&msg=${e.message}"
            }

            //log.info("respondRedirect: $url")
            //通知到前端是使用http还是https，取决于微信公众号后台配置，若前端网页与后台配置不一致，将导致storage找不到对应的值，将会出问题
            call.respondRedirect(url, permanent = false)
        }
    }
}



/**
 * 前端发出请求，获取使用jssdk时所需的认证签名
 * 请求默认路径：/api/wx/work/jssdk/signature?corpId=XXX&type=agent_config
 *
 * 请求参数：corpId均需提供， 另：
 * type只在注入agentConfig时需提供。
 * Referer非空用于获取当前host，通常满足
 *
 * 当为ISV第三方多应用：需再提供suiteId，单应用无需提供
 * 当为内部多应用：需再提供agentId，单应用无需提供
 * */
fun Routing.workJsSdkSignature() {
    get(Work.jsSdkSignaturePath) { //默认路径： /api/wx/work/jssdk/signature?corpId=XXX&type=agent_config
        val suiteId = call.request.queryParameters["suiteId"]//IsvWorkMulti时需非空
        val corpId = call.request.queryParameters["corpId"] //均不能空
        val agentId = call.request.queryParameters["agentId"]?.trim()?.toInt()//内部多应用时提供
        val isAgent = call.request.queryParameters["type"] == "agent_config" //agent_config
        val url = (call.request.queryParameters["url"]?: call.request.headers["Referer"])?.split('#')?.firstOrNull()

        val jsTicket: String?
        if (url == null) {
            call.respond(HttpStatusCode.BadRequest, "request Referer is null")
        } else {
            if (corpId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid parameters: corpId is null")
            } else {
                if (Work.isIsv) {
                    if (Work.isMulti) {
                        if (suiteId == null) {
                            call.respond(HttpStatusCode.BadRequest, "IsvWorkMulti invalid parameters: suiteId is null")
                        } else {
                            jsTicket = if (isAgent)
                                IsvWorkMulti.ApiContextMap[suiteId]?.agentJsTicket?.get()
                            else
                                IsvWorkMulti.ApiContextMap[suiteId]?.corpJsTicket?.get()

                            if (jsTicket == null) {
                                call.respond(HttpStatusCode.BadRequest, "IsvWorkMulti: jsTicket is null")
                            } else {
                                //agentId在登录时得到
                                call.respond(DataBox("OK", null, JsAPI.getSignature(corpId, jsTicket, url)))
                            }
                        }
                    } else {
                        jsTicket = if (isAgent)
                            IsvWorkSingle.ctx.agentJsTicket?.get()
                        else
                            IsvWorkSingle.ctx.corpJsTicket?.get()

                        if (jsTicket == null)
                            call.respond(HttpStatusCode.BadRequest, "IsvWorkSingle: jsTicket is null")
                        else {
                            //agentId在登录时得到
                            call.respond(DataBox("OK", null, JsAPI.getSignature(corpId, jsTicket, url)))
                        }
                    }
                } else {
                    if (Work.isMulti) {
                        if (agentId == null) {
                            call.respond(HttpStatusCode.BadRequest, "invalid parameters: corpId and agentId could not be null")
                        } else {
                            jsTicket = if (isAgent)
                                WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)?.agentJsTicket?.get()
                            else
                                WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)?.corpJsTicket?.get()

                            if (jsTicket == null)
                                call.respond(HttpStatusCode.BadRequest, "WorkMulti: jsTicket is null, isAgent=$isAgent")
                            else
                                call.respond(DataBox.ok(JsAPI.getSignature(corpId, jsTicket, url, agentId)))
                        }
                    } else {
                        jsTicket = if (isAgent)
                            WorkSingle.agentContext.agentJsTicket?.get()
                        else
                            WorkSingle.agentContext.corpJsTicket?.get()

                        if (jsTicket == null)
                            call.respond(HttpStatusCode.BadRequest, "WorkSingle: jsTicket is null, isAgent=$isAgent")
                        else
                            call.respond(
                                DataBox(
                                    "OK",
                                    null,
                                    JsAPI.getSignature(corpId, jsTicket, url, WorkSingle.agentId)
                                )
                            )
                    }
                }
            }
        }
    }

}


