/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-03-07 22:15
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

import com.github.rwsbillyang.wxSDK.IBase
import com.github.rwsbillyang.wxSDK.WxApi
import com.github.rwsbillyang.wxSDK.work.Work
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class ThirdOAuth(private val suiteId: String, accessTokenKey: String="access_token") : WxApi(accessTokenKey) {
    override val base = "https://qyapi.weixin.qq.com/cgi-bin"
    override val group = "service"
    override fun accessToken() = if(Work.isMulti){
        IsvWorkMulti.ApiContextMap[suiteId]?.suiteAccessToken?.get()
    }else{
        IsvWorkSingle.ctx.suiteAccessToken?.get()
    }


    /**
     * 第三方根据code获取企业成员信息
     *
     */
    fun getUserId(code: String): ResponseUserIdInfo3rd = doGet("getuserinfo3rd",
        mapOf("code" to code))

    /**
     * 第三方使用user_ticket获取成员详情
     * */
    fun getUserDetail(user_ticket: String): ResponseUserDetail3rd = doPost("getuserdetail3rd",mapOf("user_ticket" to user_ticket) )
}

/**
 * @param corpId	用户所属企业的corpid
 * @param userId	用户在企业内的UserID，如果该企业与第三方应用有授权关系时，返回明文UserId，否则返回密文UserId
 * @param deviceId	手机设备号(由企业微信在安装时随机生成，删除重装会改变，升级不受影响)
 * @param ticket	成员票据，最大为512字节。
 * @param scope 为snsapi_userinfo或snsapi_privateinfo，且用户在应用可见范围之内时返回此参数。
 * 后续利用该参数可以获取用户信息或敏感信息，参见“第三方使用user_ticket获取成员详情”。
 * @param expireIn	user_ticket的有效时间（秒），随user_ticket一起返回
 * @param openId 若用户不属于任何企业返回openId和deviceId
 * a) 当用户属于某个企业，返回示例如下：
    {
    "errcode": 0,
    "errmsg": "ok",
    "CorpId":"CORPID",
    "UserId":"USERID",
    "DeviceId":"DEVICEID",
    "user_ticket": "USER_TICKET"，
    "expires_in":7200
    }

    b) 若用户不属于任何企业，返回示例如下：
    {
    "errcode": 0,
    "errmsg": "ok",
    "OpenId":"OPENID",
    "DeviceId":"DEVICEID"
    }

    出错返回示例：
    {
    "errcode": 40029,
    "errmsg": "invalid code"
    }
 * */
@Serializable
class ResponseUserIdInfo3rd(
    override val errCode: Int? = 0,
    override val errMsg: String? = null,
    @SerialName("CorpId")
    val corpId: String? = null,
    @SerialName("UserId")
    val userId: String? = null,
    @SerialName("DeviceId")
    val deviceId: String? = null, //手机设备号(由企业微信在安装时随机生成，删除重装会改变，升级不受影响)
    @SerialName("user_ticket")
    val ticket: String? = null,
    val scope: String? = null,
    @SerialName("expires_in")
    val expireIn: Int? = null,

    @SerialName("OpenId")
    val openId: String? = null //非企业成员的标识，对当前服务商唯一
): IBase

/**
 * 第三方使用user_ticket获取成员详情
 * @param corpId	用户所属企业的corpid
 * @param userId	成员UserID
 * @param name	成员姓名
 * @param mobile	成员手机号，仅在用户同意snsapi_privateinfo授权时返回
 * @param gender	性别。0表示未定义，1表示男性，2表示女性
 * @param email	成员邮箱，仅在用户同意snsapi_privateinfo授权时返回
 * @param avatar	头像url。注：如果要获取小图将url最后的”/0”改成”/100”即可。仅在用户同意snsapi_privateinfo授权时返回
 * @param qr_code	员工个人二维码（扫描可添加为外部联系人），仅在用户同意snsapi_privateinfo授权时返回

   "corpid":"wwxxxxxxyyyyy",
"userid":"lisi",
"name":"李四",
"mobile":"15913215421",
"gender":"1",
"email":"xxx@xx.com",
"avatar":"http://shp.qpic.cn/bizmp/xxxxxxxxxxx/0",
"qr_code":"https://open.work.weixin.qq.com/wwopen/userQRCode?vcode=vcfc13b01dfs78e981c"
 * */
@Serializable
class ResponseUserDetail3rd(
    override val errCode: Int? = 0,
    override val errMsg: String? = null,

    @SerialName("corpid")
    val corpId: String? = null,
    @SerialName("userid")
    val userId: String? = null,
    val name: String? = null,
    val mobile: String? = null,
    val gender: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val qr_code: String? = null
): IBase