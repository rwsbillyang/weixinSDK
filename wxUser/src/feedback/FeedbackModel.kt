/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-28 12:46
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
package com.github.rwsbillyang.wxUser.feedback

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId



@Serializable
data class Feedback(
        val _id: ObjectId? = null,
        val uId: ObjectId,
        val desc: String,
        val type: String? = null,
        val imgs: List<String>? = null,
        val time: Long = System.currentTimeMillis(),
        val reply: String? = null
)

@Serializable
data class ReplyBean(
        val id: ObjectId,
        val reply: String
)