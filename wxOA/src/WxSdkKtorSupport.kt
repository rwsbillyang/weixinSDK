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

package com.github.rwsbillyang.wxOA

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.wxSDK.bean.OAuthInfo
import com.github.rwsbillyang.wxSDK.officialAccount.OAuthApi
import com.github.rwsbillyang.wxSDK.officialAccount.OfficialAccount
import com.github.rwsbillyang.wxSDK.officialAccount.ResponseOauthAccessToken
import com.github.rwsbillyang.wxSDK.officialAccount.ResponseUserInfo
import com.github.rwsbillyang.wxSDK.security.JsAPI
import com.github.rwsbillyang.wxSDK.security.SignUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit




fun Routing.dispatchMsgApi(path: String = OfficialAccount.msgUri) {
    val log = LoggerFactory.getLogger("officialAccountMsgApi")

    route(path) {
        /**
         * 使用https有可能失败
         * 公众号后台中的开发者->配置开发服务器URL，必须调用此函数进行处理GET请求
         *
         * https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
         * https://developers.weixin.qq.com/doc/offiaccount/Getting_Started/Getting_Started_Guide.html
         * */
        get("/{appId}") {
            val appId = call.parameters["appId"]
            var msg: String
            if (appId.isNullOrBlank()) {
                msg = "no appId, wrong uri"
                log.warn(msg)
            } else {
                val signature = call.request.queryParameters["signature"]
                val timestamp = call.request.queryParameters["timestamp"]
                val nonce = call.request.queryParameters["nonce"]
                val echostr = call.request.queryParameters["echostr"]

                val ctx = OfficialAccount.ApiContextMap[appId]
                val token = ctx?.token
                if (token.isNullOrBlank()) {
                    msg = "not config token, ctx=$ctx"
                    log.warn(msg)
                } else {
                    msg = echostr ?: ""
                    if (signature.isNullOrBlank() || timestamp.isNullOrBlank() || nonce.isNullOrBlank() || echostr.isNullOrBlank()) {
                        log.warn("invalid parameters: token=$token, signature=$signature, timestamp=$timestamp, nonce=$nonce,echostr=$echostr")
                        msg = "invalid parameters"
                    } else {
                        if (!SignUtil.checkSignature(token, signature, timestamp, nonce)) {
                            msg = "fail to check signature"
                            log.warn(msg)
                        }
                    }
                }
            }
            //log.info("respond $msg")
            call.respondText(msg, ContentType.Text.Plain, HttpStatusCode.OK)
        }

        /**
         * 公众号后台中的开发者->配置开发服务器URL，必须调用此函数进行处理POST请求
         *
         * 当普通微信用户向公众账号发消息时，微信服务器将POST消息的XML数据包到开发者填写的URL上。
         *
         * 假如服务器无法保证在五秒内处理并回复，可以直接回复空串，微信服务器不会对此作任何处理，并且不会发起重试。
         *  1） 被动回复消息，即发送被动响应消息，不同于客服消息接口
         *  2） 它其实并不是一种接口，而是对微信服务器发过来消息的一次回复
         *  3） 收到粉丝消息后不想或者不能5秒内回复时，需回复“success”字符串（下文详细介绍）
         *  4） 客服接口在满足一定条件下随时调用
         *
         *   假如服务器无法保证在五秒内处理回复，则必须回复“success”（推荐方式）或者“”（空串），否则微信后台会发起三次重试。
         *   三次重试后，依旧没有及时回复任何内容，系统自动在粉丝会话界面出现错误提示“该公众号暂时无法提供服务，请稍后再试”。
         *
         *
         *   启用加解密功能（即选择兼容模式或安全模式）后，公众平台服务器在向公众账号服务器配置地址（可在“开发者中心”修改）推送消息时，
         *   URL将新增加两个参数（加密类型和消息体签名），并以此来体现新功能。加密算法采用AES。
         *   1、在接收已授权公众号消息和事件的 URL 中，增加 2 个参数：encrypt_type（加密类型，为 aes）和 msg_signature（消息体签名，
         *   用于验证消息体的正确性）（此前已有 2 个参数，为时间戳 timestamp，随机数 nonce）
         *   2、postdata 中的 XML 体，将使用第三方平台申请时的接收消息的加密 symmetric_key（也称为 EncodingAESKey）来进行加密。
         *
         *   开发者安全模式（推荐）：公众平台发送消息体的内容只含有密文，公众账号回复的消息体也为密文。但开发者通过客服接口等API调用形式向用户发送消息，则不受影响。
         * */
        post("/{appId}") {
            val appId = call.parameters["appId"]
            if (appId.isNullOrBlank()) {
                log.warn("no appId, wrong uri")
                call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
            } else {
                val apiCtx = OfficialAccount.ApiContextMap[appId]
                if (apiCtx == null) {
                    log.warn("not config officialAccount: appId=$appId")
                } else {
                    val body: String = call.receiveText()

                    val msgSignature = call.request.queryParameters["msg_signature"]
                    val timeStamp = call.request.queryParameters["timestamp"]
                    val nonce = call.request.queryParameters["nonce"]
                    val encryptType = call.request.queryParameters["encrypt_type"] ?: "security"

                    if (msgSignature == null || timeStamp == null || nonce == null) {
                        log.warn("Should not null: msgSignature=$msgSignature, timeStamp=$timeStamp, nonce=$nonce")
                        call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
                    } else {
                        val reXml =
                            apiCtx.msgHub.handleXmlMsg(appId, null, body, msgSignature, timeStamp, nonce, encryptType)

                        if (reXml.isNullOrBlank())
                            call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
                        else
                            call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)
                    }

                }
            }
        }
    }
}


fun Routing.oAuthApi(
    oauthInfoPath: String = OfficialAccount.oauthInfoPath,
    notifyPath1: String = OfficialAccount.oauthNotifyPath1,
    notifyPath2: String = OfficialAccount.oauthNotifyPath2,
    notifyWebAppUrl: String = OfficialAccount.oauthNotifyWebAppUrl,
    needUserInfo: ((String?, String) -> Boolean?)? = null, //第一个参数时owner，系统注册用户id，用于查询用户设置，第二个参数是访客的openId
    onGetOauthAccessToken: ((ResponseOauthAccessToken, appId: String) -> Unit)? = null,
    onGetUserInfo: ((info: ResponseUserInfo, appId: String) -> Unit)? = null
) {
    val log = LoggerFactory.getLogger("oAuthApi")
    //保存state，用于校验非法请求,只是前端校验
    val stateCache = Caffeine.newBuilder().maximumSize(Int.MAX_VALUE.toLong())
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, String>()
    /**
     * 当不能明确需要是否获取用户信息时，调用此API，上传owner参数，用于与notify1中获取的openId决定是否需要获取用户信息
     * owner用于获取用户设置，openId用于查询是否已经有值
     *
     * 前端webapp请求该api获取appid，state等信息，然后重定向到腾讯的授权页面，用户授权之后将重定向到下面的notify
     * @param owner 系统注册用户id，用于获取用户设置
     * @param host 跳转host，如："https：//www.example.com"，用于构建跳转url
     * 前端重定向地址：https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect" 然后重定向到该url
     *
     * */
    get(oauthInfoPath) {
        val appId = call.request.queryParameters["appId"] ?: OfficialAccount.ApiContextMap.values.firstOrNull()?.appId
        val owner = call.request.queryParameters["owner"] //系统用户注册id，传递该参数，目的在于notify1中使用它确定是否获取用户信息，然后通知前端跳转
        val openId = call.request.queryParameters["openId"]//若已经登录过只有openId，若再需获取用户信息时，上传openId即可，有可能直接进入第二步
        if (appId.isNullOrBlank()) {
            log.warn("no appId in query parameters for oauth and no config oa")
            call.respond(HttpStatusCode.BadRequest, "no appId in query parameters and no config oa")
        } else {
            //call.request.origin.scheme总得到是https,故通过指定的方式强行使用http or https
            val host = call.request.queryParameters["host"] ?: (OAuthInfo.schema +"://"+ call.request.host())
            log.info("wxOA oauth, host=$host")
            if (openId.isNullOrBlank()) {
                //第一次只获取openId，不获取用户信息
                val oAuthInfo = OAuthApi(appId).prepareOAuthInfo("$host$notifyPath1/$appId", false)
                if (owner != null) stateCache.put(oAuthInfo.state, owner)
                call.respond(oAuthInfo)
            } else {
                val isNeedUserInfo =
                    needUserInfo?.let { needUserInfo(owner, openId) } ?: OfficialAccount.defaultGetUserInfo
                val oAuthInfo = OAuthApi(appId).prepareOAuthInfo("$host$notifyPath2/$appId", isNeedUserInfo)
                call.respond(oAuthInfo)
            }
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
    get("$notifyPath1/{appId}") {
        val appId = call.parameters["appId"] ?: OfficialAccount.ApiContextMap.values.firstOrNull()?.appId
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        log.info("wxOA oauth, notify1 schema=${call.request.origin.scheme}")

        var url = "$notifyWebAppUrl?state=$state&step=1&appId=${appId}"

        url += if (appId == null || code.isNullOrBlank() || state.isNullOrBlank()) {
            "&code=KO&msg=null_appId_or_code_or_state"
        } else {
            val owner = stateCache.getIfPresent(state)
//            if (owner == null) {
//                log.warn("state=$state for owner is not present in cache, ip=${call.request.origin.host},ua=${call.request.userAgent()}")
//            }
            val oauthAi = OAuthApi(appId)
            val res = oauthAi.getAccessToken(code)
            if (res.isOK()) {
                if (res.openId.isNullOrBlank()) {
                    log.warn("openid is blank: openid=${res.openId}")
                    // "&code=KO&msg=微信出错了，请重新打开"
                    "&code=OK&needUserInfo=1"
                } else {
                    val isNeedUserInfo =
                        needUserInfo?.let { needUserInfo(owner, res.openId!!) } ?: OfficialAccount.defaultGetUserInfo
                    if (!isNeedUserInfo) {
                        stateCache.invalidate(state)
                    }
                    val isNeedUserInfoInt = if (isNeedUserInfo) 1 else 0
                    "&code=OK&openId=${res.openId}&needUserInfo=$isNeedUserInfoInt" //通知前端是否跳转到第二步获取userInfo
                }
            } else {
                log.warn("fail when oauthAi.getAccessToken: ${res.errMsg}")
                "&code=KO&msg=${res.errMsg}"
            }
        }

        call.respondRedirect(url, permanent = false)
    }

    get("$notifyPath2/{appId}") {
        val appId = call.parameters["appId"] ?: OfficialAccount.ApiContextMap.values.firstOrNull()?.appId
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        var url = "$notifyWebAppUrl?state=$state&step=2&appId=$appId"

        if (appId == null || code.isNullOrBlank() || state.isNullOrBlank()) {
            url += "&code=KO&msg=null_appId_or_code_or_state"
        } else {
            //val owner = stateCache.getIfPresent(state)
            stateCache.invalidate(state)
//            if(owner == null){
//                log.warn("step2: state=$state for owner is not present in cache, ip=${call.request.origin.host},ua=${call.request.userAgent()}")
//            }

            val oauthAi = OAuthApi(appId)
            val res:ResponseOauthAccessToken = oauthAi.getAccessToken(code)
            url += if (res.isOK() && res.openId != null) {
                onGetOauthAccessToken?.let { run { it.invoke(res, appId) } }
                if (res.accessToken != null ) {
                    val resUserInfo = oauthAi.getUserInfo(res.accessToken!!, res.openId!!)
                    if (resUserInfo.isOK()) {
                        onGetUserInfo?.let { run { it.invoke(resUserInfo, appId) } }//async save fan user info
                    } else {
                        log.warn("fail getUserInfo: $resUserInfo")
                    }
                }
                "&code=OK&openId=${res.openId}"
            } else {
                "&code=KO&msg=${res.errMsg}"
            }
        }
        //notify webapp
        call.respondRedirect(url, permanent = false)
    }
}


fun Routing.jsSdkSignature(path: String = OfficialAccount.jsSdkSignaturePath) {
    //val log = LoggerFactory.getLogger("jsSdkSignature")
    get(path) {
        val appId = call.request.queryParameters["appId"] ?: OfficialAccount.ApiContextMap.values.firstOrNull()?.appId
        val url = (call.request.queryParameters["url"]?: call.request.headers["Referer"])?.split('#')?.firstOrNull()
        val msg = if(url == null){
            "request Referer is null"
        }else{
            if (appId.isNullOrBlank()) {
                //call.respond(HttpStatusCode.BadRequest, "no appId in query parameters and no config oa")
                "no appId in query parameters and no config oa"
            } else {
                val ticket = OfficialAccount.ApiContextMap[appId]?.ticket?.get()
                if (ticket == null) {
                    "appId=$appId is configured?"
                } else {
                    //log.info("url=$url, referer=$referer")
                    call.respond(DataBox.ok(JsAPI.getSignature(appId, ticket, url)))
                    null
                }
            }
        }

        if (msg != null) {
            call.respond(HttpStatusCode.BadRequest, msg)
        }
    }
}
