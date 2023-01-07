/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 11:34
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
import com.github.rwsbillyang.wxSDK.security.AesException
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.isv.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

//当为多应用模式时，提供suiteId
fun Routing.isvDispatchMsgApi(){
    val log = LoggerFactory.getLogger("isvDispatchMsgApi")


    //"/api/wx/work/isv/msg/{suiteId}"
    route(IsvWork.msgNotifyPath+"/{suiteId}") {
        get {
            val suiteId = call.parameters["suiteId"]?:"NoSuiteId"

            val signature = call.request.queryParameters["msg_signature"]
            val timestamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val echostr = call.request.queryParameters["echostr"]

            val ctx = IsvWorkMulti.ApiContextMap[suiteId]

            val token = ctx?.token

            //if (StringUtils.isAnyBlank(token, signature, timestamp, nonce,echostr)) {
            if(token.isNullOrBlank() || signature.isNullOrBlank() || timestamp.isNullOrBlank()
                || nonce.isNullOrBlank() || echostr.isNullOrBlank()){
                log.warn("invalid parameters: token=$token, signature=$signature, timestamp=$timestamp, nonce=$nonce, echostr=$echostr")
                call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
            } else {
                try{
                    val str = ctx.wxBizMsgCrypt?.verifyUrl(suiteId, signature,timestamp,nonce,echostr)
                    call.respondText(str?:"", ContentType.Text.Plain, HttpStatusCode.OK)
                }catch (e: AesException){
                    log.warn("AesException: ${e.message}")
                    call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
                }
            }
        }
    }

    post{
        val suiteId = call.parameters["suiteId"]?:"NoSuiteId"
        val ctx = IsvWorkMulti.ApiContextMap[suiteId]

        val body: String = call.receiveText()

        val msgSignature = call.request.queryParameters["msg_signature"]
        val timestamp = call.request.queryParameters["timestamp"]
        val nonce = call.request.queryParameters["nonce"]
        val encryptType = call.request.queryParameters["encrypt_type"]?:"aes"
        if(msgSignature == null || timestamp == null || nonce == null)
        {
            log.warn("Should not null: msgSignature=$msgSignature, timestamp=$timestamp, nonce=$nonce")
            call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
        }else{
            val reXml = ctx?.msgHub?.handleXmlMsg(suiteId, null, body, msgSignature, timestamp, nonce, encryptType)

            if(reXml.isNullOrBlank())
                call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
            else
                call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)
        }

    }
}

/**
 * 从第三方网站发起对app的授权
 *
 * @param onGetPermanentAuthInfo 成功获取到永久授权码后的回调
 */
fun Routing.isvAuthFromOutsideApi(onGetPermanentAuthInfo: (suiteId: String, info: ResponsePermanentCodeInfo)->Unit){
    val stateCache = Caffeine.newBuilder().maximumSize(Long.MAX_VALUE)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<String, String>()
    /**
     * 前端请求该endpoint，从服务商网站发起应用授权
     * */
    get(IsvWork.authFromOutsidePath+"/{suiteId}"){
        val suiteId = call.parameters["suiteId"]
        if(suiteId.isNullOrBlank())
        {
            call.respond(HttpStatusCode.BadRequest, "invalid parameter: suiteId")
        }else{
            //获取预授权码后，重定向到用户授权页面
            val preAuthCode = ThirdPartyApi(suiteId).getPreAuthCode()
            if(preAuthCode.isOK() && !preAuthCode.pre_auth_code.isNullOrBlank()){
                val state = WXBizMsgCrypt.getRandomStr()
                stateCache.put(state,suiteId)
                val host = call.request.host() //如："https：//www.example.com"
                val redirect = URLEncoder.encode("$host/${IsvWork.oauthNotifyPath}/${suiteId}","UTF-8")
                //引导用户进入授权页
                val url = "https://open.work.weixin.qq.com/3rdapp/install?suite_id=$suiteId&pre_auth_code=${preAuthCode.pre_auth_code}&redirect_uri=$redirect&state=$state"
                call.respondRedirect(url)
            }else{
                call.respond(HttpStatusCode.BadRequest, "fail to get preAuthCode: "+preAuthCode.errMsg)
            }
        }
    }

    /**
     * 腾讯发送通知结果：临时授权码
     *  用户授权成功后，接收来自企业微信的通知，得到临时授权码，再使用临时授权码获取永久授权码
     * 获取永久授权码后，通过指定的回调onGetPermanentAuthInfo处理永久授权码，期间任何错误处理均通过指定的web通知path通知到前端
     * */
    get(IsvWork.oauthNotifyPermanentCodePath+"/{suiteId}"){//  /api/wx/work/isv/oauth/notify/{suiteId}
        val authCode = call.request.queryParameters["auth_code"]
        //val expiresIn = call.request.queryParameters["expires_in"]
        val state = call.request.queryParameters["state"]

        val webNotifyResult = if(state.isNullOrBlank() || authCode.isNullOrBlank()){
            "?ret=KO&msg=no_state_or_authCode"
        }else{
            val suiteIdInCache = stateCache.getIfPresent(state)
            val suiteId = call.parameters["suiteId"]
            if(suiteIdInCache == null){
                "?ret=KO&msg=no_suiteId_in_cache"
            }else if(suiteIdInCache != suiteId){
                "?ret=KO&msg=wrong_suiteId"
            }else{
                val permanentCodeInfo = ThirdPartyApi(suiteId).getPermanentCode(authCode)
                if(permanentCodeInfo.isOK()){
                    launch {
                        stateCache.invalidate(state)
                        onGetPermanentAuthInfo(suiteId, permanentCodeInfo)
                    }
                    "?ret=OK"
                }else{
                    "?ret=KO&msg=no_suiteIdInCache"
                }
            }
        }

        val url = call.request.host() + IsvWork.permanentWebNotifyPath + webNotifyResult
        call.respondRedirect(url, permanent = false)
    }
}

