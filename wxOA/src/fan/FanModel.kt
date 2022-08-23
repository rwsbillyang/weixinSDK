/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-10 15:19
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.wxOA.fan

import com.github.rwsbillyang.ktorKit.apiBox.Box
import com.github.rwsbillyang.ktorKit.apiBox.IUmiListParams
import com.github.rwsbillyang.wxSDK.officialAccount.ResponseOauthAccessToken
import com.github.rwsbillyang.wxSDK.officialAccount.ResponseUserInfo
import io.ktor.resources.*

import kotlinx.serialization.Serializable
import org.bson.conversions.Bson
import org.litote.kmongo.and
import org.litote.kmongo.contains
import org.litote.kmongo.eq
import org.litote.kmongo.regex


/**
 * 公众号关注者
 * @param _id openid	用户的标识，对当前公众号唯一， 也是OAuth中获取用户信息
 * @param uId unionid	只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段。， 也是OAuth中获取用户信息
 * @param name nickname	用户的昵称， 也是OAuth中获取用户信息
 * @param sex	用户的性别，值为1时是男性，值为2时是女性，值为0时是未知， 也是OAuth中获取用户信息
 * @param city	用户所在城市， 也是OAuth中获取用户信息
 * @param cty country	用户所在国家， 也是OAuth中获取用户信息
 * @param pro province	用户所在省份， 也是OAuth中获取用户信息
 *
 * @param img headImgUrl  也是OAuth中获取用户信息。headimgurl	用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空。若用户更换头像，原有头像URL将失效。
 * @param lang language	用户的语言，简体中文为zh_CN
 * @param sub subscribe	用户是否订阅该公众号标识，1:关注， 值为0时，代表此用户没有关注该公众号，拉取不到其余信息。
 * @param st subscribeTime subscribe_time	用户关注时间，为时间戳。如果用户曾多次关注，则取最后关注时间
 * @param re	公众号运营者对粉丝的备注，公众号运营者可在微信公众平台用户管理界面对粉丝添加备注
 * @param gId groupId groupid	用户所在的分组ID（兼容旧的用户分组接口）
 * @param tags tagIdList tagid_list	用户被打上的标签ID列表
 * @param ss subscribe_scene	返回用户关注的渠道来源，ADD_SCENE_SEARCH 公众号搜索，ADD_SCENE_ACCOUNT_MIGRATION 公众号迁移，ADD_SCENE_PROFILE_CARD 名片分享，ADD_SCENE_QR_CODE 扫描二维码，ADD_SCENE_PROFILE_LINK 图文页内名称点击，ADD_SCENE_PROFILE_ITEM 图文页右上角菜单，ADD_SCENE_PAID 支付后关注，ADD_SCENE_WECHAT_ADVERTISEMENT 微信广告，ADD_SCENE_OTHERS 其他
 * @param qr qr_scene	二维码扫码场景（开发者自定义）
 * @param qrs qr_scene_str	二维码扫码场景描述（开发者自定义）
 * */
@Serializable
data class Fan(
        val appId: String? = null,//为null，表示向前兼容没该字段的老系统
        val _id: String, //openId
        val uId: String? = null, //unionid
        val name: String? = null,
        val sex: Int? = null,
        val city: String? = null,
        val cty: String? = null,
        val pro: String? = null,
        val lang: String? = null,
        val img: String? = null,
        val sub: Int? = null,
        val st: Long? = null,
        val re: String? = null,
        val gId: Int? = null,
        val tags: List<Int>? = null,
        val ss: String? = null,
        val qr: Int? = null,
        val qrs: String? = null, // first time subscribe
        val t: Long = System.currentTimeMillis()
)

fun ResponseUserInfo.toFan(appId: String) = if(!openid.isNullOrBlank()) Fan(appId, openid!!, unionId, nickname, sex, city,country, province, language,
    headImgUrl?.replace("http:","https:"),//https适用于http和https网址，而http在https下无法打开，统一处理避免前端复杂化
        subscribe, subscribeTime, remark, groupId, tagIdList, subscribeScene, qrScene, qrSceneStr) else null

/**
 * 用户同意授权后获取的信息
 * */
@Serializable
data class Guest(
    val appId: String? = null,//为null，表示向前兼容没该字段的老系统
    val _id: String, //openId
    val uId: String? = null, //unionid
    val name: String? = null,
    val sex: Int? = null,
    val city: String? = null,
    val cty: String? = null,
    val pro: String? = null,
    val lang: String? = null,
    val img: String? = null,
    val t: Long = System.currentTimeMillis(),

)
fun ResponseUserInfo.toGuest(appId: String) = if(isOK() && openid != null) Guest(appId, openid!!, unionId, nickname, sex, city,country, province, language,
    headImgUrl?.replace("http:","https:"),//https适用于http和https网址，而http在https下无法打开，统一处理避免前端复杂化
     ) else null


/**
 * 返回的列表数据
 * */
@Serializable
class FanListBox(
        val total: Long,
        val data: List<Fan>? = null
) : Box()

@Serializable
@Resource("/list")
data class FanListParams(
        override val umi: String? = null ,
        val _id: String? = null,//openid
        val uId: String? = null, //unionid
        val name: String? = null,
        val sex: Int? = null,
        val city: String? = null,
        val cty: String? = null,
        val pro: String? = null,
        val lang: String? = null,
        val tags: Int? = null,
        val ss: String? = null,
        val qr: Int? = null,
        val qrs: String? = null,
        val appId: String? = null

) : IUmiListParams {
    fun toFilter(): Bson {
        val idFilter = _id?.let { Fan::_id eq it }
        val uidFilter = uId?.let { Fan::uId eq it }
        val nameFilter = name?.let { Fan::name regex ".*$it.*" }
        val sexFilter = sex?.let { Fan::sex eq it }
        val cityFilter = city?.let { Fan::city eq it }
        val ctyFilter = cty?.let { Fan::cty eq it }
        val proFilter = pro?.let { Fan::pro eq it }
        val langFilter = lang?.let { Fan::lang eq it }
        val tagsFilter = tags?.let { Fan::tags contains  it }
        val ssFilter = ss?.let { Fan::ss eq it }
        val qrFilter = qr?.let { Fan::qr eq it }
        val qrsFilter = qrs?.let { Fan::qrs eq it }
        val appIdFilter = appId?.let{Fan::appId eq appId}
        return and(idFilter,uidFilter, nameFilter, sexFilter,cityFilter,ctyFilter,proFilter,langFilter,tagsFilter,ssFilter,qrFilter,qrsFilter,appIdFilter)
    }
}

/**
 * TODO: 根据OauthToken更新用户信息
 * @param _id openId openid	用户唯一标识，请注意，在未关注公众号时，用户访问公众号的网页，也会产生一个用户和公众号唯一的OpenID
 * @param aToken accessToken access_token	网页授权接口调用凭证,注意：此access_token与基础支持的access_token不同
 * @param expire expires_in	access_token接口调用凭证超时时间，单位（秒）
 * @param rToken refreshToken refresh_token	由于access_token拥有较短的有效期，当access_token超时后，可以使用refresh_token进行刷新，
 * refresh_token有效期为30天，当refresh_token失效之后，需要用户重新授权。
 * @param scope	用户授权的作用域，使用逗号（,）分隔
 * */
@Serializable
data class OauthToken(
        val _id: String,
        val aToken: String? = null,
        val expire: Int? = null,
        val rToken: String? = null,
        val scope: String? = null,
        val time: Long = System.currentTimeMillis()
)

fun ResponseOauthAccessToken.toOauthToken() = if(isOK() && openId != null) OauthToken(openId!!, accessToken, expire, refreshToken, scope) else null
