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

package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.rwsbillyang.wxSDK.aes.SignUtil
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.async
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


class OfficialAccountFeature {
    // Body of the feature
//    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
//        //  context.call.response.header(name, value)
//    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, OAConfiguration, OfficialAccountFeature> {
        // Creates a unique key for the feature.
        override val key = AttributeKey<OfficialAccountFeature>("OfficialAccountFeature")

        // Code to execute when installing the feature.
        override fun install(pipeline: ApplicationCallPipeline, configure: OAConfiguration.() -> Unit): OfficialAccountFeature {
            OfficialAccount.config(configure)
            
            // 首先创建一个Configuration，然后对其apply：执行install的configure代码块，
            //val configuration = OAConfiguration().apply(configure)

            // Install an interceptor that will be run on each call and call feature instance
//            pipeline.intercept(ApplicationCallPipeline.Setup) {
//                feature.intercept(this)
//            }

            return OfficialAccountFeature()
        }
    }
}



fun Routing.officialAccountApi(path: String = OfficialAccount.wxEntryPoint) {
    val log = LoggerFactory.getLogger("officialAccountApi")

    route(path) {
        /**
         * 公众号后台中的开发者->配置开发服务器URL，必须调用此函数进行处理GET请求
         *
         * https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
         * https://developers.weixin.qq.com/doc/offiaccount/Getting_Started/Getting_Started_Guide.html
         *
         * 第二步：验证消息的确来自微信服务器
         * 开发者提交信息后，微信服务器将发送GET请求到填写的服务器地址URL上，GET请求携带参数如下表所示：
         *
         * signature	微信加密签名，signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
         * timestamp	时间戳
         * nonce	随机数
         * echostr	随机字符串
         *
         * 开发者通过检验signature对请求进行校验（下面有校验方式）。若确认此次GET请求来自微信服务器，请原样返回echostr参
         * 数内容，则接入生效，成为开发者成功，否则接入失败。加密/校验流程如下：
         * 1）将token、timestamp、nonce三个参数进行字典序排序
         * 2）将三个参数字符串拼接成一个字符串进行sha1加密
         * 3）开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
         * */
        get {
            val signature = call.request.queryParameters["signature"]
            val timestamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val echostr = call.request.queryParameters["echostr"]

            val token = OfficialAccount.OA.token

            if (StringUtils.isAnyBlank(token, signature, timestamp, nonce,echostr)) {
                log.warn("invalid parameters: token=$token, signature=$signature, timestamp=$timestamp, nonce=$nonce,echostr=$echostr")
            } else {
                if (!SignUtil.checkSignature(token, signature!!, timestamp!!, nonce!!)) {
                    log.warn("fail to check signature")
                }
            }
            call.respondText(echostr?:"", ContentType.Text.Plain, HttpStatusCode.OK)
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
        post {
            val body: String = call.receiveText()

            val msgSignature = call.request.queryParameters["msg_signature"]
            val timeStamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val encryptType = call.request.queryParameters["encrypt_type"]?:"aes"

            val reXml = OfficialAccount.OA.msgHub.handleXmlMsg(body, msgSignature, timeStamp, nonce, encryptType)

            if(reXml.isNullOrBlank())
                call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
            else
                call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)

        }
    }
}

fun Routing.oAuthApi(
        oauthInfoPath: String = OfficialAccount.oauthInfoPath,
        notifyPath: String = OfficialAccount.notifyPath,
        notifyWebAppUrl: String = OfficialAccount.notifyWebAppUrl,
        needUserInfo: ((String, String) -> Int)? = null,
        onGetOauthAccessToken: ((ResponseOauthAccessToken)-> Unit)? = null,
        onGetUserInfo: ((info: ResponseUserInfo) -> Unit)? = null
) {
    val log = LoggerFactory.getLogger("oAuthApi")
    val stateCache = Caffeine.newBuilder()
            .maximumSize(Long.MAX_VALUE)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(0, TimeUnit.SECONDS)
            .build<String, Boolean>()
    /**
     * 前端webapp请求该api获取appid，state等信息，然后重定向到腾讯的授权页面，用户授权之后将重定向到下面的notify
     * @param userInfo 0或1分别表示是否需要获取用户信息，优先使用前端提供的参数, 没有提供的话使用needUserInfo(host, uri)进行判断（用于从某个用户设置中获取），再没有的话则默认为0
     * @param host 跳转host，如："https：//www.example.com"
     * */
    get(oauthInfoPath){
        val userInfo = (call.request.queryParameters["userInfo"]?.toInt()?:(needUserInfo?.let { it(call.request.host(), call.request.uri) })?:0) == 1
        val host = call.request.queryParameters["host"]?:call.request.host()

        val oAuthInfo = OAuthApi.prepareOAuthInfo(host + notifyPath, userInfo)
        stateCache.put(oAuthInfo.state, userInfo)
        call.respond(oAuthInfo)
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
    get(notifyPath){
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        if(code.isNullOrBlank() || state.isNullOrBlank()){
            //notify webapp fail
            call.respondRedirect("$notifyWebAppUrl?state=$state&code=KO&msg=nullCodeOrState", permanent = false)
        }else{
            val res = OAuthApi.getAccessToken(code)
            if(res.isOK()  && res.openId != null){

                onGetOauthAccessToken?.let { async { it.invoke(res) } }

                var url = "$notifyWebAppUrl?state=$state&code=OK&openId=${res.openId}"
                val needUserInfo = stateCache.getIfPresent(state)?:false
                stateCache.invalidate(state)
                if(needUserInfo && res.accessToken != null){
                    val resUserInfo = OAuthApi.getUserInfo(res.accessToken, res.openId)
                    if(resUserInfo.isOK())
                    {
                        if(resUserInfo.unionId != null){
                            url += "&unionId=${resUserInfo.unionId}"
                        }
                        //async save fan user info
                        onGetUserInfo?.let { async { it.invoke(resUserInfo)} }
                    }else{
                        log.warn("fail getUserInfo: $resUserInfo")
                    }
                }
                //notify webapp OK
                call.respondRedirect(url, permanent = false)
            }
        }
    }
}


