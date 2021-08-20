/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-11 11:25
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


import com.github.benmanes.caffeine.cache.Caffeine
import com.github.rwsbillyang.wxSDK.bean.DataBox
import com.github.rwsbillyang.wxSDK.security.AesException
import com.github.rwsbillyang.wxSDK.security.JsAPI
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


fun Routing.isvDispatchMsgApi(suiteId: String){
    val log = LoggerFactory.getLogger("suiteMsgApi")

    val suiteApiCtx = ThirdPartyWork.ApiContextMap[suiteId]
    if(suiteApiCtx == null){
        log.warn("not init suiteApiCtx for: suiteId=$suiteId")
        return
    }

    if(suiteApiCtx.wxBizMsgCrypt == null || suiteApiCtx.msgHub == null){
        log.warn("wxBizMsgCrypt or msgHub is null, please init them correctly")
        return
    }

    //"/api/wx/work/3rd"
    route(ThirdPartyWork.msgNotifyPath) {
        get {
            val appId = call.parameters["suiteId"]

            val signature = call.request.queryParameters["msg_signature"]
            val timestamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val echostr = call.request.queryParameters["echostr"]

            val ctx = ThirdPartyWork.ApiContextMap[appId]
            val token = ctx?.token

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
    }

    post{
        val appId = call.parameters["suiteId"]
        val ctx = ThirdPartyWork.ApiContextMap[appId]

        val body: String = call.receiveText()

        val msgSignature = call.request.queryParameters["msg_signature"]
        val timeStamp = call.request.queryParameters["timeStamp"]
        val nonce = call.request.queryParameters["nonce"]
        val encryptType = call.request.queryParameters["encrypt_type"]?:"aes"

        val reXml = ctx?.msgHub!!.handleXmlMsg(null, body, msgSignature, timeStamp, nonce, encryptType)

        if(reXml.isNullOrBlank())
            call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
        else
            call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)
    }
}



private val thirdStateCache = Caffeine.newBuilder()
    .maximumSize(Long.MAX_VALUE)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .expireAfterAccess(1, TimeUnit.SECONDS)
    .build<String, String>()
/**
 * 第三方授权认证的api
 *
 * @param onGetPermanentAuthInfo 成功获取到永久授权码后的回调
 */
fun Routing.isvAuthFromOutsideApi(onGetPermanentAuthInfo: (suiteId: String, info: ResponsePermanentCodeInfo)->Unit){
    /**
     * 前端请求该endpoint，从服务商网站发起应用授权
     * */
    get(ThirdPartyWork.authFromOutsidePath){
        val suiteId = call.parameters["suiteId"]
        if(suiteId.isNullOrBlank())
        {
            call.respond(DataBox("KO", "invalid parameter: suiteId"))
        }else{
            //获取预授权码后，重定向到用户授权页面
            val preAuthCode = ThirdPartyApi(suiteId).getPreAuthCode()
            if(preAuthCode.isOK() && !preAuthCode.pre_auth_code.isNullOrBlank()){
                val state = RandomStringUtils.randomAlphanumeric(16)
                thirdStateCache.put(state,suiteId)
                val host = call.request.host() //如："https：//www.example.com"
                val redirect = URLEncoder.encode("$host/${ThirdPartyWork.authCodeNotifyPath}/${suiteId}","UTF-8")
                //引导用户进入授权页
                val url = "https://open.work.weixin.qq.com/3rdapp/install?suite_id=$suiteId&pre_auth_code=${preAuthCode.pre_auth_code}&redirect_uri=$redirect&state=$state"
                call.respondRedirect(url)
            }else{
                call.respond(DataBox("KO", "fail to get preAuthCode: "+preAuthCode.errMsg))
            }
        }
    }

    /**
     * 腾讯发送通知结果：临时授权码
     *  用户授权成功后，接收来自企业微信的通知，得到临时授权码，再使用临时授权码获取永久授权码
     * 获取永久授权码后，通过指定的回调onGetPermanentAuthInfo处理永久授权码，期间任何错误处理均通过指定的web通知path通知到前端
     * */
    get(ThirdPartyWork.authCodeNotifyPath){
        val authCode = call.request.queryParameters["auth_code"]
        //val expiresIn = call.request.queryParameters["expires_in"]
        val state = call.request.queryParameters["state"]

        val webNotifyResult = if(state.isNullOrBlank() || authCode.isNullOrBlank()){
            "?ret=KO&msg=no_state_or_authCode"
        }else{
            val suiteIdInCache = thirdStateCache.getIfPresent(state)
            val suiteId = call.parameters["suiteId"]
            if(suiteIdInCache == null){
                "?ret=KO&msg=no_suiteId_in_cache"
            }else if(suiteIdInCache != suiteId){
                "?ret=KO&msg=wrong_suiteId"
            }else{
                val permanentCodeInfo = ThirdPartyApi(suiteId).getPermanentCode(authCode)
                if(permanentCodeInfo.isOK()){
                    launch {
                        thirdStateCache.invalidate(state)
                        onGetPermanentAuthInfo(suiteId, permanentCodeInfo)
                    }
                    "?ret=OK"
                }else{
                    "?ret=KO&msg=no_suiteIdInCache"
                }
            }
        }

        val url = call.request.host() + ThirdPartyWork.permanentWebNotifyPath + webNotifyResult
        call.respondRedirect(url, permanent = false)
    }
}


/**
 * 前端发出请求，获取使用jssdk时所需的认证签名
 * */
fun Routing.isvJsSdkSignature(){
    get(ThirdPartyWork.jsSdkSignaturePath){
        val suiteId = call.request.queryParameters["suiteId"]
        val corpId = call.request.queryParameters["corpId"]

        val msg = if(suiteId == null || corpId == null){
            "invalid parameters: corpId or suiteId could not be null"
        }else{
            val ticket =  ThirdPartyWork.ApiContextMap[suiteId]?.jsTicket?.get()
            if(ticket == null){
                "suiteId=$suiteId, corpId=$corpId is configured?"
            }else{
                val url = call.request.headers["Referer"]
                if(url == null){
                    "request Referer is null"
                }else{
                    call.respond(DataBox("OK",null, JsAPI.getSignature(corpId,ticket, url)))
                    null
                }
            }
        }
        if(msg != null){
            call.respond(DataBox("KO", msg))
        }
    }
}
