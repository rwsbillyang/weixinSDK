/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-11 16:57
 *
 */

@file:UseContextualSerialization(ObjectId::class)

package com.github.rwsbillyang.wxOA.pref


import com.github.rwsbillyang.ktorKit.apiJson.Box
import com.github.rwsbillyang.ktorKit.apiJson.IUmiListParams
import com.github.rwsbillyang.wxSDK.bean.MenuType
import com.github.rwsbillyang.wxOA.msg.MyMsg
import io.ktor.locations.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.and
import org.litote.kmongo.eq

/**
 * 公众号配置类
 *
 * 支持多公众号接入，但media和msg中则共用，没有区分是哪个公众号的
 *
 * @param _id appId       公众号或企业微信等申请的app id
 * @param name 公众号名称，暂可不设置
 * @param secret      对应的secret
 * @param token       Token可由开发者可以任意填写，用作生成签名（该Token会和接口URL中包含的Token进行比对，从而验证安全性）
 * @param aesKey encodingAESKey  安全模式需要需要  43个字符，EncodingAESKey由开发者手动填写或随机生成，将用作消息体加解密密钥。
 * @param wechatId 比如公众号的微信号，客服系统中需要设置
 * @param enable 方便多个公众号（比如测试）进行切换，只有一个是enable的，多个的话，则会查询列表时最后一个为准
 * @param host 用于拼接公众号菜单中的url和模板消息中的url，如：http://test.yangjiafang.com, https://youke.niukid.com
 * @param oauthWebUrl 微信oauth登录时，前端url，用于通知前端, 可在数据库中配置修改默认配置
 * */
@Serializable
data class PrefOfficialAccount(
        val _id: String, //appId
        val name: String,
        val secret: String,
        val token: String,
        val aesKey: String? = null,
        val wechatId: String? = null,
        val enable: Boolean? = null, //使用true时，api请求返回结果将移除默认值
        val host: String, // http://test.yangjiafang.com, https://youke.niukid.com
        val oauthWebUrl: String? = null //微信oauth登录时，前端url，用于通知前端, 可在数据库中配置修改默认配置
)

//返回列表时，去掉敏感信息
@Serializable
class OfficialAccountBean(
    val _id: String, //appId
    val name: String,
    val wechatId: String? = null,
    val enable: Boolean? = null, //使用true时，api请求返回结果将移除默认值
    val host: String, // http://test.yangjiafang.com, https://youke.niukid.com
    val oauthWebUrl: String? = null //微信oauth登录时，前端url，用于通知前端, 可在数据库中配置修改默认配置
)

/**
 * 保存到数据库的 消息/事件 的被动回复消息 配置
 * 接收到的msg对应的回复消息配置，区分了appId
 * 前端负责枚举所需各种msg类型，与appId共同拼接成_id
 * //@param appId: appId
 * @param type: msg type or event type
 * @param msgId 回复消息id
 * @param cat 类别，0: msg, 1: event
 * */
@Serializable
data class PrefReInMsg(
        var appId: String? = null,
        val type: String,
        val msgId: ObjectId,
        val cat: Int = 0,
        val _id: ObjectId = ObjectId()
) {
    companion object {
        const val CAT_MSG = 0
        const val CAT_EVENT = 1
    }
}
@Serializable
class PrefReMsgBean(
        var appId: String? = null,
        val type: String,
        val msgId: ObjectId?,
        val cat: Int = 0,
        val _id: ObjectId? = null,
        var msg: MyMsg? = null
)

@Location("/reMsgList")
data class PrefReMsgListParams(
        override val umi: String? = null,
        val cat: Int? = null,
        val type: String? = null,
        val appId: String? = null
) : IUmiListParams {
    fun toFilter(): Bson {
        val idFilter = cat?.let { PrefReInMsg::cat eq it }
        val typeFilter = type?.let { PrefReInMsg::type eq it }
        val appIdFilter = appId?.let { PrefReInMsg::appId eq it }
        return and(idFilter, typeFilter, appIdFilter)
    }
}

@Serializable
class PrefReInMsgList(
    val data: List<PrefReMsgBean>,
    val total: Long
) : Box()

/**
 * 保存到数据库的自定义菜单配置
 *
 * @param pId 父Id
 * @param name 菜单名称
 * @param type 菜单类型，当为父菜单时，为null
 * @param
 * */
@Serializable
data class PrefMenu(
        val name: String,
        var _id: ObjectId = ObjectId(),
        var appId: String? = null,
        val type: MenuType? = null,
        val key: String? = null,
        val url: String? = null,
        val mediaId: String? = null,
        val pagePath: String? = null,
        val miniId: String? = null,
        val pId: ObjectId? = null
)

/**
 * 给到前端的树形列表
 * */
@Serializable
class PrefMenuTree(
        val name: String,
        var _id: ObjectId? = null,
        var appId: String? = null,
        val type: MenuType? = null,
        val key: String? = null,
        val url: String? = null,
        val mediaId: String? = null,
        val pagePath: String? = null,
        val miniId: String? = null,
        val pId: ObjectId? = null,
        val children: List<PrefMenuTree>? = null //与PrefMenu多了一个children
)


@Location("/menu/list")
data class PrefMenuListParams(
    override val umi: String? = null,
    val appId: String? = null
) : IUmiListParams {
    fun toFilter(): Bson? {
        return appId?.let { PrefReInMsg::appId eq it }
    }
}
