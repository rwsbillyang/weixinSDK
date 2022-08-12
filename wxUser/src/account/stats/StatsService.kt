/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-28 16:49
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

package com.github.rwsbillyang.wxUser.account.stats

import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxUser.wxUserAppModule
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

//统计数据保存在各自自己的agent之中
class StatsService (cache: ICache) : CacheService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxUserAppModule.dbName!!))


    private val loginLogCol: CoroutineCollection<LoginLog> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val loginTokenCol: CoroutineCollection<LoginToken> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val uaMd5Col: CoroutineCollection<LoginUa> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun insertLoginLog(doc: LoginLog) = runBlocking {
        loginLogCol.insertOne(doc)
    }

    fun upsertLoginToken(uId: ObjectId, now: Long, token: String) = runBlocking {
        loginTokenCol.updateOneById(uId, set(SetTo(LoginToken::t, now), SetTo(LoginToken::token, token)), upsert())
    }


    fun upsertMd5Ua(md5: String, ua: String) = runBlocking{
        uaMd5Col.updateOneById(md5, combine(setOnInsert(LoginUa::ua, ua), inc(LoginUa::count, 1), setValue(LoginUa::time, System.currentTimeMillis())), upsert())
    }

    fun findLoginToken(uId: ObjectId) = runBlocking {
        loginTokenCol.findOneById(uId)
    }
}