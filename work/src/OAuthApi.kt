/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-27 11:42
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

import com.github.rwsbillyang.wxSDK.IBase
import com.github.rwsbillyang.wxSDK.bean.OAuthInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomStringUtils
import java.net.URLEncoder

class OAuthApi private constructor (corpId: String) : WorkBaseApi(corpId){
    /**
     * ISV模式，suiteId为null表示single单应用模式
     * */
    constructor(suiteId: String?, corpId: String) : this(corpId) {
        this.suiteId = suiteId
    }

    /**
     * 企业内部应用模式，空参表示single单应用模式
     * */
    constructor(corpId: String, agentId: Int? = null) : this(corpId) {
        this.agentId = agentId
    }

    override val group = "user"
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
        val state = RandomStringUtils.randomAlphanumeric(16)
        return OAuthInfo(corpId!!, URLEncoder.encode(redirectUri,"UTF-8") ,
            "snsapi_base",state,needUserInfo)
    }
    /**
     * 获取访问用户身份
     *
     * 根据code获取成员信息，用于网页授权后的身份信息获取
     * 跳转的域名须完全匹配access_token对应应用的可信域名，否则会返回50001错误。
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/91023
     * */
    fun getUserInfo(code: String): ResponseOAuthUserInfo = doGet("getuserinfo", mapOf("code" to code))

}

/**
 * 当用户为企业成员时（无论是否在应用可见范围之内）返回示例如下：
 * UserId	成员UserID。若需要获得用户详情信息，可调用通讯录接口：读取成员。如果是互联企业，则返回的UserId格式如：CorpId/userid
 * DeviceId	手机设备号(由企业微信在安装时随机生成，删除重装会改变，升级不受影响)
 *
 * 非企业成员时，返回示例如下：
 * OpenId	非企业成员的标识，对当前企业唯一。不超过64字节
 * DeviceId	手机设备号(由企业微信在安装时随机生成，删除重装会改变，升级不受影响)
 * external_userid	外部联系人id，当且仅当用户是企业的客户，且跟进人在应用的可见范围内时返回。如果是第三方应用调用，针对同一个客户，同一个服务商不同应用获取到的id相同
 * */
@Serializable
class ResponseOAuthUserInfo(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    @SerialName("DeviceId")
    val deviceId: String? = null,
    @SerialName("UserId")
    val userId: String? = null,
    @SerialName("external_userid")
    val externalUserId: String? = null,
    @SerialName("OpenId")
    val openId: String? = null
): IBase