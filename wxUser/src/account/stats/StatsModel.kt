/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-28 16:48
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

package com.github.rwsbillyang.wxUser.account.stats

import com.github.rwsbillyang.wxUser.account.LoginParamBean
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId



/**
 * 用户登录日志，可以用于登录频率统计分析，登录地址，登录设备分析等
 * @param _id 每次新生成
 * @param t 创建时间
 * @param ip IP地址
 * @param ua user agent
 * */
@Serializable
data class LoginLog(
        val _id: ObjectId,
        val uId: ObjectId,
        val t: Long,
        val type: Char,
        val ip: String?,
        val ua: String? //md5
        //,val type: Int
){
    companion object{
        fun fromLoginParam(param: LoginParamBean) = when(param.type){
            LoginParamBean.ACCOUNT -> LoginType_ACCOUNT
            LoginParamBean.MOBILE -> LoginType_MOBILE
            LoginParamBean.WECHAT -> LoginType_WECHAT
            LoginParamBean.WXWORK -> LoginType_WXWORK
            else -> LoginType_UNKNOEW
        }
        const val LoginType_ACCOUNT = 'A'
        const val LoginType_MOBILE = 'M'
        const val LoginType_WECHAT = 'W'
        const val LoginType_WXWORK = 'Q'
        const val LoginType_UNKNOEW = 'U'
    }
}

/**
 * 用户最近登录的token，可用于查验token是否有效，最近登录的用户统计等
 * @param _id 用户ID，即Account._id
 * @param t 时间
 * @param token 登录时返回的token  每次重新登录时候更新t和token
 * */
@Serializable
data class LoginToken(
        val _id: ObjectId,
        val t: Long,
        val token: String
)
/**
 * user agent md5化，包括计数, 最后更新时间，share，relay，click操作均会引起更新
 * */
@Serializable
data class LoginUa(
    val _id: String, //md5
    val ua: String,
    val count: Long,
    val time: Long, //最后更新时间
)