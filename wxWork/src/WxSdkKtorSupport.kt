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


import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.respondBoxKO
import com.github.rwsbillyang.ktorKit.server.respondBoxOK
import com.github.rwsbillyang.ktorKit.util.IpCheckUtil
import com.github.rwsbillyang.wxSDK.security.AesException
import com.github.rwsbillyang.wxSDK.security.JsAPI
import com.github.rwsbillyang.wxSDK.work.*
import com.github.rwsbillyang.wxSDK.work.isv.IsvWork
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkMulti
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory


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
            val agentIdOrSysAgentKey = call.parameters["agentId"]
            val ctx = WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentIdOrSysAgentKey)
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
            val agentIdOrSysAgentKey = call.parameters["agentId"]

            if(msgSignature == null || timestamp == null || nonce == null || corpId == null)
            {
                log.warn("Should not null: msgSignature=$msgSignature, timestamp=$timestamp, nonce=$nonce,corpId=$corpId, agentId=$agentIdOrSysAgentKey")
                call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
            }else{
                val ctx = WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentIdOrSysAgentKey)

                val msgHub = ctx?.msgHub
                if(msgHub == null)
                {
                    log.error("no agentContext or msgHub, corpId=$corpId, agentId=$agentIdOrSysAgentKey, msgHub=$msgHub, config it?")
                }

                log.info("handle post: uri=${call.request.uri}, body=$body")
                val reXml = if(agentIdOrSysAgentKey != null && Regex("""\d+""").matches(agentIdOrSysAgentKey))
                    msgHub?.handleXmlMsg(corpId, agentIdOrSysAgentKey, body, msgSignature, timestamp, nonce, encryptType)
                else
                    msgHub?.handleXmlMsg(corpId, null, body, msgSignature, timestamp, nonce, encryptType)
                if (reXml.isNullOrBlank())
                    call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
                else
                    call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)
            }
        }
    }
}



class OAuthResult(
    var code: String,// OK or KO
    val state: String?,//回传给前端用于校验
    var errMsg: String? = null,
    var unionId: String? = null,
    var deviceId: String? = null,
    var openId: String? = null,
    var userId: String? = null,
    var externalUserId: String? = null,
    var corpId: String? = null,
    var agentId: String? = null, //内建应用才有值，ISV第三方应用则为空
    var suiteId: String? = null
){
    fun serialize(): String{
        val map = mutableMapOf<String, String>()

        map["code"] = code
        if(state != null ) map["state"] = state
        if(errMsg != null )map["errMsg"] = errMsg!!
        if(unionId != null ) map["unionId"] = unionId!!
        if(deviceId != null ) map["deviceId"] = deviceId!!
        if(openId != null ) map["openId"] = openId!!
        if(userId != null ) map["userId"] = userId!!
        if(externalUserId != null ) map["externalUserId"] = externalUserId!!
        if(corpId != null ) map["corpId"] = corpId!!
        if(agentId != null ) map["agentId"] = agentId!!
        if(suiteId != null ) map["suiteId"] = suiteId!!
        return map.toList().joinToString("&"){"${it.first}=${it.second}"}
    }
}



/**
 * 企业微信oauth用户认证登录的api
 * */
fun Routing.wxWorkOAuthApi(
    notifyWebAppUrl: String = Work.oauthNotifyWebAppUrl,
    onResponseOauthUserDetail3rd: ((res: ResponseOauthUserDetail3rd) -> Unit)? = null //第三方应用需要获取用户敏感信息（头像和二维码）时提供，一般情况下没必要
) {
    val log = LoggerFactory.getLogger("wxWorkOAuthApi")


    /**
     * 腾讯在用户授权之后，将调用下面的api通知code，并附带上原state。
     *
     * 第二步：通过code换取网页授权access_token，然后必要的话获取用户信息。
     * 然后将一些登录信息通知到前端（调用前端提供的url）
     *
     * 用户同意授权后, 如果用户同意授权，页面将跳转至此处的redirect_uri/?code=CODE&state=STATE。
     * code作为换取access_token的票据，每次用户授权带上的code将不一样，code只能使用一次，5分钟未被使用自动过期。
     * "/api/wx/work/oauth/notify/{corpId}/{agentId}/{needUserInfo?}" or /api/wx/work/isv/oauth/notify/{suiteId}/{needUserInfo?}
     * */
    get(if(Work.isIsv) IsvWork.oauthNotifyPath + "/{suiteId}/{needUserInfo?}"
        else Work.oauthNotifyPath + "/{corpId}/{agentId}/{needUserInfo?}")
    { //默认路径： /api/wx/work/oauth/notify/{corpId}/{agentId} or /api/wx/work/isv/oauth/notify/{suiteId}
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        val result = OAuthResult("OK", state)
        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            log.warn("code or state is null, code=$code, state=$state")
            result.code = "KO"
            result.errMsg = "nullCodeOrState"
        } else {
            val suiteId = call.parameters["suiteId"]
            val corpId = call.parameters["corpId"]
            val agentId = call.parameters["agentId"]

            try {
                val api = OAuthApi(corpId, agentId, suiteId)
                val res = if(Work.isIsv){
                    api.getUserInfo3rd(code)
                }else{
                    api.getUserInfo(code)
                }

                if(!res.isOK()){
                    log.info("OAuthApi.getUserInfo:${Json.encodeToString(res)}")
                    result.code = "KO"
                    result.errMsg = "${res.errCode}: ${res.errMsg}"
                }else{
                    if(Work.isIsv && onResponseOauthUserDetail3rd != null && res.userTicket != null){
                        launch { onResponseOauthUserDetail3rd(api.getUserDetail3rd(res.userTicket!!)) }

//                    when(val needUserInfo = call.parameters["needUserInfo"]?.toInt()?: NeedUserInfoType.Force_Not_Need){
//                        NeedUserInfoType.Force_Not_Need -> {
//                            //do nothing
//                        }
//                        NeedUserInfoType.NeedIfNo -> {
//                            //TODO: 暂时都获取
//                            launch { onResponseOauthUserDetail3rd(api.getUserDetail3rd(res.userTicket!!)) }
//                        }
//                        NeedUserInfoType.NeedIfNoNameOrImg -> {
//                            //TODO: 暂时都获取
//                            launch { onResponseOauthUserDetail3rd(api.getUserDetail3rd(res.userTicket!!)) }
//                        }
//                        NeedUserInfoType.NeedByUserSettings -> {
//                            //TODO: 暂时都获取
//                            launch { onResponseOauthUserDetail3rd(api.getUserDetail3rd(res.userTicket!!)) }
//                        }
//                        NeedUserInfoType.ForceNeed -> {
//                            launch { onResponseOauthUserDetail3rd(api.getUserDetail3rd(res.userTicket!!)) }
//                        }
//                        else -> {
//                            log.warn("Not support needUserInfoType:$needUserInfo")
//                        }
//                    }
                    }

                    result.code = "OK"
                    result.corpId = res.corpId?:corpId
                    result.suiteId = suiteId
                    result.agentId = agentId
                    result.userId = res.userId
                    result.externalUserId = res.externalUserId
                    result.openId = res.openId
                    result.deviceId = res.deviceId
                    //result.unionId = res.u
                }

            }catch(e: IllegalArgumentException) {
                result.code = "KO"
                result.errMsg = e.message
            }

            //log.info("get oauth notify from tencent: schema=${call.request.origin.scheme},path=${call.request.origin.uri}")
            //通知到前端是使用http还是https，取决于微信公众号后台配置，若前端网页与后台配置不一致，将导致storage找不到对应的值，将会出问题
            //val host = call.request.origin.scheme +"://"+ call.request.host()

            //前端若是SPA，通知路径可能需要添加browserHistorySeparator: /wxwork/authNotify  or /#!/wxwork/authNotify
            val path = if (Work.browserHistorySeparator.isEmpty()) notifyWebAppUrl
            else "/${Work.browserHistorySeparator}${notifyWebAppUrl}"
            call.respondRedirect("$path?${result.serialize()}", permanent = false)
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
    val log = LoggerFactory.getLogger("workJsSdkSignature")

    //  /api/wx/work/jssdk/signature
    //corpId=wwfc2fead39b1e60dd&agentId=1000006&url=https%3A%2F%2Fwxadmin.zhuanzhuan360.com%2F
    get(Work.jsSdkSignaturePath) { //默认路径： /api/wx/work/jssdk/signature?corpId=XXX&type=agent_config
        val suiteId = call.request.queryParameters["suiteId"]//IsvWorkMulti时需非空
        val corpId = call.request.queryParameters["corpId"] //均不能空
        val agentId = call.request.queryParameters["agentId"]//内部多应用时提供 OrSysAgentKey
        val isInjectAgentConfig = call.request.queryParameters["type"] == "agent_config" //agent_config
        val url = (call.request.queryParameters["url"]?: call.request.headers["Referer"])?.split('#')?.firstOrNull()

        val jsTicket: String?
        if (url == null) {
            log.warn("request Referer or url parameter is null")
            call.respondBoxKO("request Referer is null")
        } else {
            if (corpId == null) {
                log.warn("invalid parameters: corpId is null")
                call.respondBoxKO("invalid parameters: corpId is null")
            } else {
                if (Work.isIsv) {
                    if (suiteId == null) {
                        log.warn("IsvWorkMulti invalid parameters: suiteId is null")
                        call.respondBoxKO("IsvWorkMulti invalid parameters: suiteId is null")
                    } else {
                        jsTicket =  if(isInjectAgentConfig)
                            IsvWorkMulti.ApiContextMap[suiteId]?.agentJsTicket?.get()
                            else IsvWorkMulti.ApiContextMap[suiteId]?.jsTicket?.get()

                        if (jsTicket == null) {
                            log.warn("IsvWorkMulti: jsTicket is null")
                            call.respondBoxKO("IsvWorkMulti: jsTicket is null")
                        } else {
                            //agentId在登录时得到
                            call.respond(DataBox("OK", null, JsAPI.getSignature(corpId, jsTicket, url,
                            if(isInjectAgentConfig)agentId else null)))
                        }
                    }
                } else {
                    if (agentId == null) {
                        log.warn("agentId is null")
                        call.respondBoxKO("invalid parameters: corpId and agentId could not be null")
                    } else {
                        jsTicket = if(isInjectAgentConfig){
                            WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)?.agentJsTicket?.get()
                        }else
                            WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)?.jsTicket?.get()

                        if (jsTicket == null) {
                            log.warn("jsTicket is null,config correctly or not enabled?")
                            call.respondBoxKO("WorkMulti: jsTicket is null, isInjectAgentConfig=$isInjectAgentConfig")
                        }
                        else
                            call.respond(DataBox.ok(JsAPI.getSignature(corpId, jsTicket, url, if(isInjectAgentConfig)agentId else null)))
                    }
                }
            }
        }
    }

}


//为其它系统访问的accessToken信息, 必须是本机访问
fun Routing.publishWorkAccessTokenApi(){
    val log = LoggerFactory.getLogger("publishWorkAccessTokenApi")
    route("/api/wx/work/"){
        get("/accessToken"){
            val fromIp = call.request.origin.remoteHost
            if(!IpCheckUtil.isFromLocalIp(fromIp)){
                log.warn("not from local IP: fromIp=$fromIp")
                call.respondBoxKO("visit forbidden")
            }else{
                val corpId = call.request.queryParameters["corpId"]
                val agentId = call.request.queryParameters["agentId"]
                if(corpId == null || agentId == null){
                    log.warn("invalid query parameter, no corpId/agentId, corpId=$corpId agentId=$agentId")
                    call.respondBoxKO("invalid query parameter, no corpId/agentId")
                }else{
                    val accessToken = WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)?.accessToken?.get()
                    if(accessToken == null){
                        call.respondBoxKO("no accessToken")
                    }else{
                        call.respondBoxOK(accessToken)
                    }
                }
            }
        }
        get("/jsTikect"){
            val fromIp = call.request.origin.remoteHost
            if(!IpCheckUtil.isFromLocalIp(fromIp)){
                log.warn("not from local IP: fromIp=$fromIp")
                call.respondBoxKO("visit forbidden")
            }else{
                val corpId = call.request.queryParameters["corpId"]
                val agentId = call.request.queryParameters["agentId"]
                if(corpId == null || agentId == null){
                    log.warn("invalid query parameter, no corpId/agentId, corpId=$corpId agentId=$agentId")
                    call.respondBoxKO("invalid query parameter, no corpId/agentId")
                }else{
                    val ticket = WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)?.jsTicket?.get()
                    if(ticket == null){
                        call.respondBoxKO("no ticket")
                    }else{
                        call.respondBoxOK(ticket)
                    }
                }
            }
        }
    }
}
