/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-02-02 16:02
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

package com.github.rwsbillyang.wxSDK.wxMini.config


import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxSDK.wxMini.wxMiniProgramModule
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

class MiniConfigService(cache: ICache) : CacheService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxMiniProgramModule.dbName!!))

    private val wxMiniConfigCol: CoroutineCollection<MiniConfig> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun findConfigList(enable: Boolean? = null) = runBlocking {
        if(enable != null){
            wxMiniConfigCol.find(MiniConfig::enable eq enable).toList()
        }else
            wxMiniConfigCol.find().toList()
    }
    fun saveConfig(doc: MiniConfig) = runBlocking {
        wxMiniConfigCol.save(doc)
        doc
    }
    fun delConfig(corpId: String) = runBlocking {
        wxMiniConfigCol.deleteOneById(corpId)
    }
}