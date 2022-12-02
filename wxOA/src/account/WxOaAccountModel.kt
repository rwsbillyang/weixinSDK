/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-29 20:49
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

@file:UseContextualSerialization(ObjectId::class,LocalDateTime::class)
package com.github.rwsbillyang.wxOA.account

import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
import com.github.rwsbillyang.ktorKit.server.Role
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxUser.account.AuthBean
import com.github.rwsbillyang.wxUser.account.ExpireInfo
import io.ktor.resources.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.and
import org.litote.kmongo.eq
import java.time.LocalDateTime

@Serializable
data class WxOaAccount(
    val _id: ObjectId,

    val appId: String,
    val openId: String,
    val unionId: String? = null,

    val sysId: ObjectId? = null, //绑定的系统账号，未绑定则为空
    val state: String = "1", //0 disabled, 1 enabled
    val time: LocalDateTime = LocalDateTime.now(),//创建时间
    val need: Int? = null, //needUserInfo setting

    val expire: ExpireInfo? = null, //各app使用自己的expire，没有的话则使用系统级的
    val roles: List<String>? = null, //'user' | 'guest' | 'admin'; 优先使用appId中的roles，没有的话使用系统级账号roles，再没有使用app级默认roles
    val gId: List<ObjectId>? = null //用户所属群组，一个用户可以加入多个群组 app级与system级取并集
)

@Serializable
@Resource("/list")
data class WxOaAccountListParams(
    override val umi: String? = null,
    val _id: String? = null,
    val status: String = "-1", //
    val openId: String? = null,
    val appId: String? = null,
    val lastId: String? = null
) : IUmiPaginationParams { //实现IUmiPaginationParams接口目的在于继承一个函数：将umi字符串转换成UmiPagination对象
    override fun toFilter(): Bson {
        val idFilter = _id?.let { WxOaAccount::_id eq it.toObjectId() }
        val statusF = if (status != "-1") WxOaAccount::state eq status else null
        val openIdF = openId?.let { WxOaAccount::openId eq it }

        val appIdF = appId?.let { WxOaAccount::appId eq it }
        return and(idFilter, openIdF, statusF, appIdF)
    }
}
//纯访客信息
@Serializable
class WxOaGuest(
    val appId: String,
    val openId: String? = null, // 微信openId
    val unionId: String? = null,
)

@Serializable
class WxOaAccountAuthBean(
    val _id: String, //wxOaAccount._id
    val authBean: AuthBean,
    val guest: WxOaGuest
)