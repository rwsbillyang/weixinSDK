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
import com.github.rwsbillyang.ktorKit.toObjectId

import io.ktor.resources.*

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.apache.commons.codec.digest.DigestUtils
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.*


//为空时，表示无限期
@Serializable
class ExpireInfo(
    val expire: Long,
    var level: Int = EditionLevel.Free
)


@Serializable
class Profile(
    val nick: String? = null,
    val avatar: String? = null,
    val sex: Int? = null, //用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
    val address: String? = null //click IP地址，查看用户详情时则是微信用户信息里的地址
)
@Serializable
class AuthBean (
    val token: String,
    val sysId: String? = null, //system accountId
    var roles: List<String>?, //'user' | 'guest' | 'admin';
    val expire: ExpireInfo?,
    val profile: Profile?,
    val gId: List<String>?, //group id
)

//形式与wxOA和wxWork统一，便于前端统一处理
@Serializable
class SysAccountAuthBean(val authBean: AuthBean)
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
 * @param time 创始时间
 * */
@Serializable
data class Account(
    val _id: ObjectId,
    val state: String = STATE_ENABLED,
    val time: Long = System.currentTimeMillis(), //create time

    //web
    val tel: String? = null,
    val name: String? = null, // login name
    val pwd: String? = null, //login pwd
    val salt: String? = null,

    val profile: Profile? = null,
    val remark: String? = null,

    val expire: ExpireInfo? = null, //各app使用自己的expire，没有的话则使用系统级的
    val roles: List<String>? = null, //'user' | 'guest' | 'admin'; 优先使用appId中的roles，没有的话使用系统级账号roles，再没有使用app级默认roles
    val gId: List<ObjectId>? = null //用户所属群组，一个用户可以加入多个群组 app级与system级取并集
) {
    companion object {
        const val STATE_ENABLED = "1"
        const val STATE_DISABLED = "0"

        fun encryptPwd(pwd: String, salt: String): String = DigestUtils.sha256Hex(pwd + salt)
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
    val name: String? = null,

    val gId: List<IdName>? = null, //用户所属群组，一个用户可以加入多个群组
    val expire: ExpireInfo? = null,
    val profile: Profile? = null
)



@Serializable
@Resource("/list")
data class AccountListParams(
    override val umi: String? = null,
    val _id: String? = null,
    val status: String = "-1", //
    val name: String? = null,
    val nick: String? = null,
    val gId: String? = null,
    val lastId: String? = null
) : IUmiPaginationParams { //实现IUmiPaginationParams接口目的在于继承一个函数：将umi字符串转换成UmiPagination对象
    override fun toFilter(): Bson {
        val idFilter = _id?.let { Account::_id eq it.toObjectId() }
        val statusF = if (status != "-1") Account::state eq status else null
        val nameF = name?.let { Account::name regex (".*$it.*") }
        val nickF = nick?.let { Account::profile / Profile::nick eq it }
        val gIdF = gId?.let { Account::gId contains it.toObjectId() }
        return and(idFilter, gIdF, nickF, statusF, nameF)
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
