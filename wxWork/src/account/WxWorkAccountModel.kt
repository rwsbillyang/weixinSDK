/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-29 10:50
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
package com.github.rwsbillyang.wxWork.account

import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
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
import org.litote.kmongo.regex
import java.time.LocalDateTime

/**
 * wxwork 独立的账户体系，也可与system Account关联绑定，统一共享同一个账户
 * */
@Serializable
data class WxWorkAccount(
    val _id: ObjectId,

    val corpId: String? = null,
    val suiteId: String? = null, //ISV模式，否则空
    val agentId: String? = null, //首次绑定账号时使用的agentId

    val openId: String? = null, // 企业微信的openId
    val userId: String? = null, //企业成员userId
    val externalId: String? = null, //外部成员ID
    val deviceId: String? = null,

    val sysId: ObjectId? = null, //绑定的系统账号，未绑定则为空
    val state: String = "1", //0 disabled, 1 enabled
    val time: LocalDateTime = LocalDateTime.now(),//创建时间

    val expire: ExpireInfo? = null, //各app使用自己的expire，没有的话则使用系统级的
    val roles: List<String>? = null, //'user' | 'guest' | 'admin'; 优先使用appId中的roles，没有的话使用系统级账号roles，再没有使用app级默认roles
    val gId: List<ObjectId>? = null //用户所属群组，一个用户可以加入多个群组 app级与system级取并集
)

@Serializable
@Resource("/list")
data class WxAccountListParams(
    override val umi: String? = null,
    val _id: String? = null,
    val status: String = "-1", //
    val userId: String? = null,
    val corpId: String? = null,
    val lastId: String? = null
) : IUmiPaginationParams { //实现IUmiPaginationParams接口目的在于继承一个函数：将umi字符串转换成UmiPagination对象
    override fun toFilter(): Bson {
        val idFilter = _id?.let { WxWorkAccount::_id eq it.toObjectId() }
        val statusF = if (status != "-1") WxWorkAccount::state eq status else null
        val userIdF = userId?.let { WxWorkAccount::userId regex (".*$it.*") }

        val corpIdF = corpId?.let { WxWorkAccount::corpId eq it }
        return and(idFilter, userIdF, statusF, corpIdF)
    }
}
//纯访客信息
@Serializable
class WxWorkGuest(
    val corpId: String? = null,
    val suiteId: String? = null, //ISV模式，否则空
    val agentId: String? = null,

    val openId: String? = null, // 企业微信的openId
    val userId: String? = null, //企业成员userId
    val externalId: String? = null, //外部成员ID
    val deviceId: String? = null
)

@Serializable
class WxWorkAccountAuthBean(
    val _id: String,
    val authBean: AuthBean,
    val guest: WxWorkGuest
)