/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-29 17:31
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

package com.github.rwsbillyang.wxUser.account

import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
import com.github.rwsbillyang.ktorKit.toObjectId
import io.ktor.resources.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.regex

@Serializable
class IdName(val _id: String, val name: String?)


@Serializable
data class Group(
    val _id: ObjectId,
    val name: String,
    val appId: String? = null,
    val status: Int = 1,
    val creator: ObjectId? = null, //创建者
    val admins: List<ObjectId>? = null, //管理成员
    val time: Long = System.currentTimeMillis() //创建时间
)

@Serializable
class GroupBean(
    var _id: String? = null,
    val name: String,
    val appId: String? = null,
    val status: Int = 1,
    val creator: String? = null, //创建者
    val creatorName: String? = null, //创建者
    val admins: List<IdName>? = null, //管理成员
    val time: Long = System.currentTimeMillis() //创建时间
)

@Serializable
@Resource("/list")
data class GroupListParams(
    override val umi: String? = null,
    val _id: String? = null,
    val status: Int = -1, //
    val name: String? = null,
    val appId: String? = null, //某个公众号的 某个企业的
    val lastId: String? = null
) : IUmiPaginationParams { //实现IUmiPaginationParams接口目的在于继承一个函数：将umi字符串转换成UmiPagination对象
    override fun toFilter(): Bson {
        val idFilter = _id?.let { Group::_id eq it.toObjectId() }
        val statusF = if (status > 0) Group::status eq status else null
        val nameF = name?.let { Group::name regex (".*$it.*") }
        val appIdF = appId?.let { Group::appId eq it  }
        return and(idFilter, nameF, appIdF, statusF)
    }
}
