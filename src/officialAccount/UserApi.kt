package com.github.rwsbillyang.wxSDK.officialAccount

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.github.rwsbillyang.wxSDK.common.IBase

/**
 * 用户管理
 * TODO: 其余API
 * */
object UserApi : OABaseApi(){
    override val group: String = "user"

    /**
     * 获取用户基本信息（包括UnionID机制）
     * @param lang 国家地区语言版本，zh_CN 简体，zh_TW 繁体，en 英语，默认为zh-CN
     *
     * https://developers.weixin.qq.com/doc/offiaccount/User_Management/Get_users_basic_information_UnionID.html#UinonId
     * */
    fun getUserInfo(openId: String, lang: String = "zh_CN"): ResponseUserInfo = doGet2("info", mapOf("openid" to openId,"lang" to lang))

    /**
     * 批量获取用户基本信息, 最多支持一次拉取100条。
     * */
    fun batchGet(list: List<OpenIdLang>):ResponseUserInfoList = doPost2("batchget", mapOf("user_list" to list))
}

/**
 * @param lang 国家地区语言版本，zh_CN 简体，zh_TW 繁体，en 英语，默认为zh-CN
 * */
@Serializable
class OpenIdLang(@SerialName("openid")val openId: String, val lang: String = "zh_CN")
/**
 *
 * @param openid	用户的标识，对当前公众号唯一， 也是OAuth中获取用户信息
 * @param nickname	用户的昵称， 也是OAuth中获取用户信息
 * @param sex	用户的性别，值为1时是男性，值为2时是女性，值为0时是未知， 也是OAuth中获取用户信息
 * @param city	用户所在城市， 也是OAuth中获取用户信息
 * @param country	用户所在国家， 也是OAuth中获取用户信息
 * @param province	用户所在省份， 也是OAuth中获取用户信息
 * @param unionId unionid	只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段。， 也是OAuth中获取用户信息
 * @param headImgUrl  也是OAuth中获取用户信息。headimgurl	用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空。若用户更换头像，原有头像URL将失效。
 *
 * @param language	用户的语言，简体中文为zh_CN
 * @param subscribe	用户是否订阅该公众号标识，值为0时，代表此用户没有关注该公众号，拉取不到其余信息。
 * @param subscribeTime subscribe_time	用户关注时间，为时间戳。如果用户曾多次关注，则取最后关注时间
 * @param remark	公众号运营者对粉丝的备注，公众号运营者可在微信公众平台用户管理界面对粉丝添加备注
 * @param groupId groupid	用户所在的分组ID（兼容旧的用户分组接口）
 * @param tagIdList tagid_list	用户被打上的标签ID列表
 * @param subscribeScene subscribe_scene	返回用户关注的渠道来源，ADD_SCENE_SEARCH 公众号搜索，ADD_SCENE_ACCOUNT_MIGRATION 公众号迁移，ADD_SCENE_PROFILE_CARD 名片分享，ADD_SCENE_QR_CODE 扫描二维码，ADD_SCENE_PROFILE_LINK 图文页内名称点击，ADD_SCENE_PROFILE_ITEM 图文页右上角菜单，ADD_SCENE_PAID 支付后关注，ADD_SCENE_WECHAT_ADVERTISEMENT 微信广告，ADD_SCENE_OTHERS 其他
 * @param qrScene qr_scene	二维码扫码场景（开发者自定义）
 * @param qrSceneStr qr_scene_str	二维码扫码场景描述（开发者自定义）
 * */
@Serializable
class UserInfo(
        val subscribe: Int? = null,
        @SerialName("openId")
        val openid: String? = null,
        val nickname: String,
        val sex: Int? = null,
        val city: String? = null,
        val country: String? = null,
        val province: String? = null,
        val language: String? = null,
        @SerialName("headimgurl")
        val headImgUrl: String? = null,
        @SerialName("subscribe_time")
        val subscribeTime: String? = null,
        @SerialName("unionid")
        val unionId: String? = null,
        val remark: String? = null,
        @SerialName("groupid")
        val groupId: String? = null,
        @SerialName("tagid_list")
        val tagIdList: String? = null,
        @SerialName("subscribe_scene")
        val subscribeScene: String? = null,
        @SerialName("qr_scene")
        val qrScene: String? = null,
        @SerialName("qr_scene_str")
        val qrSceneStr: String? = null
)

@Serializable
class ResponseUserInfo(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,


        @SerialName("openId")
        val openid: String? = null,
        val nickname: String,
        val sex: Int? = null,
        val city: String? = null,
        val country: String? = null,
        val province: String? = null,
        val language: String? = null,
        @SerialName("headimgurl")
        val headImgUrl: String? = null,
        @SerialName("unionid")
        val unionId: String? = null,

        val subscribe: Int? = null,
        @SerialName("subscribe_time")
        val subscribeTime: String? = null,
        val remark: String? = null,
        @SerialName("groupid")
        val groupId: String? = null,
        @SerialName("tagid_list")
        val tagIdList: String? = null,
        @SerialName("subscribe_scene")
        val subscribeScene: String? = null,
        @SerialName("qr_scene")
        val qrScene: String? = null,
        @SerialName("qr_scene_str")
        val qrSceneStr: String? = null
): IBase

@Serializable
class ResponseUserInfoList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("user_info_list")
        val list: List<UserInfo>
): IBase