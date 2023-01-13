/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-05 11:50
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
package com.github.rwsbillyang.wxWork.wxkf

import com.github.rwsbillyang.wxSDK.work.EnterSessionContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.json.JsonObject
import org.bson.types.ObjectId

@Serializable
data class WxKfAccount(
    val _id: String, //open_kfid
    val name: String,
    val avatar: String,
    val manage_privilege: Boolean,
    val corpId: String,
)

/**
 * @param kfId 客服id
 * @param scene 场景值 不多于32字节 字符串取值范围(正则表达式)：[0-9a-zA-Z_-]*
 * @param corpId 企业corpId
 * @param desc 场景说明
 *
 * @param url 获取客服帐号链接返回的url，
 * 形如："https://work.weixin.qq.com/kf/kfcbf8f8d07ac7215f?enc_scene=ENCGFSDF567DF"
 * 用于拼接：https://work.weixin.qq.com/kf/kfcbf8f8d07ac7215f?enc_scene=ENCGFSDF567DF&scene_param=a%3D1%26b%3D2
 * */
@Serializable
data class WxKfScene(
    var _id: ObjectId? = null,
    val kfId: String,
    val scene: String,
    val corpId: String,
    val desc: String? = null,
    var url: String? = null
)

@Serializable
data class WxkfMsg(
    val _id: String, //msgid
    val corpId: String,
    val open_kfid: String,
    val external_userid: String,
    val send_time: Long,
    val origin: Int,
    val servicer_userid: String,
    val msgtype: String,
    val content: JsonObject // msg content json
)
@Serializable
data class WxkfMsgCursor(
    val _id: String,//open_kfid
    val cursor: String
)


@Serializable
class WxKfCustomer(
    val nickname: String,
    val avatar: String?,
    val gender: Int,
    val externalId: String,
    val enterSessions: List<EnterSessionContext>? = null, //wxkf 中的来源场景列表
)