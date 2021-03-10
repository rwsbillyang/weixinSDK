/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-03-07 20:42
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

package com.github.rwsbillyang.wxSDK.work.third


import com.github.rwsbillyang.wxSDK.IBase
import com.github.rwsbillyang.wxSDK.Response
import kotlinx.serialization.Serializable

class ThirdPartyApi(suiteId: String) : ThirdOAuth(suiteId, "suite_access_token") {
    override val base = "https://qyapi.weixin.qq.com/cgi-bin"
    override val group = "service"


    /**
     * 获取预授权码
     * */
    fun getPreAuthCode():PreAuthCode = doGet("get_pre_auth_code")

    /**
     * 设置授权配置
     * @param pre_auth_code 来自getPreAuthCode
     * @param auth_type 授权类型：0 正式授权， 1 测试授权。 默认值为0。注意，请确保应用在正式发布后的授权类型为“正式授权”
     * */
    fun setSessionInfo(pre_auth_code: String, auth_type: Int): Response = doPost("set_session_info",
        mapOf("pre_auth_code" to pre_auth_code, "session_info" to mapOf("auth_type" to auth_type)))

    /**
     * 获取企业永久授权码
     * 该API用于使用临时授权码换取授权方的永久授权码，并换取授权信息、企业access_token，临时授权码一次有效。建议第三方以userid为主键，来建立自己的管理员账号。
     * @param auth_code 临时授权码，会在授权成功时附加在redirect_uri中跳转回第三方服务商网站，或通过回调推送给服务商。长度为64至512个字节
     * */
    fun getPermanentCode(auth_code: String):ResponsePermanentCodeInfo = doPost("get_permanent_code",
        mapOf("auth_code" to auth_code))

    /**
     * 获取企业授权信息
     * 该API用于通过永久授权码换取企业微信的授权信息。 永久code的获取，是通过临时授权码使用get_permanent_code 接口获取到的
     * @param auth_corpid 授权方corpid
     * @param permanent_code 永久授权码，通过get_permanent_code获取
     * */
    fun getAuthInfo(auth_corpid: String, permanent_code: String): ResponsePermanentCodeInfo = doPost("get_auth_info",
        mapOf("auth_corpid" to auth_corpid, "permanent_code" to permanent_code))

    /**
     * 获取企业access_token
     *
     * 第三方服务商在取得企业的永久授权码后，通过此接口可以获取到企业的access_token。
     *
     * 获取后可通过通讯录、应用、消息等企业接口来运营这些应用。
     * 此处获得的企业access_token与企业获取access_token拿到的token，本质上是一样的，只不过获取方式不同。
     * 获取之后，就跟普通企业一样使用token调用API接口
     *
     * 应用授权的token
     * 企业在授权应用时，第三方需要以suite_id（第三方应用ID）、suite_secret（第三方应用密钥）
     * （获取方法为：登录服务商管理后台->标准应用服务->应用管理栏，点进某个应用即可看到）换取suite_access_token，
     * 再以suite_access_token访问应用授权的接口。在最终访问授权企业的接口时，再将suite_access_token换为企业
     * 的access_token。
     * */
    //fun getCorpToken(authCorpid: String, permanentCode: String) = doPost()


    /**
     * 获取应用的管理员列表
     * 第三方服务商可以用此接口获取授权企业中某个第三方应用的管理员列表(不包括外部管理员)，
     * 以便服务商在用户进入应用主页之后根据是否管理员身份做权限的区分。该应用必须与SUITE_ACCESS_TOKEN对
     * 应的suiteid对应，否则没权限查看
     * */
    fun getAdminList(corpId: String, agentId: Int):ResponseAdminList = doPost("get_admin_list",
        mapOf("auth_corpid" to corpId, "agentid" to agentId))

}

class PreAuthCode(
    override val errCode: Int? = 0,
    override val errMsg: String? = null,
    val pre_auth_code: String? = null,
    val expires_in: String? = null
): IBase

/**
 * @param corpid	授权方企业微信id
 * @param corp_name	授权方企业微信名称
 * @param corp_type	授权方企业微信类型，认证号：verified, 注册号：unverified
 * @param corp_square_logo_url	授权方企业微信方形头像
 * @param corp_user_max	授权方企业微信用户规模
 * @param corp_full_name	所绑定的企业微信主体名称(仅认证过的企业有)
 * @param subject_type	企业类型，1. 企业; 2. 政府以及事业单位; 3. 其他组织, 4.团队号
 * @param verified_end_time	认证到期时间
 * @param corp_wxqrcode	授权企业在微工作台（原企业号）的二维码，可用于关注微工作台
 * @param corp_scale	企业规模。当企业未设置该属性时，值为空
 * @param corp_industry	企业所属行业。当企业未设置该属性时，值为空
 * @param corp_sub_industry	企业所属子行业。当企业未设置该属性时，值为空
 * @param location	企业所在地信息, 为空时表示未知

"corpid": "xxxx",
"corp_name": "name",
"corp_type": "verified",
"corp_square_logo_url": "yyyyy",
"corp_user_max": 50,
"corp_agent_max": 30,
"corp_full_name":"full_name",
"verified_end_time":1431775834,
"subject_type": 1,
"corp_wxqrcode": "zzzzz",
"corp_scale": "1-50人",
"corp_industry": "IT服务",
"corp_sub_industry": "计算机软件/硬件/信息服务",
"location":"广东省广州市"
 * */
@Serializable
class AuthCorpInfo(
    val corpid: String,
    val corp_name: String,
    val corp_type: String,
    val corp_square_logo_url: String,
    val corp_user_max: Int,
    val corp_agent_max: Int,
    val corp_full_name: String? = null,
    val verified_end_time: Long? = null,
    val subject_type: Int,
    val corp_wxqrcode: String? = null,
    val corp_scale: String? = null,
    val corp_industry: String? = null,
    val corp_sub_industry: String? = null,
    val location: String? = null
)

/**
 * 授权的应用信息，注意是一个数组，但仅旧的多应用套件授权时会返回多个agent，对新的单应用授权，永远只返回一个agent
 * @param agentid	授权方应用id
 * @param name	授权方应用名字
 * @param square_logo_url	授权方应用方形头像
 * @param round_logo_url	授权方应用圆形头像
 * @param appid	旧的多应用套件中的对应应用id，新开发者请忽略
 * @param privilege	应用对应的权限

"agentid":1,
"name":"NAME",
"round_logo_url":"xxxxxx",
"square_logo_url":"yyyyyy",
"appid":1,
"privilege":
{
"level":1,
"allow_party":[1,2,3],
"allow_user":["zhansan","lisi"],
"allow_tag":[1,2,3],
"extra_party":[4,5,6],
"extra_user":["wangwu"],
"extra_tag":[4,5,6]
}
 * */
@Serializable
class AgentInfo(
    val agentid: Int,
    val name: String,
    val round_logo_url:  String? = null,
    val square_logo_url:  String? = null,
    val privilege: AgentPrivilege? = null,
)
/**
 * 应用对应的权限
 *  @param level	权限等级。
 *  1:通讯录基本信息只读
 *  2:通讯录全部信息只读
 *  3:通讯录全部信息读写
 *  4:单个基本信息只读
 *  5:通讯录全部信息只写
 *
 * @param allow_party	应用可见范围（部门）
 * @param allow_tag	应用可见范围（标签）
 * @param allow_user	应用可见范围（成员）
 * @param extra_party	额外通讯录（部门）
 * @param extra_user	额外通讯录（成员）
 * @param extra_tag	额外通讯录（标签）
 * */
@Serializable
class AgentPrivilege(
    val level: Int,
    val allow_party: List<Int>? = null,
    val allow_user: List<String>? = null,
    val allow_tag: List<Int>? = null,
    val extra_party: List<Int>? = null,
    val extra_user:  List<String>? = null,
    val extra_tag: List<Int>? = null
)

/**
 * 授权管理员的信息
 * @param userid	授权管理员的userid，可能为空（内部管理员一定有，不可更改）
 * @param name	授权管理员的name，可能为空（内部管理员一定有，不可更改）
 * @param avatar	授权管理员的头像url
 * */
@Serializable
class AuthUserInfo(
    val userid: String? = null,
    val name: String? = null,
    val avatar: String? = null
)
@Serializable
class AuthInfo(
    val agent: List<AgentInfo>? = null,
    val auth_user_info: AuthUserInfo? = null
)

/**
 * @param access_token	授权方（企业）access_token,最长为512字节
 * @param expires_in	授权方（企业）access_token超时时间
 * @param auth_corp_info	授权方企业信息
 * @param permanent_code	企业微信永久授权码,最长为512字节
 * @param auth_info 授权信息。如果是通讯录应用，且没开启实体应用，是没有该项的。通讯录应用拥有企业通讯录的全部信息读写权限
 * */
@Serializable
class ResponsePermanentCodeInfo(
    override val errCode: Int? = 0,
    override val errMsg: String? = null,
    val access_token: String? = null,
    val expires_in: Int = 0,
    val permanent_code: String? = null,
    val auth_corp_info: AuthCorpInfo? = null,
    val auth_info : AuthInfo? = null
): IBase


/**
 * 管理员列表中的成员
 * @param userid	管理员的userid
 * @param auth_type	该管理员对应用的权限：0=发消息权限，1=管理权限
 * */
@Serializable
class AuthUser(
    val userid: String,
    val auth_type: Int
)

@Serializable
class ResponseAdminList(
    override val errCode: Int? = 0,
    override val errMsg: String? = null,
    val admin: List<AuthUser>? = null
): IBase