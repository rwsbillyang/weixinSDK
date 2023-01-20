/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:37
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

import com.github.rwsbillyang.wxSDK.IBase
import com.github.rwsbillyang.wxSDK.Response
import com.github.rwsbillyang.wxSDK.bean.OAuthInfo
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URLEncoder

/**
 * https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html
 * */
class OAuthApi(appId: String) : OABaseApi(appId){
    override val base = "https://api.weixin.qq.com/sns"
    override val group: String = "oauth2"

    /**
     * 第一步：用户同意授权，获取code
     *
     * 前端webapp根据本地缓存信息，判断用户是否已有登录信息；
     * 没有的话，就发送一个请求，获取appId、state，redirect_uri等信息，自行拼接url：
     * "https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect"
     * 然后重定向到该url
     *
     * scope为snsapi_base，snsapi_userinfo
     * */
    fun prepareOAuthInfo(redirectUri: String, needUserInfo: Boolean = false): OAuthInfo {
        val state = WXBizMsgCrypt.getRandomStr()
        return OAuthInfo(appId, URLEncoder.encode(redirectUri,"UTF-8") ,if(needUserInfo) "snsapi_userinfo" else "snsapi_base",state,null)
    }

    /**
     * 通过code换取网页授权access_token
     * 如果网页授权的作用域为snsapi_base，则本步骤中获取到网页授权access_token的同时，也获取到了openid，
     * snsapi_base式的网页授权流程即到此为止。
     *
     *  注意：此API返回的响应头包含：Content-Type: text/plain
     *  需特别处理，在install JsonFeature时添加accept(ContentType.Text.Any)
     *  否则出现NoTransformationFoundException异常：
     *  io.ktor.client.call.NoTransformationFoundException: No transformation found: class io.ktor.utils.io.ByteBufferChannel -> class com.github.rwsbillyang.wxSDK.officialAccount.ResponseOauthAccessToken
     *
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code"
     *
     * Content-Type: text/plain
     * */
    fun getAccessToken(code: String): ResponseOauthAccessToken = doGetByUrl(url("access_token", mapOf("appid" to appId, "secret" to OfficialAccount.ApiContextMap[appId]?.secret, "code" to code, "grant_type" to "authorization_code"),false))

    /**
     * 第三步：刷新access_token（如果需要）
     * https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=APPID&grant_type=refresh_token&refresh_token=REFRESH_TOKEN
     * */
    fun refreshAccessToken(refreshToken: String): ResponseOauthAccessToken = doGet("refresh_token", mapOf("appid" to appId, "secret" to OfficialAccount.ApiContextMap[appId]?.secret, "refresh_token" to refreshToken, "grant_type" to "refresh_token"))


    /**
     * @param accessToken access_token	网页授权接口调用凭证,注意：此access_token与基础支持的access_token不同
     * @param openId openid	用户的唯一标识
     * @param lang	返回国家地区语言版本，zh_CN 简体，zh_TW 繁体，en 英语
     *
     * openid	用户的唯一标识
     * nickname	用户昵称
     * sex	用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
     * province	用户个人资料填写的省份
     * city	普通用户个人资料填写的城市
     * country	国家，如中国为CN
     * headimgurl	用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空。若用户更换头像，原有头像URL将失效。
     * privilege	用户特权信息，json 数组，如微信沃卡用户为（chinaunicom）
     * unionid	只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段。
     * */
    fun getUserInfo(accessToken: String, openId: String,lang: String = "zh_CN"): ResponseUserInfo = doGetByUrl("https://api.weixin.qq.com/sns/userinfo?access_token=$accessToken&openid=$openId&lang=$lang")
    /**
     * 检验授权凭证（access_token）是否有效
     * */
    fun isValid(accessToken: String, openId: String): Response = doGet("auth", mapOf("access_token" to accessToken, "openid" to openId))
}



/**
 * @param accessToken access_token	网页授权接口调用凭证,注意：此access_token与基础支持的access_token不同
 * @param expire expires_in	access_token接口调用凭证超时时间，单位（秒）
 * @param refreshToken refresh_token	由于access_token拥有较短的有效期，当access_token超时后，可以使用refresh_token进行刷新，
 * refresh_token有效期为30天，当refresh_token失效之后，需要用户重新授权。
 * @param openId openid	用户唯一标识，请注意，在未关注公众号时，用户访问公众号的网页，也会产生一个用户和公众号唯一的OpenID
 * @param scope	用户授权的作用域，使用逗号（,）分隔
 *
 * https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html
 * */
@Serializable
class ResponseOauthAccessToken(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("access_token")
        val accessToken: String? = null,
        @SerialName("expires_in")
        val expire: Int? = null,
        @SerialName("refresh_token")
        val refreshToken: String? = null,
        @SerialName("openid")
        val openId: String? = null,
        val scope: String? = null,
        val unionid: String? = null,//用户统一标识（针对一个微信开放平台帐号下的应用，同一用户的 unionid 是唯一的），只有当 scope 为"snsapi_userinfo"时返回
        val is_snapshotuser: Int? = null //是否为快照页模式虚拟账号，只有当用户是快照页模式虚拟账号时返回，值为1
): IBase