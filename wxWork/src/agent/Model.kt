/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-27 15:50
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
package com.github.rwsbillyang.wxWork.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId



/**
 * agentid	企业应用id
 * name	企业应用名称
 * square_logo_url	企业应用方形头像
 * description	企业应用详情
 * allow_userinfos	企业应用可见范围（人员），其中包括userid
 * allow_partys	企业应用可见范围（部门）
 * allow_tags	企业应用可见范围（标签）
 * close	企业应用是否被停用
 * redirect_domain	企业应用可信域名
 * report_location_flag	企业应用是否打开地理位置上报 0：不上报；1：进入会话上报；
 * isreportenter	是否上报用户进入应用事件。0：不接收；1：接收
 * home_url	应用主页url
 * */
@Serializable
data class Agent(
    val corpId: String,
    val id: Int, //agentid
    val name: String?,
    val logo: String?,
    val description: String?,
    val userList: List<String>?, // 若是指定了一个统一的部门，没指定用户，则此字段内容为空，只有部门
    val depList: List<Int>? = null, //
    val tagList: List<Int>? = null,
    val close: Int? = null,
    val domain: String?, //    "domain" : "zhike.niukid.com",
    val location: Int?,
    val enter: Int?,
    val url: String?,
    val _id: ObjectId = ObjectId()
)
