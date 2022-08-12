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
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkMulti
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkSingle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URLEncoder

/**
 * scope为:
 * snsapi_base 静默授权，可获取成员的基础信息（UserId与DeviceId）；
 * snsapi_userinfo 静默授权，可获取成员的详细信息，但不包含手机、邮箱等敏感信息
 * snsapi_privateinfo 手动授权，可获取成员的详细信息，包含手机、邮箱等敏感信息（已不再支持获取手机号/邮箱）
 * 第三方应用必须有“成员敏感信息授权”的权限:“成员敏感信息授权”的开启方法为：登录服务商管理后台->标准应用服务->本地应用->进入应用->点击基本信息栏“编辑”按钮->勾选”成员敏感信息”
 *
 * 第三方应用id（即ww或wx开头的suite_id）。注意与企业的网页授权登录不同
 * 当oauth2中appid=corpid时，scope为snsapi_userinfo或snsapi_privateinfo时，必须填agentid参数，
 * 否则系统会视为snsapi_base，不会返回敏感信息. 企业自建应用调用读取成员接口没有字段限制，可以获取包括敏感字段在内的所有信息。
 * 因此，只有第三方应用才有必要使用snsapi_userinfo或snsapi_privateinfo的scope。
 * */
enum class SnsApiScope(val value: String){
    Base("snsapi_base"), UserInfo("snsapi_userinfo"), PrivateInfo("snsapi_privateinfo")
}

/**
 * OAUTH身份认证，支持第三方应用
 * 注意：prepareOAuthInfo在企业多应用情况下可以缺少corpId，此时可以用""代替coprId，避免参数错误异常
 * */
class OAuthApi (corpId: String?, agentId: Int?, suiteId: String?)
    : WorkBaseApi(corpId, agentId, suiteId)
{
    override val group = "user"
    /**
     * 第一步：用户同意授权，获取code
     *
     * 前端webapp根据本地缓存信息，判断用户是否已有登录信息；
     * 没有的话，就发送一个请求，获取appId、state，redirect_uri等信息，自行拼接url：
     * "https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect"
     * 然后重定向到该url
     *
     * @param redirectUri 重定向地址，用于接收code（获取用户信息时使用）
     * @param snsApiScope: snsapi_base, snsapi_userinfo, snsapi_privateinfo
     * snsapi_base 静默授权，可获取成员的基础信息（UserId与DeviceId）；
     * snsapi_userinfo 静默授权，可获取成员的详细信息，但不包含手机、邮箱等敏感信息
     * snsapi_privateinfo 手动授权，可获取成员的详细信息，包含手机、邮箱等敏感信息（已不再支持获取手机号/邮箱）
     * 第三方应用必须有“成员敏感信息授权”的权限:“成员敏感信息授权”的开启方法为：登录服务商管理后台->标准应用服务->本地应用->进入应用->点击基本信息栏“编辑”按钮->勾选”成员敏感信息”
     *
     * 第三方应用id（即ww或wx开头的suite_id）。注意与企业的网页授权登录不同
     * 当oauth2中appid=corpid时，scope为snsapi_userinfo或snsapi_privateinfo时，必须填agentid参数，
     * 否则系统会视为snsapi_base，不会返回敏感信息. 企业自建应用调用读取成员接口没有字段限制，可以获取包括敏感字段在内的所有信息。
     * 因此，只有第三方应用才有必要使用snsapi_userinfo或snsapi_privateinfo的scope。
     *
     * 对于multi模式内建应用，需提供corpId和agentId
     * 对于multi模式ISV应用，需提供suiteId和corpId
     * */
    fun prepareOAuthInfo(redirectUri: String, snsApiScope: SnsApiScope = SnsApiScope.PrivateInfo): OAuthInfo {
        val state = WXBizMsgCrypt.getRandomStr()
        val appId: String
        var agentId2: Int? = null
        if(Work.isIsv){
            appId = if(Work.isMulti){
                suiteId!!
            }else{
                IsvWorkSingle.suiteId
            }
        }else{
            if(Work.isMulti){
                appId = corpId!!
                agentId2 = agentId
            }else{
                appId = WorkSingle.corpId
                agentId2 = WorkSingle.agentId
            }
        }

        return OAuthInfo(appId, URLEncoder.encode(redirectUri,"UTF-8") ,
            snsApiScope.value, state, agentId2)
    }
    /**
     * 获取访问用户身份(内建应用使用)
     *
     * 根据code获取成员信息，用于网页授权后的身份信息获取
     * 跳转的域名须完全匹配access_token对应应用的可信域名，否则会返回50001错误。
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/91023
     * */
    fun getUserInfo(code: String): ResponseOAuthUserInfo = doGet("getuserinfo", mapOf("code" to code))

    /**
     * 获取访问用户身份
     * https://work.weixin.qq.com/api/doc/90001/90143/91121
     *
     * */
    fun getUserInfo3rd(code: String): ResponseOAuthUserInfo {
        val token = if(Work.isMulti){
            IsvWorkMulti.ApiContextMap[suiteId]?.suiteAccessToken?.get()
        }else
            IsvWorkSingle.ctx.suiteAccessToken?.get()

        return get("https://qyapi.weixin.qq.com/cgi-bin/service/getuserinfo3rd?suite_access_token=$token&code=$code")
    }

    fun getUserDetail3rd(userTicket: String):ResponseOauthUserDetail3rd {
        val token = if(Work.isMulti){
            IsvWorkMulti.ApiContextMap[suiteId]?.suiteAccessToken?.get()
        }else
            IsvWorkSingle.ctx.suiteAccessToken?.get()

        return post<Unit, ResponseOauthUserDetail3rd>("https://qyapi.weixin.qq.com/cgi-bin/service/getuserdetail3rd?suite_access_token=$token&user_ticket=$userTicket")
    }
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
    @SerialName("DeviceId") //both：ISV和内部应用共有
    val deviceId: String? = null,
    @SerialName("UserId")
    val userId: String? = null, //both：ISV和内部应用共有。 用户在企业内的UserID，如果该企业与第三方应用有授权关系时，返回明文UserId，否则返回密文UserId
    @SerialName("external_userid")
    val externalUserId: String? = null, //both：ISV和内部应用共有。 外部联系人id，当且仅当用户是企业的客户，且跟进人在应用的可见范围内时返回。如果是第三方应用调用，针对同一个客户，同一个服务商不同应用获取到的id相同
    @SerialName("OpenId")
    val openId: String? = null, //both：ISV和内部应用共有。 非企业成员的标识，对当前企业唯一。不超过64字节. ISV: 非企业成员的标识，对当前服务商唯一

    @SerialName("CorpId")
    val corpId: String? = null, //ISV模式下独有
    @SerialName("user_ticket")
    val userTicket: String? = null,//ISV模式下独有 成员票据，最大为512字节。 scope为snsapi_userinfo或snsapi_privateinfo，且用户在应用可见范围之内时返回此参数。 后续利用该参数可以获取用户信息或敏感信息，参见“第三方使用user_ticket获取成员详情”。
    @SerialName("expires_in")
    val expiresIn: Int? = null,//ISV模式下独有
    @SerialName("open_userid")
    val openUserid: String? = null //ISV模式下独有 同一个内部成员对服务商唯一。最多64个字节。
): IBase

@Serializable
class ResponseOauthUserDetail3rd(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    @SerialName("corpid")
    val corpId: String? = null, //用户所属企业的corpid
    @SerialName("userid")
    val userId: String? = null,//成员UserID
    val name: String? = null, // 成员姓名，对新创建第三方应用不再返回真实name，使用userid代替name返回，第三方页面需要通过通讯录展示组件来展示名字
    val gender: Int? = null, //性别。0表示未定义，1表示男性，2表示女性
    val avatar: String? = null, //头像url。仅在用户同意snsapi_privateinfo授权时返回
    @SerialName("qr_code")
    val qrCode: String? = null //员工个人二维码（扫描可添加为外部联系人），仅在用户同意snsapi_privateinfo授权时返回
): IBase