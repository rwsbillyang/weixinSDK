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

package com.github.rwsbillyang.wxSDK.work

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.rwsbillyang.wxSDK.bean.DataBox
import com.github.rwsbillyang.wxSDK.security.AesException
import com.github.rwsbillyang.wxSDK.security.JsAPI
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*


import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit



/**
 * 接收企业微信消息和事件, 然后分发处理。
 * 只适用于企业内部部署时使用
 * */
fun Routing.dispatchAgentMsgApi(corpId: String, agentId: Int) {
    val log = LoggerFactory.getLogger("agentMsgApi")

    val corpApiCtx = Work.ApiContextMap[corpId]
    if(corpApiCtx == null){
        log.warn("not init WorkApiContext for: corpId=$corpId")
        return
    }
    val ctx = corpApiCtx.agentMap[agentId]
    if(ctx == null){
        log.warn("not add agentId=$agentId,corpId=$corpId. please call Work.add(...) first")
        return
    }
    if(!ctx.enableMsg){
        log.warn("not enableMsg: agentId=$agentId,corpId=$corpId, ignore")
        return
    }
    if(ctx.wxBizMsgCrypt == null || ctx.msgHub == null){
        log.warn("wxBizMsgCrypt or msgHub is null, please init them correctly")
        return
    }

    route(ctx.msgNotifyUri) {
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

            val token = ctx.token

            //if (StringUtils.isAnyBlank(token, signature, timestamp, nonce,echostr)) {
            if(token.isNullOrBlank() || signature.isNullOrBlank() || timestamp.isNullOrBlank()
                    || nonce.isNullOrBlank() || echostr.isNullOrBlank()){
                log.warn("invalid parameters: token=$token, signature=$signature, timestamp=$timestamp, nonce=$nonce, echostr=$echostr")
                call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
            } else {
                try{
                    //第一个参数为null，不进行corpId的校验，公众号则校验，所有post的消息都校验
                    val str = ctx.wxBizMsgCrypt!!.verifyUrl(null, signature,timestamp,nonce,echostr)
                    call.respondText(str, ContentType.Text.Plain, HttpStatusCode.OK)
                }catch (e: AesException){
                    log.warn("AesException: ${e.message}")
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
            val timeStamp = call.request.queryParameters["timeStamp"]
            val nonce = call.request.queryParameters["nonce"]
            val encryptType = call.request.queryParameters["encrypt_type"]?:"aes"

            val reXml = ctx.msgHub!!.handleXmlMsg(null, body, msgSignature, timeStamp, nonce, encryptType)

            if(reXml.isNullOrBlank())
                call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
            else
                call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)
        }
    }
}

private val stateCache = Caffeine.newBuilder()
    .maximumSize(Long.MAX_VALUE)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .expireAfterAccess(1, TimeUnit.SECONDS)
    .build<String, Pair<String, Int>>()

class OAuthResult(
        val corpId: String,
        val agentId: Int,
        val userId: String?,
        val externalUserId: String?,
        val openId: String?,
        val deviceId: String?
)

/**
 * 企业微信oauth用户认证登录的api
 * */
fun Routing.wxWorkOAuthApi(
        oauthInfoPath: String = Work.oauthInfoPath,
        oauthNotifyPath: String = Work.oauthNotifyPath,
        oauthNotifyWebAppUrl: String = Work.oauthNotifyWebAppUrl,
        hasPermission: (ApplicationCall, OAuthResult)-> Boolean
) {
    val log = LoggerFactory.getLogger("wxWorkOAuthApi")

    /**
     * 前端webapp请求该api获取appid，state等信息，然后重定向到腾讯的授权页面，用户授权之后将重定向到下面的notify
     * @param corpId
     * @param agentId 要登录的agentID
     * @param host 跳转host，如："https：//www.example.com"
     * 前端重定向地址：https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect" 然后重定向到该url
     *
     * */
    get(oauthInfoPath){
        val corpId = call.request.queryParameters["corpId"]
        val agentId = call.request.queryParameters["agentId"]?.toInt()?:-1
        val host = call.request.queryParameters["host"]?:call.request.host()
        if(corpId.isNullOrBlank() || agentId < 0)
        {
            call.respond(HttpStatusCode.BadRequest, "no corpId or agentId")
        }else{
            val oAuthInfo = OAuthApi(corpId, agentId).prepareOAuthInfo(host + oauthNotifyPath, false)
            stateCache.put(oAuthInfo.state, Pair(corpId, agentId))
            call.respond(oAuthInfo)
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
     *
     * */
    get(oauthNotifyPath) {
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        val url = if (code.isNullOrBlank() || state.isNullOrBlank()) {
            //notify webapp fail
            log.warn("code or state is null, code=$code, state=$state")
            "$oauthNotifyWebAppUrl?state=$state&code=KO&msg=nullCodeOrState"
        } else {
            val pair = stateCache.getIfPresent(state)
            stateCache.invalidate(state)
            if (pair == null) {
                log.warn("not found corpId&agentId pair in cache, code=$code, state=$state")
                "$oauthNotifyWebAppUrl?state=$state&code=KO&msg=NotFoundAgentIdInCache"
            } else {
                val res = OAuthApi(pair.first, pair.second).getUserInfo(code)
                val part = if (res.isOK()) {
                    val isAllow = hasPermission(call, OAuthResult(pair.first, pair.second, res.userId, res.externalUserId, res.openId, res.deviceId))
                    if (isAllow) {
                        //log.info("res=$res")
                        var param = if(!res.userId.isNullOrBlank()) "&userId=${res.userId}" else ""
                        if(!res.externalUserId.isNullOrBlank()) param += "&externalUserId=${res.externalUserId}"
                        if(!res.openId.isNullOrBlank())param += "&openId=${res.openId}"
                        "$oauthNotifyWebAppUrl?state=$state&code=OK${param}"
                    } else {
                        "$oauthNotifyWebAppUrl?state=$state&code=KO&msg=Forbidden"
                    }
                } else {
                    "$oauthNotifyWebAppUrl?state=$state&code=KO&msg=${res.errCode}:${res.errMsg}"
                }
                "$part&corpId=${pair.first}&agentId=${pair.second}"
            }
        }
        call.respondRedirect(url, permanent = false)
    }
}


/**
 * 前端发出请求，获取使用jssdk时所需的认证签名
 * */
fun Routing.workJsSdkSignature(path: String = Work.jsSdkSignaturePath){
    get(path){
        val corpId = call.request.queryParameters["corpId"]
        val agentId = call.request.queryParameters["agentId"]?.toInt()
        val msg = if(corpId == null || agentId == null){
            "invalid parameters: corpId and agentId could not be null"
        }else{
            val ticket =  Work.ApiContextMap[corpId]?.agentMap?.get(agentId)?.jsTicket?.get()
            if(ticket == null){
                "corpId=$corpId,agentId=$agentId is configured?"
            }else{
                val url = call.request.headers["Referer"]
                if(url == null){
                    "request Referer is null"
                }else{
                    call.respond(DataBox("OK",null,JsAPI.getSignature(corpId,ticket, url)))
                    null
                }
            }
        }
        if(msg != null){
            call.respond(DataBox("KO", msg))
        }
    }
}


