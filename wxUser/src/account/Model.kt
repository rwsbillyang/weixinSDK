/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-09-23 10:51
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

@file:UseContextualSerialization(ObjectId::class)

package com.github.rwsbillyang.wxUser.account


import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.ktorKit.server.corpId
import com.github.rwsbillyang.ktorKit.server.oId
import com.github.rwsbillyang.ktorKit.server.uId
import com.github.rwsbillyang.wxUser.externalUserId
import com.github.rwsbillyang.wxUser.fakeRpc.EditionLevel
import com.github.rwsbillyang.wxUser.userId
import io.ktor.resources.*
import io.ktor.server.application.*

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.apache.commons.codec.digest.DigestUtils
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.*


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
data class LoginParamBean(val name: String, val pwd: String? = null, val type: String? = ACCOUNT) {
    companion object {
        const val ACCOUNT = "account"
        const val MOBILE = "mobile"
        const val WECHAT = "wechat"
        const val WXWORK = "wxWork"
        const val SCAN_QRCODE = "scanQrcode"
    }
}

/**
 * 公众号或企业微信注册时提供的信息
 * */
@Serializable
class GuestOAuthBean(
    val appId: String? = null,// 公众号
    val unionId: String? = null,// 公众号
    val openId1: String? = null, // 公众号访客openId
    //val hasUserInfo: Boolean ? = null,

    val openId2: String? = null, // 企业微信访客openId
    //企业微信
    val userId: String? = null, //企业微信内部员工userid
    //val externalUserId: String? = null,//企业微信外部成员id
    val corpId: String? = null,
    val agentId: Int? = null,
    val suiteId: String? = null,
    val deviceId: String? = null,

    val rcm: String? = null //推荐者uId
)

/**
 * login后返回的信息
 * */
@Serializable
class AuthBean(
    val uId: String,
    val token: String,
    val level: Int, // 当前edition level 可能已过期
    val expire: Long? = null,
    val role: List<String>? = null, //若为空，自动赋值 "user"
    val gId:List<String>? = null, //所属群组

    val unionId: String? = null,
    val openId1: String? = null, // 公众号 用于一个运行中的程序同时支持公众号和企业微信。 返回给前端，只在访客click/relay时用于标识clicker和relayer
    val openId2: String? = null, // 企业微信 用于一个运行中的程序同时支持公众号和企业微信。 返回给前端，只在访客click/relay时用于标识clicker和relayer
    val appId: String? = null,
    val miniId: String? = null,

    //企业微信
    val userId: String? = null,
    val externalUserId: String? = null,
    val corpId: String? = null,
    val agentId: Int? = null,
    val suiteId: String? = null,
    val avatar: String? = null,
    val nick: String? = null,
    val qrCode: String? = null,

    val ext: String? = null, //留给app使用的扩展信息，字符串值可以被任意解释，如JSON字符串值

    var permittedlevel: Int = EditionLevel.Free, // 操作权限，过期后降为免费权限
) {
    init {
        // 操作权限，过期后降为免费权限
        permittedlevel = if (level > EditionLevel.Free && expire != null && expire <= System.currentTimeMillis())
            EditionLevel.Free
        else
            level
    }
}

/**
 * 推荐关系链
 * */
@Serializable
data class Recommend(
    val _id: ObjectId, //被推荐人
    val rcm: ObjectId, //推荐人
    val time: Long = System.currentTimeMillis()
)

class ExpireInfo(
    val expire: Long? = null, //为空时，表示无限期
    val role: List<String>? = null, //'user' | 'guest' | 'admin';
    val level: Int = EditionLevel.Free
)

/**
 * 用户账号，最简设计原则进行设计，更多的用户信息放到其profile中
 * 可支持：一个运行中的后端server，同时支持公众号与企业微信中的agent共享一个账户，
 * 多个agent与一个共享一个账户；也支持各agent有自己不同的权限期限，
 * 但公众号和企业微信agent的权限信息不共享，各是各的。可以考虑同一个应用支持公众号和agent，此时共享权限信息或者以最高者为准
 *
 * @param tel 电话号码
 * @param name 登录用户名
 * @param pwd 密码 使用salt加密后的密码
 * @param salt 密码加盐
 * @param state 状态
 * @param openId1 公众号对应的微信openId
 * @param unionId 公众号对应的微信unionId
 * @param time 创始时间
 * */
@Serializable
data class Account(
    val _id: ObjectId,
    val state: String = STATE_ENABLED,
    val time: Long = System.currentTimeMillis(), //create time

    //web
    val tel: String? = null,
    val name: String? = null,
    val pwd: String? = null,
    val salt: String? = null,

    //公众号
    val openId1: String? = null,//openId  公众号 用于支持同一个账户可绑定到企业微信,暂只支持绑定到一个企业微信
    val unionId: String? = null,//unionId
    val needUserInfo: Boolean? = null, //是否获取用户信息
    val appId: String? = null,

    //企业微信
    val userId: String? = null, //企业成员userId，与corpId配对使用，否则可能不唯一 优先使用corpID+openId查找，其次是corpId+userID
    val deviceId: String? = null,
    val corpId: String? = null,
    val suiteId: String? = null,
    val openId2: String? = null, // 企业微信的openId，用于支持同一个账户可绑定到企业微信,暂只支持绑定到一个企业微信
    //val agentId: Int? = null //注释掉后，各agent可共享account，expire信息另放其它表中。打开注释: 则一个agent应用拥有自己account数据表，不同agent不共享

    //权限信息 对于微信公众号，权限信息存于此处
    // 由前端webapp传递agent参数来决定将权限存于何处，若额外特别指定了agent则，则存于AccountExpire中，否则存于此处
    val expire: Long? = null, //为空时，表示无限期
    val role: List<String>? = null, //'user' | 'guest' | 'admin';
    val level: Int? = null,

    val gId: List<ObjectId>? = null, //用户所属群组，一个用户可以加入多个群组

    val ext: String? = null, //留给app使用的扩展信息，字符串值可以被任意解释，如JSON字符串值.对于MiniMall可以用于存储shopId, 如推广模式：个人品牌，产品广告等

    val miniId: String? = null, //小程序的appId，openId与unionId与公众号共用
    val nick: String? = null //昵称 免得另使用profile
) {
    companion object {
        const val STATE_ENABLED = "1"
        const val STATE_DISABLED = "0"

        fun encryptPwd(pwd: String, salt: String): String = DigestUtils.sha256Hex(pwd + salt)
    }

    fun toVID() = VID(_id.to64String(), userId, openId2, null, corpId)
    fun toExpireInfo() = if (expire == null && level == null && role == null) null else ExpireInfo(
        expire,
        role,
        level ?: EditionLevel.Free
    )

}

@Serializable
@Resource("/list")
data class AccountListParams(
    override val umi: String? = null,
    val _id: String? = null,
    val status: String = "-1", //
    val name: String? = null,
    val appId: String? = null, //某个公众号的
    val corpId: String? = null,//某个企业的
    val userId: String? = null,//企业微信
    val gId: String? = null,
    val lastId: String? = null
) : IUmiPaginationParams { //实现IUmiPaginationParams接口目的在于继承一个函数：将umi字符串转换成UmiPagination对象
    override fun toFilter(): Bson {
        val idFilter = _id?.let { Account::_id eq it.toObjectId() }
        val statusF = if (status != "-1") Account::state eq status else null
        val corpIdF = corpId?.let { Account::corpId eq it }
        val appIdF = appId?.let { Account::appId eq it }
        val nameF = name?.let { Account::name regex (".*$it.*") }
        val userIdF = userId?.let { Account::name eq it }
        val gIdF = gId?.let { Account::gId contains it.toObjectId() }
        return and(idFilter, userIdF, gIdF, appIdF, corpIdF, statusF, nameF)
    }
}

/**
 * 查询账户列表时返回的账号项
 * */
@Serializable
class AccountBean(
    val _id: String,
    val state: String,
    val time: Long, //create time

    //web
    val tel: String? = null,
    var name: String? = null,

    //公众号
    val openId1: String? = null,//openId  公众号 用于支持同一个账户可绑定到企业微信,暂只支持绑定到一个企业微信
    val needUserInfo: Boolean? = null, //是否获取用户信息
    val appId: String? = null,

    //企业微信
    val userId: String? = null, //企业成员userId，与corpId配对使用，否则可能不唯一 优先使用corpID+openId查找，其次是corpId+userID
    val corpId: String? = null,
    val suiteId: String? = null,
    val openId2: String? = null, // 企业微信的openId，用于支持同一个账户可绑定到企业微信,暂只支持绑定到一个企业微信

    //权限信息 对于微信公众号，权限信息存于此处
    // 由前端webapp传递agent参数来决定将权限存于何处，若额外特别指定了agent则，则存于AccountExpire中，否则存于此处
    val expire: Long? = null, //为空时，表示无限期
    val role: List<String>? = null, //'user' | 'guest' | 'admin';
    val level: Int? = null,

    val gId: List<IdName>? = null, //用户所属群组，一个用户可以加入多个群组

    val ext: String? = null, //留给app使用的扩展信息，字符串值可以被任意解释，如JSON字符串值.对于MiniMall可以用于存储shopId

)

@Serializable
class IdName(val _id: String, val name: String)

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
@Serializable
data class AccountExpire(
    val _id: String, // 公众号：account._id.to64String， 企业微信：account._id.to64String/agentId
    //权限信息
    val expire: Long? = null, //为空时，表示无限期
    val role: List<String>? = null, //'user' | 'guest' | 'admin';
    val level: Int = EditionLevel.Free
    //val agentId: Int? = null //企业微信账户才有值
) {
    companion object {
        fun id(uId: String, agentId: Int?) = if (agentId != null) "$uId/$agentId" else uId
    }

    fun toExpireInfo() = ExpireInfo(expire, role, level)
}

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
@Serializable
class VID(
    val uId: String? = null, // Account._id
    val userId: String? = null, //企业成员userId，与corpId配对使用，否则可能不唯一
    var openId: String? = null,//企业userId会改变，优先使用openId查找 未登录的可见范围内部成员访问他人信息时将只有userId，无openId，需要查修改添加上
    val externalId: String? = null,
    val corpId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other is VID) {
            if (uId != null && other.uId != null && uId == other.uId)
                return true

            if (other.corpId == null || corpId == null || other.corpId != corpId)
                return false

            if (openId != null && other.openId != null && openId == other.openId)
                return true

            if (userId != null && other.userId != null && userId == other.userId)
                return true

            if (externalId != null && other.externalId != null && externalId == other.externalId)
                return true
        }

        return false
    }

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
    fun toOpenId(): String? {
        if (corpId == null) return null
        if (openId != null) return openId //"$corpId-$openId"
        if (uId != null) return uId
        if (userId != null) return userId //"$corpId-$userId"不能用斜杠，因可能作为请求路径中的参数
        if (externalId != null) return externalId //"$corpId-$externalId"
        return null
    }
}

fun ApplicationCall.toVID() =
    if (corpId == null || (userId == null && externalUserId == null && oId == null)) null else VID(
        uId,
        userId,
        oId,
        externalUserId,
        corpId
    )

@Serializable
data class Group(
    val _id: ObjectId,
    val name: String,
    val status: Int = 1,
    val appId: String? = null, //所属公众号
    val corpId: String? = null,//所属企业微信中的某个企业
    val creator: ObjectId? = null, //创建者
    val admins: List<ObjectId>? = null, //管理成员
    val time: Long = System.currentTimeMillis() //创建时间
)

@Serializable
class GroupBean(
    var _id: String? = null,
    val name: String,
    val status: Int = 1,
    val appId: String? = null, //所属公众号
    val corpId: String? = null,//所属企业微信中的某个企业
    val creator: String? = null, //创建者
    val creatorName: String? = null, //创建者
    val admins: List<IdName>? = null, //管理成员
    val time: Long = System.currentTimeMillis() //创建时间
)

@Serializable
@Resource("/list")
data class GroupListParams(
    override val umi: String? = null,
    val _id: String? = null,
    val status: Int = -1, //
    val name: String? = null,
    val appId: String? = null, //某个公众号的
    val corpId: String? = null,//某个企业的
    val lastId: String? = null
) : IUmiPaginationParams { //实现IUmiPaginationParams接口目的在于继承一个函数：将umi字符串转换成UmiPagination对象
    override fun toFilter(): Bson {
        val idFilter = _id?.let { Group::_id eq it.toObjectId() }
        val statusF = if (status > 0) Group::status eq status else null
        val corpIdF = corpId?.let { Group::corpId eq it }
        val appIdF = appId?.let { Group::appId eq it }
        val nameF = name?.let { Group::name regex (".*$it.*") }

        return and(idFilter, appIdF, corpIdF, nameF, statusF)
    }
}


/**
 * 用户profile
 * @param avatar 头像
 * @param email 邮箱
 * @param title 头衔
 * @param nick
 * @param tags 标签
 * */
@Serializable
data class Profile(
    val _id: ObjectId,
    val nick: String? = null,
    val avatar: String? = null,
    val email: String? = null,
    val title: String? = null,
    val tags: List<String>? = null
)


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
