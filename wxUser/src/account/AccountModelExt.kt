/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-30 19:47
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


@file:UseContextualSerialization(ObjectId::class)
package com.github.rwsbillyang.wxUser.account

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId
import java.time.LocalDateTime


/**
 * login时的参数信息
 * @param name
 * @param pwd
 * @param type
 * 为account时，name和pwd分别表示用户名和密码，
 * 为mobile时，表示电话和验证码，
 * 为wechat时，表示openId和unionId
 * */
@Serializable
class LoginParamBean(
    val name: String,
    val pwd: String? = null,
    val type: String? = ACCOUNT)
{
    companion object {
        const val ACCOUNT = "account"
        const val MOBILE = "mobile"
        const val WECHAT = "wechat"
        const val WECHAT_SCANQRCODE = "wechat_scan"
        const val WXWORK = "wxWork"
        const val WXWORK_SCAN = "wxWork_scan"
        const val WXWORK_SUITE = "wxWork_isv"
        const val SCAN_QRCODE = "scanQrcode"
        const val WXMINI = "wxMini"
    }
}

@Serializable
class Owner(
    val appId: String?, //wxoa appID, wxwork corpId, siteId
    val aId: ObjectId?, //app account Id
    val sysId: ObjectId? = null, //global system account
)
/**
 * 配置url时的键值
 * */
enum class PayNotifierType{
    ExpireAlarm, PaySuccess, BonusNotify
}

/**
 * 用户等级，与前端需保持一致
 * */
object EditionLevel {
    const val Free = 0
    const val VIP = 10
    const val DiamondVIP = 20
    fun level2Name(level: Int?) = when (level) {
        EditionLevel.Free -> "免费会员"
        EditionLevel.VIP -> "VIP"
        EditionLevel.DiamondVIP -> "SVIP"
        else -> "免费会员"
    }
}




@Serializable
data class NoticeIconData(
    val id: String?,
    val key: String?,
    val avatar: String?,
    val title: String?,
    val datetime: String?,
    val type: String,
    val read: Boolean?,
    val description: String,
    val clickClose: Boolean?,
    val extra: String?,
    val status: String
)

/**
 * 用于支持企业微信中多agent共享一个account账户，每个agent可以有自己的单独权限过期信息。
 * 只有付费用户才需创建一条记录，免费用户无需创建记录
 *
 * 像这样把expire信息隔离开来，好处是一个企业各agent可共享同一个账号，各agent可以有不同的权限信息。
 * 坏处是不能支持公众号应用和企业应用共享同一个账号信息（或者说增加了麻烦）
 *
 * @param expire 过期时间,为空时，表示无限期
 * @param level 用户等级
 * @param role 角色
 * */
//@Serializable
//data class AccountExpire(
//    val _id: String, // 公众号：account._id.to64String， 企业微信：account._id.to64String/agentId
//    //权限信息
//    val expire: Long? = null, //为空时，表示无限期
//    val role: List<String>? = null, //'user' | 'guest' | 'admin';
//    val level: Int = EditionLevel.Free
//    //val agentId: Int? = null //企业微信账户才有值
//) {
//    companion object {
//        fun id(uId: String, agentId: Int?) = if (agentId != null) "$uId/$agentId" else uId
//    }
//
//    fun toExpireInfo() = ExpireInfo(expire, role, level)
//}

/**
 * 标识了一个企业微信用户：内部成员、外部客户，其它未知访客、非可见内部成员。
 * oAuth认证时4种类型人，有3种结果形式：
 * 1.可见范围内的企业内部成员，只有userId，无openID。
 * 2.外部客户，有externalId和openId
 * 3.其它外部成员或非可见内部成员，只有openId
 *
 * 唯一ID标识号为："$corpId-$openId"，称之为mockOpenId或openId，因此必须得到所有访客的openId
 *
 * （1）系统初始化时获取agent信息时，将获取所有可见成员contact detail以及其openId。
 *  注意：若从未登录过，oauth认证提交上来的VID将无openId，此时需要根据userId补充openId信息
 *
 * （2）可见成员登录时，获取其个人用户信息和其外部客户信息（必须设置为"客户联系"可调用应用），[ 可选 ]获取detail信息以及openId
 * （3）其它外部客户，将无头像和昵称，只有openId
 * （4）不可见内部客户，只有openId，可尝试对所有只有openId的转化成内部userId，若成功将获取contact detail，目的在于获取头像信息
 * */
//@Serializable
//class VID(
//    val uId: String? = null, // Account._id
//    val userId: String? = null, //企业成员userId，与corpId配对使用，否则可能不唯一
//    var openId: String? = null,//企业userId会改变，优先使用openId查找 未登录的可见范围内部成员访问他人信息时将只有userId，无openId，需要查修改添加上
//    val externalId: String? = null,
//    val corpId: String? = null
//) {
//    override fun equals(other: Any?): Boolean {
//        if (other == null) return false
//        if (other is VID) {
//            if (uId != null && other.uId != null && uId == other.uId)
//                return true
//
//            if (other.corpId == null || corpId == null || other.corpId != corpId)
//                return false
//
//            if (openId != null && other.openId != null && openId == other.openId)
//                return true
//
//            if (userId != null && other.userId != null && userId == other.userId)
//                return true
//
//            if (externalId != null && other.externalId != null && externalId == other.externalId)
//                return true
//        }
//
//        return false
//    }

/**
 * 内嵌文档在group, aggregate时比较复杂， 从而统一通过openId唯一标识一个访客，与corpId组合后应该不会重复
 * @return 返回null时表示无效id，意味着不能标识出一个访客
 *
 * 内部成员登录，oatuh认证腾讯没有返回openID，但通过同步Contact详情时同时将userId转化成openId，从而contact中保存了openId
 * 外部客户，oauth认证会同时返回openId，另同步详情时也转换了openId，因此有openId
 * 其它外部人，oAuth认证只有openId，无任何详细信息
 * 内部成员但对应用不可见，似乎与外部人相同，oAuth认证只有openId
 *
 * 因此，通过openID，应该可以唯一标识出所有的企业微信访问
 * */
//    fun toOpenId(): String? {
//        if (corpId == null) return null
//        if (openId != null) return openId //"$corpId-$openId"
//        if (uId != null) return uId
//        if (userId != null) return userId //"$corpId-$userId"不能用斜杠，因可能作为请求路径中的参数
//        if (externalId != null) return externalId //"$corpId-$externalId"
//        return null
//    }
//}

//fun ApplicationCall.toVID() =
//    if (corpId == null || (userId == null && externalUserId == null && oId == null)) null else VID(
//        uId,
//        userId,
//        oId,
//        externalUserId,
//        corpId
//    )


/**
 *
 * 公众号或企业微信注册时提供的信息
 * */
//@Serializable
//class GuestOAuthBean(
//    val appId: String? = null,// 公众号
//    val unionId: String? = null,// 公众号
//    val openId1: String? = null, // 公众号访客openId
//    //val hasUserInfo: Boolean ? = null,
//
//    val openId2: String? = null, // 企业微信访客openId
//    //企业微信
//    val userId: String? = null, //企业微信内部员工userid
//    //val externalUserId: String? = null,//企业微信外部成员id
//    val corpId: String? = null,
//    val agentId: Int? = null,
//    val suiteId: String? = null,
//    val deviceId: String? = null,
//
//    val rcm: String? = null //推荐者uId
//)

/**
 * login后返回的信息
 * */
//@Serializable
//class AuthBean(
//    val uId: String,
//    val token: String,
//    val level: Int, // 当前edition level 可能已过期
//    val expire: Long? = null,
//    val role: List<String>? = null, //若为空，自动赋值 "user"
//    val gId:List<String>? = null, //所属群组
//
//    val unionId: String? = null,
//    val openId1: String? = null, // 公众号 用于一个运行中的程序同时支持公众号和企业微信。 返回给前端，只在访客click/relay时用于标识clicker和relayer
//    val openId2: String? = null, // 企业微信 用于一个运行中的程序同时支持公众号和企业微信。 返回给前端，只在访客click/relay时用于标识clicker和relayer
//    val appId: String? = null,
//    val miniId: String? = null,
//
//    //企业微信
//    val userId: String? = null,
//    val externalUserId: String? = null,
//    val corpId: String? = null,
//    val agentId: Int? = null,
//    val suiteId: String? = null,
//    val avatar: String? = null,
//    val nick: String? = null,
//    val qrCode: String? = null,
//
//    val ext: String? = null, //留给app使用的扩展信息，字符串值可以被任意解释，如JSON字符串值
//
//    var permittedlevel: Int = EditionLevel.Free, // 操作权限，过期后降为免费权限
//) {
//    init {
//        // 操作权限，过期后降为免费权限
//        permittedlevel = if (level > EditionLevel.Free && expire != null && expire <= System.currentTimeMillis())
//            EditionLevel.Free
//        else
//            level
//    }
//}

/**
 * 用户profile
 * @param avatar 头像
 * @param email 邮箱
 * @param title 头衔
 * @param nick
 * @param tags 标签
 * */
//@Serializable
//data class Profile(
//    val _id: ObjectId,
//    val nick: String? = null,
//    val avatar: String? = null,
//    val email: String? = null,
//    val title: String? = null,
//    val tags: List<String>? = null
//)


