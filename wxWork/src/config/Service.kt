/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-25 23:15
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

package com.github.rwsbillyang.wxWork.config


import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoCRUDService
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxWork.wxWorkModule
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.and
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

class ConfigService(cache: ICache) : MongoCRUDService(cache,wxWorkModule.dbName!!) {
    //private val dbSource: MongoDataSource by inject(qualifier = named(wxWorkModule.dbName!!))

    private val wxCorpCol: CoroutineCollection<Corp> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val wxWorkAgentCfgCol: CoroutineCollection<WxWorkAgentConfig> by lazy {
        dbSource.mongoDb.getCollection()
    }
    val wxWorkSysAgentCfgCol: CoroutineCollection<WxWorkAgentConfig> by lazy {
        dbSource.mongoDb.getCollection("wxWorkSysAgentConfig")
    }

    private val msgNotifyConfigCol: CoroutineCollection<WxMsgNotifyConfig> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun findMsgNotifyConfig(corpId: String, agentId: String?) = cacheIncludeNull("notifyUrlConfig/$corpId/$agentId"){
        runBlocking{
            msgNotifyConfigCol.findOne(WxMsgNotifyConfig::corpId eq corpId, WxMsgNotifyConfig::agentId eq agentId)
        }
    }

    fun findAgentConfigList(enabled: Boolean? = null) = runBlocking {
        if(enabled == null) wxWorkAgentCfgCol.find().toList()
        else wxWorkAgentCfgCol.find(WxWorkAgentConfig::enable eq enabled).toList()
    }
    fun findWxWorkAgentConfig(id: String): WxWorkAgentConfig? = runBlocking{
        wxWorkAgentCfgCol.findOneById(id.toObjectId())
    }
    fun findWxWorkAgentConfigByAgentId(corpId: String, agentId: String): WxWorkAgentConfig? = runBlocking{
        wxWorkAgentCfgCol.findOne(and(WxWorkAgentConfig::corpId eq corpId, WxWorkAgentConfig::agentId eq agentId))
    }
    fun findWxWorkAgentConfigByCorpId(corpId: String) = runBlocking{
        wxWorkAgentCfgCol.find(WxWorkAgentConfig::corpId eq corpId).toList()
    }

    fun saveWxWorkAgentConfig(doc: WxWorkAgentConfig) = runBlocking {
        wxWorkAgentCfgCol.save(doc)
    }

    fun findSysAgentConfigList(enabled: Boolean? = null) = runBlocking {
        if(enabled == null) wxWorkSysAgentCfgCol.find().toList()
        else wxWorkSysAgentCfgCol.find(WxWorkAgentConfig::enable eq enabled).toList()
    }
    fun saveWxWorkSysAgentConfig(doc: WxWorkAgentConfig) = runBlocking {
        wxWorkSysAgentCfgCol.save(doc)
    }
    fun findWxWorkSysAgentConfig(id: String): WxWorkAgentConfig? = runBlocking{
        wxWorkSysAgentCfgCol.findOneById(id)
    }

    fun delAgentConfig(id: String) = runBlocking{
        wxWorkAgentCfgCol.deleteOneById(id.toObjectId())
    }

    fun findCorpList() = runBlocking {
        wxCorpCol.find().toList()
    }
    fun saveCorp(doc: Corp) = runBlocking {
        wxCorpCol.save(doc)
        doc
    }
    fun delCorp(corpId: String) = runBlocking {
        wxCorpCol.deleteOneById(corpId)
    }




}