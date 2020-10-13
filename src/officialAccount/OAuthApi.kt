package com.github.rwsbillyang.wxSDK.officialAccount


import com.github.rwsbillyang.wxSDK.common.IBase
import com.github.rwsbillyang.wxSDK.common.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomStringUtils
import java.net.URLEncoder

/**
 * https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html
 * */
object OAuthApi: OABaseApi() {
    override val base = "https://api.weixin.qq.com/sns"
    override val group: String = "oauth2"

    /**
     * 第一步：用户同意授权，获取code
     *
     * 前端webapp根据本地缓存信息，判断用户是否已有登录信息；
     * 没有的话，就发送一个请求，获取appId、state，redirect_uri等信息，自行拼接url：
     * "https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirec"
     * 然后重定向到该url
     *
     * scope为snsapi_base，snsapi_userinfo
     * */
    fun prepareOAuthInfo(redirectUri: String, needUserInfo: Boolean = false):OAuthInfo {
        val state = RandomStringUtils.randomAlphanumeric(16)
        return OAuthInfo(OfficialAccount._OA.appId, URLEncoder.encode(redirectUri,"UTF-8") ,if(needUserInfo) "snsapi_userinfo" else "snsapi_base",state)
    }

    /**
     * 通过code换取网页授权access_token
     * 如果网页授权的作用域为snsapi_base，则本步骤中获取到网页授权access_token的同时，也获取到了openid，
     * snsapi_base式的网页授权流程即到此为止。
     *
     * "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code"
     * */
    fun getAccessToken(code: String):ResponseOauthAccessToken = doGet2("access_token", mapOf("appid" to OfficialAccount._OA.appId, "secret" to OfficialAccount._OA.secret, "code" to code, "grant_type" to "authorization_code"))

    /**
     * 第三步：刷新access_token（如果需要）
     * https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=APPID&grant_type=refresh_token&refresh_token=REFRESH_TOKEN
     * */
    fun refreshAccessToken(refreshToken: String):ResponseOauthAccessToken = doGet2("refresh_token", mapOf("appid" to OfficialAccount._OA.appId, "secret" to OfficialAccount._OA.secret, "refresh_token" to refreshToken, "grant_type" to "refresh_token"))


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
    fun getUserInfo(accessToken: String, openId: String,lang: String = "zh_CN"):ResponseUserInfo = doGet2{
        "https://api.weixin.qq.com/sns/userinfo?access_token=$accessToken&openid=$openId&lang=$lang"
    }

    /**
     * 检验授权凭证（access_token）是否有效
     * */
    fun isValid(accessToken: String, openId: String): Response = doGet2("auth", mapOf("access_token" to accessToken, "openid" to openId))
}

/**
 * @param appId	appid 是	公众号的唯一标识
 * @param redirectUri	redirect_uri 是	授权后重定向的回调链接地址， 使用 urlEncode 链接进行处理过
 * @param scope	是	应用授权作用域，snsapi_base （不弹出授权页面，直接跳转，只能获取用户openid），snsapi_userinfo （弹出授权页面，可通过openid拿到昵称、性别、所在地。并且， 即使在未关注的情况下，只要用户授权，也能获取其信息 ）
 * @param state	否	重定向后会带上state参数，开发者可以填写a-zA-Z0-9的参数值，最多128字节
 * */
@Serializable
class OAuthInfo(
        val appId: String,
        val redirectUri: String,
        val scope: String,
        val state: String
)

/**
 * @param accessToken access_token	网页授权接口调用凭证,注意：此access_token与基础支持的access_token不同
 * @param expire expires_in	access_token接口调用凭证超时时间，单位（秒）
 * @param refreshToken refresh_token	由于access_token拥有较短的有效期，当access_token超时后，可以使用refresh_token进行刷新，
 * refresh_token有效期为30天，当refresh_token失效之后，需要用户重新授权。
 * @param openId openid	用户唯一标识，请注意，在未关注公众号时，用户访问公众号的网页，也会产生一个用户和公众号唯一的OpenID
 * @param scope	用户授权的作用域，使用逗号（,）分隔
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
        val scope: String? = null
): IBase