/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-01-06 14:29
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
package com.github.rwsbillyang.wxUser.fakeRpc

import com.github.rwsbillyang.wxUser.account.VID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId

/**
 *
 * 来自微信的信息
export interface FanInfo {
    openId?: string,
    unionId?: string,
    avatar?: string
    nick?: string
    sex?: number //用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
    address?: string //微信用户信息里的地址
}
 * */
@Serializable
class FanInfo (
    val openId: String, //前端各种参数传递然后查询后端依赖于fanInfo.openId
    val unionId: String? = null,
    val avatar: String? = null,
    val nick: String? = null,
    val sex: Int? = null, //用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
    val address: String? = null //click IP地址，查看用户详情时则是微信用户信息里的地址
)


/**
 * 需要wxOA中实现，并注入实体类
 * */
interface IFan {
    fun getFanInfo(openId: String, unionId: String?): FanInfo //公众号中需要实现 如果找不到，返回一个只有openId的FanInfo

    fun getFanInfo(vId: VID): FanInfo //企业微信中实现

    /**
     * 获取订阅的用户列表，如用于统计通知发送
     * @param page 分页当前页
     * @param pageSize 负值为全部
     * */
    fun getSubscribedList(page: Int = 0, pageSize: Int = -1): List<String> //直接调用account中的有效用户
}
