/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-28 12:58
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

package com.github.rwsbillyang.wxUser.feedback


import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class FeedbackController: KoinComponent {
    private val service: FeedbackService by inject()

    fun saveFeedback(feedback: Feedback) = service.insertFeedback(feedback)

    fun reply(id: ObjectId, reply: String) = service.reply(id,reply)
}