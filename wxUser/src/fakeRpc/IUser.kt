/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-21 21:45
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

package com.github.rwsbillyang.wxUser.fakeRpc

import kotlinx.serialization.Serializable

@Serializable
class UserInfo(
    val uId: String,
    val oId: String?,
    val level: Int?, //permittedLevel
    val expire: Long? //utc time
)

/**
 * 用户等级，与前端需保持一致
 * */
object EditionLevel {
    const val Free = 0
    const val VIP = 10
    const val DiamondVIP = 20
}

fun level2Name(level: Int?) = when(level){
    EditionLevel.Free -> "免费会员"
    EditionLevel.VIP -> "VIP"
    EditionLevel.DiamondVIP -> "SVIP"
    else -> "免费会员"
}

//无需该接口，直接使用AccountService
//interface IUser {
//    /**
//     * 根据uId换取openId
//     * */
//    fun getOpenId(uId: String): String?
//
//    /**
//     * 通过uId获取用户信息
//     * */
//    fun getUserInfo(uId: String): UserInfo?
//
//    /**
//     * 通过openId获取有效用户的uId
//     * */
//    fun getUserId(openId: String): String?
//}
