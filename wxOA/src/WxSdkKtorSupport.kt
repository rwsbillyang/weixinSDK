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

import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.ktorKit.server.respondBoxKO
import com.github.rwsbillyang.ktorKit.server.respondBoxOK
import com.github.rwsbillyang.ktorKit.util.IpCheckUtil
import com.github.rwsbillyang.wxOA.account.NeedUserInfoType
import com.github.rwsbillyang.wxOA.fan.FanService
import com.github.rwsbillyang.wxOA.fan.toGuest
import com.github.rwsbillyang.wxOA.fan.toOauthToken
import com.github.rwsbillyang.wxSDK.officialAccount.*
import com.github.rwsbillyang.wxSDK.security.JsAPI
import com.github.rwsbillyang.wxSDK.security.SignUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory


//为其它系统访问的accessToken信息, 必须是本机访问
fun Routing.publishAccessTokenApi(){
    val log = LoggerFactory.getLogger("publishAccessTokenApi")
    route("/api/wx/oa/"){
        get("/accessToken"){
            val fromIp = call.request.origin.remoteHost
            if(!IpCheckUtil.isFromLocalIp(fromIp)){
                log.warn("not from local IP: fromIp=$fromIp")
                call.respondBoxKO("visit forbidden")
            }else{
                val appId = call.request.queryParameters["appId"]
                if(appId == null){
                    log.warn("invalid query parameter, no appId")
                    call.respondBoxKO("invalid query parameter, no appId")
                }else{
                    val accessToken = OfficialAccount.ApiContextMap[appId]?.accessToken?.get()
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
                val appId = call.request.queryParameters["appId"]
                if(appId == null){
                    log.warn("invalid query parameter, no appId")
                    call.respondBoxKO("invalid query parameter, no appId")
                }else{
                    val ticket = OfficialAccount.ApiContextMap[appId]?.ticket?.get()
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

/**
 * 通知给前端的结果
 * */
class OAuthNotifyResult(
    val step: Int,// 1 or 2
    var code: String,// OK or KO
    val appId: String? = null,
    val state: String? = null,//回传给前端用于校验
    var openId: String? = null,
    var msg: String? = null,
    var unionId: String? = null, //for step2
    var needEnterStep2: Int? = null // for step1: 1 enter step2, or else ends
){
    fun serialize(): String{
        val map = mutableMapOf<String, String>()
        map["step"] = step.toString()
        map["code"] = code
        if(appId != null ) map["appId"] = appId
        if(state != null ) map["state"] = state
        if(openId != null ) map["openId"] = openId!!
        if(msg != null )map["msg"] = msg!!
        if(unionId != null ) map["unionId"] = unionId!!
        if(needEnterStep2 != null ) map["needEnterStep2"] = needEnterStep2!!.toString()

        return map.toList().joinToString("&"){"${it.first}=${it.second}"}
    }
}
fun Routing.oAuthApi(
    notifyPath1: String = OfficialAccount.oauthNotifyPath1,
    notifyPath2: String = OfficialAccount.oauthNotifyPath2,
    notifyWebAppUrl: String = OfficialAccount.oauthNotifyWebAppUrl,
    needUserInfoSettingsBlock: ((String?, String) -> Boolean?)? = null, //第一个参数时owner，系统注册用户id，用于查询用户设置，第二个参数是访客的openId
    onGetOauthAccessToken: ((ResponseOauthAccessToken, appId: String) -> Unit)? = null,
    onGetUserInfo: ((info: ResponseUserInfo, appId: String) -> Unit)? = null
) {
    val fanService: FanService by inject()

    val log = LoggerFactory.getLogger("oAuthApi")

    /**
     * Step1: 无需用户授权，只获取openId
     * 此地址为前端authorize时，编码后的redirect_uri参数，供腾讯通知调用
    //前端LoginParam中的公众号部分：
    export interface LoginParam {
        appId?: string //公众号
        corpId?: string //企业微信
        suiteId?: string //企业微信ISV
        agentId?: string //企业微信
        from?: string //需要登录的页面
        owner?: string //用于公众号 用于判断用户设置是否获取用户信息
        needUserInfo: number // 用于公众号 或企业微信
        authStorageType?: number //authBean存储类型
    }
    //前端构建callback notify的url
    const url = `${Host}${notifyPath}/${params.appId}/${params.needUserInfo}/${params.openId}/${params.owner}`
     * */
    get("$notifyPath1/{appId}/{needUserInfo}/{owner?}") {
        //code作为换取access_token的票据，每次用户授权带上的code将不一样，
        // code只能使用一次，5分钟未被使用自动过期。
        val code = call.request.queryParameters["code"]

        //前端生成随机state，并传递给腾讯authroizeUrl，腾讯再传递回来
        val state = call.request.queryParameters["state"]

        val appId = call.parameters["appId"] ?: OfficialAccount.ApiContextMap.values.firstOrNull()?.appId
        val needUserInfo = call.parameters["needUserInfo"]?.toInt()?: NeedUserInfoType.Force_Not_Need

        //var tmp = call.parameters["openId"]
        //val guestOpenId = if(tmp == null || tmp == "null") null else tmp //前端中的缓存
        val tmp = call.parameters["owner"]
        val owner = if(tmp == null || tmp == "null") null else tmp//前端中的url中的uId，用户获取是否获取用户信息的配置

        log.info("wxOA oauth, notify1 schema=${call.request.origin.scheme}")

        val result = OAuthNotifyResult(1,"OK", appId, state)

        if (appId == null || code.isNullOrBlank() || state.isNullOrBlank()) {
            result.code = "KO"
            result.msg = "no appId or no code or no state"
        } else {
            val oauthAi = OAuthApi(appId)
            val res = oauthAi.getAccessToken(code)
            if (res.isOK()) {
                val openId = res.openId
                if(openId.isNullOrEmpty()){
                    log.warn("wxoa notify1: no openId from weixin")
                    result.code = "KO"
                    result.msg = "no openId from weixin"
                }else{
                    val isNeedEnterStep2 = when(needUserInfo){
                        NeedUserInfoType.Force_Not_Need -> false
                        NeedUserInfoType.NeedIfNo -> fanService.findGuest(openId) == null && fanService.findFan(openId) == null
                        NeedUserInfoType.NeedIfNoNameOrImg -> {
                            val guest = fanService.findGuest(openId)
                            val fan = fanService.findFan(openId)
                            (fan?.name.isNullOrEmpty() || fan?.img.isNullOrEmpty() ) && (guest?.name.isNullOrEmpty()  || guest?.img.isNullOrEmpty())
                        }
                        NeedUserInfoType.NeedByUserSettings -> (needUserInfoSettingsBlock?.let{ it(owner, openId) })?: false
                        NeedUserInfoType.ForceNeed -> {
                            log.warn("Should not come here, when ForceNeed frontend redirect to notify2")
                            true
                        }
                        else -> {
                            log.warn("Not support needUserInfoType:$needUserInfo")
                            false
                        }
                    }

                    val needEnterStep2 = if (isNeedEnterStep2) 1 else 0
                    result.code = "OK"
                    result.openId = openId
                    result.needEnterStep2 = needEnterStep2
                    //通知前端是否跳转到第二步获取userInfo
                }

            } else {
                log.warn("fail when oauthAi.getAccessToken: ${res.errMsg}")
                result.code = "KO"
                result.msg = res.errMsg
            }
        }
        //前端若是SPA，通知路径可能需要添加browserHistorySeparator: /wxoa/authNotify  or /#!/wxoa/authNotify
        //已注释掉PrefOfficialAccount中的oauthWebUrl，不再支持不同公众号拥有自定义的配置，否则此处需额外查询
        //各公众号各自的配置通知路径，也就是说 不同的公众号不同的通知路径，没必要再支持自定义，而是统一一致使用OfficialAccount中的配置
        val path = if (OfficialAccount.browserHistorySeparator.isEmpty()) notifyWebAppUrl
        else "/${OfficialAccount.browserHistorySeparator}${notifyWebAppUrl}"
        call.respondRedirect("$path?${result.serialize()}", permanent = false)
    }

    /**
     * notify2: 用户授权后得到通知回调，获取用户信息，并重定向通知给前端
     * 引入needUserInfo完全是与notify1兼容，没有其它意义
     * */
    get("$notifyPath2/{appId}") {
        val appId = call.parameters["appId"] ?: OfficialAccount.ApiContextMap.values.firstOrNull()?.appId
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        val result = OAuthNotifyResult(2,"OK", appId, state)
        if (appId == null || code.isNullOrBlank() || state.isNullOrBlank()) {
            result.code = "KO"
            result.msg = "no appId or no code or no state"
        } else {
            val oauthAi = OAuthApi(appId)
            val res:ResponseOauthAccessToken = oauthAi.getAccessToken(code)
            if (res.isOK() && res.openId != null) {
                launch {
                    if(onGetOauthAccessToken != null){
                        onGetOauthAccessToken(res, appId)
                    }else{
                        res.toOauthToken(appId)?.let { fanService.saveOauthToken(it) }
                    }
                }

                if (res.accessToken != null ) {
                    val resUserInfo = oauthAi.getUserInfo(res.accessToken!!, res.openId!!)
                    result.unionId = resUserInfo.unionId
                    if (resUserInfo.isOK()) {
                        launch{
                            if(onGetUserInfo != null){
                                onGetUserInfo(resUserInfo, appId)
                            }else{
                                resUserInfo.toGuest(appId)?.let { fanService.saveGuest(it) }
                            }
                        }
                    } else {
                        log.warn("fail getUserInfo: ${Json.encodeToString(resUserInfo)}")
                    }
                }
                result.code = "OK"
                result.openId = res.openId
            } else {
                result.code = "KO"
                result.openId = res.errMsg
            }
        }
        //notify webapp
        //前端若是SPA，通知路径可能需要添加browserHistorySeparator: /wxoa/authNotify  or /#!/wxoa/authNotify
        val path = if (OfficialAccount.browserHistorySeparator.isEmpty()) notifyWebAppUrl
        else "/${OfficialAccount.browserHistorySeparator}${notifyWebAppUrl}"
        call.respondRedirect("$path?${result.serialize()}", permanent = false)
    }
}


fun Routing.jsSdkSignature(path: String = OfficialAccount.jsSdkSignaturePath) {

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
