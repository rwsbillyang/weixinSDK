/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-28 12:53
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


import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxUser.wxUserAppModule
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.core.component.inject

import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.setValue

class FeedbackService(cache: ICache) : CacheService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxUserAppModule.dbName!!))

    private val feedbackCol: CoroutineCollection<Feedback> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun insertFeedback(doc: Feedback) = runBlocking{
        feedbackCol.insertOne(doc)
    }
    fun reply(id: ObjectId, content: String) = runBlocking {
        feedbackCol.updateOneById(id, setValue(Feedback::reply, content))
    }

}