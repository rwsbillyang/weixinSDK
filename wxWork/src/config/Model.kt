/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-25 23:02
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

package com.github.rwsbillyang.wxWork.config


import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId

/**
 * 企业微信配置信息
 * 预置到wxWorkSettings中的信息：
 * @param _id corpId
 * @param enable 是否激活
 * @param agentId
 * @param secret
 * 1. 外部联系人：客户联系 -> 客户 -> API（点开“API”小按钮），即可看到。企业自建应用或小程序可以调用可见范围内的外部联系人相关接口
 * 2. 通讯录管理secret。“管理工具”-“通讯录同步”里面查看（需开启“API接口同步”）；使用通讯录管理接口，原则上需要使用 通讯录管理secret，
 * 也可以使用 应用secret。但是使用应用secret只能进行“查询”、“邀请”等非写操作，而且只能操作应用可见范围内的通讯录。
 * 3. 基础应用secret。“应用与小程序”->“应用-> “基础”，点进某个应用，点开“API”小按钮，即可看到。
 * 2. 自建应用secret：“应用与小程序”->“应用”->“自建”，点进某个应用，即可看到。
 *
 * @param token
 * @param aesKey
 * @param private 会话存档里需要的对会话进行解密的私钥，
 * 私钥的生成：genrsa -out app_private_key.pem 2048
 * 利用私钥生成公钥（放到管理后台中）：
 * rsa -in app_private_key.pem -pubout -out app_public_key.pem
 *
 * @param systemAccessTokenKeyMap 高权限key以及对应的secret，如通讯录
 * "systemAccessTokenKeyMap" : { "Contacts" : "secret", "ExternalContact" : "secret", "ChatArchive" : "secrets"}
 * */
@Serializable
data class WxWorkAgentConfig(
    var _id: ObjectId? = null,
    val name: String? = null, //便于记忆标识
    val url: String? = null, //从管理后台直接进入
    val corpId: String, //corpId 不是有corpID作为唯一ID：一个企业可以配置多个agent应用
    val enable: Boolean, //是否激活
    val agentId: String,
    val secret: String,
    val enableJsSdk: Boolean,
    val enableMsg: Boolean, //是否激活：消息解析、分发、处理
    val token: String? = null,
    val aesKey: String? = null,
    val private: String? = null
    //val systemAccessTokenKeyMap: HashMap<String, String>? = null //key->secret
)

/**
 * 消息通知中的url配置
 * */
@Serializable
data class WxMsgNotifyConfig(
    val _id: ObjectId, // appId
    val corpId: String,
    val agentId: String,
    val host: String, // eg: http://zhike.niukid.com or https://zhike.niukid.com
    val pathMap: Map<String, String> //key: 类型， value：路径
)

@Serializable
data class Corp(
    val _id: String, // corpId
    val name: String,
    val time: Long = System.currentTimeMillis()
)



//@Serializable
//class WxWorkAgentConfigBean(
//    val _id: ObjectId? = null,
//    val corpId: String, //corpId 不是有corpID作为唯一ID：一个企业可以配置多个agent应用
//    val enable: Boolean, //是否激活
//    val agentId: Int,
//    val enableJsSdk: Boolean,
//    val enableMsg: Boolean, //是否激活：消息解析、分发、处理
//)

//@Serializable
//data class WxWorkSysAgentConfig(
//    var _id: String? = null, //SysAccessTokenKey+corpId
//    val enable: Boolean, //是否激活
//    val key: String,
//    val corpId: String,
//    val secret: String,
//    val enableJsSdk: Boolean = false,
//    val enableMsg: Boolean = false, //是否激活：消息解析、分发、处理
//    val token: String? = null,
//    val aesKey: String? = null,
//    val private: String? = null
//){
//    fun id() = "$corpId-${key}"
//}
