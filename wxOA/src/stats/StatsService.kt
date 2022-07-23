/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-10 18:01
 *
 */

package com.github.rwsbillyang.wxOA.stats

import com.github.rwsbillyang.ktorKit.DataSource
import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.wxOA.wxOaAppModule
import kotlinx.coroutines.runBlocking
import org.koin.core.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection

class StatsService (cache: ICache) : CacheService(cache) {
    private val dbSource: DataSource by inject(qualifier = named(wxOaAppModule.dbName!!))


    private val eventCol: CoroutineCollection<StatsEvent> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val msgCol: CoroutineCollection<StatsMsg> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun insertEvent(event: StatsEvent) = runBlocking { eventCol.insertOne(event) }

    fun insertMsg(msg: StatsMsg) = runBlocking { msgCol.insertOne(msg) }

}
