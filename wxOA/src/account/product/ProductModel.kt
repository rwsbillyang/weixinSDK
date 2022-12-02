/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-30 20:04
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

package com.github.rwsbillyang.wxOA.account.product

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId



//虚拟物品，会员期限
@Serializable
data class Product(
    val _id: ObjectId = ObjectId(),
    val tag: String? = null, //产品标签，如channelHelper, chatViewer, zhike etc , 默认空类型支持以前的产品
    val name: String,
    val brief: String,
    val edition: Int,
    val actualPrice: Int, //actual price， unit:fen
    val displayPrice: Int, // display price
    val status: Int = 1,
    val type: Int? = null,
    val year: Int = 0,
    val month: Int = 0,
    val bonus: Int = 0, //Month
    val badged: Boolean? = null,
    val time: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_NORMAL = 0 //普通价
        const val TYPE_RECOMMEND = 1 //内部推荐价，创建不同的价格列表。前端根据类型加载

    }
}

