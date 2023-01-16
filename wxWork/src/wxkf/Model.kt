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
import kotlinx.serialization.Transient
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.json.JsonObject
import org.bson.types.ObjectId

@Serializable
data class WxkfAccount(
    val _id: String, //open_kfid
    val name: String,
    val avatar: String,
    val manage_privilege: Boolean,
    val corpId: String,
)
@Serializable
data class WxkfServicer(
    val _id: String, //open_kfid
    val corpId: String,
    val userIds: List<String>?,
    val department: List<Int>?
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
data class WxkfScene(
    var _id: ObjectId? = null,
    val kfId: String,
    val scene: String,
    val corpId: String,
    val desc: String? = null,
    var url: String? = null
)

//{"errcode":0,"errmsg":"ok","next_cursor":"4gw7MepFLfgF2VC5npU","msg_list":[
// {"msgid":"4rJQ5Bspe1Ft4FGsbbGiCnSWHHkkX3CQ3DadYwvhXMQC","send_time":1673578055,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"iIShBMyC1CJbonqJx16yjRtKSHzXHbTSJzMoA1Di-VA","scene_param":"%E5%9C%BA%E6%99%AF3"}},
// {"msgid":"4WmUMDx6HgwfSXzkmTBZjgPPDsnNN5B7YMHUhMVv3aWC","send_time":1673579617,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"6z6ju2dGucXHJ2WO1ggSv5L7OmG1t72VEq3qURQcUGs","scene_param":"%E5%9C%BA%E6%99%AF3"}},
// {"msgid":"7d2uv6UpjKHAGoEGbDk431GN5DsTtqy3mLEjFFLGDZ3J","send_time":1673581267,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"lj8_dNRP-QacJmV3C336mhJBPSGRsNyQVyU-vOg5T8Q","scene_param":"%E5%9C%BA%E6%99%AF3"}},
// {"msgid":"MXbuYYJYGqC7TXxWEPWqY4JzF3XKGKkkaQYWMnU8p","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","send_time":1673582683,"origin":3,"msgtype":"text","text":{"content":"几"}},
// {"msgid":"4rFuqvAWXpKUtviTNnu2afUF9gb1bh6myU3hSeee8U5z","send_time":1673624501,"origin":4,"msgtype":"event","event":{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"DiDoSXaUDrG4UUZtaN1kXqd7l1VXIl05YEylHKEeSjU","scene_param":"%E5%9C%BA%E6%99%AF3"}}],
// "has_more":0}
@Serializable
data class WxkfMsg(
    val _id: String, //msgid
    val corpId: String,
    val send_time: Long?,
    val origin: Int?,//消息来源。3-微信客户发送的消息 4-系统推送的事件消息 5-接待人员在企业微信客户端发送的消息
    val msgtype: String?,//event or msg type

    //kmongo reports exception because jershell/kbson not support jsonObject:
    // Exception: This serializer can be used only with Json format.Expected Encoder to be JsonEncoder, got class com.github.jershell.kbson.BsonEncoder
    @Transient var json: JsonObject? = null, //msg content json, key is msgtype
    val jsonStr: String?, //保存到db时需手工 json -> jsonStr, 读取时需手工 jsonStr -> json

    val open_kfid: String? = null, //enter_session时为空
    val external_userid: String? = null,//enter_session时为空
    val servicer_userid: String? = null

)
@Serializable
data class WxkfMsgCursor(
    val _id: String,//open_kfid
    val cursor: String
)


@Serializable
class WxkfCustomer(
    val nickname: String,
    val avatar: String?,
    val gender: Int,
    val externalId: String,
    val enterSessions: List<EnterSessionContext>? = null, //wxkf 中的来源场景列表
)