/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-11 16:50
 *
 */

@file:UseContextualSerialization(ObjectId::class)
//@file:UseContextualSerialization(ObjectId::class, MyReMsgSerializer::class)

package com.github.rwsbillyang.wxOA.msg

import com.github.rwsbillyang.ktorKit.apiJson.Box
import com.github.rwsbillyang.ktorKit.apiJson.IUmiListParams
import com.github.rwsbillyang.wxSDK.msg.MsgBody
import io.ktor.locations.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.and
import org.litote.kmongo.bson
import org.litote.kmongo.eq
import org.litote.kmongo.regex


//
/**
 * 包含了被动回复消息、群发消息和客服消息，这些消息可以保存到数据库中
 *
 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Passive_user_reply_message.html
 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html
 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Service_Center_messages.html
 *
 * 被动回复消息内容， 可以创建多个消息，用于回复各种inMsg或event
 * msg中的ClassDiscriminator本用来序列化和反序列化的多态，也用来给前端做消息类型信息
 *
 *
 *
 * @param name 给消息起的名称，便于识别记忆
 * //@param type 消息类型 通过上面的SerialName多态作为msg的一部分 没有appId属性
 * @param msg 消息内容
 * @param _id 主键
 * */
@Serializable
data class MyMsg(
        val name: String,
        val msg: MsgBody,
        val _id: ObjectId = ObjectId(),
        val appId: String? = null //为null，表示向前兼容没该字段的老系统
)



/**
 * 返回的列表数据
 * */
@Serializable
class MyMsgListBox(
        val total: Long,
        val data: List<MyMsg>? = null
) : Box()

@Location("/list")
data class MyMsgListParams(
        override val umi: String? = null,
        val _id: ObjectId? = null,
        val name: String? = null,
        val flag: Int? = null,
        val type: String? = null,
        val appId: String? = null,
        val lastId: String? = null
) : IUmiListParams {
    fun toFilter(): Bson {
        val idFilter = _id?.let { MyMsg::_id eq it }
        val nameFilter = name?.let { MyMsg::name regex ".*$it.*" }
        val flagFilter = flag?.let { "{\"msg.flag\": \"$it\"}".bson }
        val typeFilter = type?.let { "{\"msg.___type\": \"$it\"}".bson }
        val appIdFilter = appId?.let { MyMsg::appId eq it }
        return and(idFilter, flagFilter,  typeFilter, nameFilter,appIdFilter)
    }
}
