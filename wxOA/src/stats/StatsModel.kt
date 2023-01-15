/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-10 18:01
 *
 */

@file:UseContextualSerialization(ObjectId::class)
package com.github.rwsbillyang.wxOA.stats

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId

/**
 * 推送过来的事件信息，用于统计
 * */
@Serializable
data class StatsEvent(
        val appId: String? = null, //为null，表示向前兼容没该字段的老系统
        val to: String?,//公众号微信号
        val from: String?,
        val time: Long?,
        val event: String?,
        //val eKey: String? = null,
        val extra:  String? = null,
        val _id: ObjectId = ObjectId()
)

/**
 * 推送过来的消息，用于统计
 * */
@Serializable
data class StatsMsg(
        val appId: String? = null,//为null，表示向前兼容没该字段的老系统
        val to: String?,//公众号微信号
        val from: String?,
        val time: Long?,
        val msgId: Long?,
        val extra: String? = null,
        val _id: ObjectId = ObjectId()
)
