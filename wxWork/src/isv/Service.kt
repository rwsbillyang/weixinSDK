/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-09 20:56
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

package com.github.rwsbillyang.wxWork.isv

import com.github.rwsbillyang.ktorKit.DataSource
import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.wxSDK.work.isv.AgentInfo
import com.github.rwsbillyang.wxSDK.work.isv.AgentPrivilege
import com.github.rwsbillyang.wxSDK.work.isv.AuthInfo
import com.github.rwsbillyang.wxWork.wxWorkModule

import kotlinx.coroutines.runBlocking
import org.koin.core.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class IsvCorpService (cache: ICache) : CacheService(cache) {

    private val dbSource: DataSource by inject(qualifier = named(wxWorkModule.dbName!!))

    private val corpInfoCol: CoroutineCollection<CorpInfo> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val userDetail3rdCol: CoroutineCollection<UserDetail3rd> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val suiteConfigCol: CoroutineCollection<SuiteConfig> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun save(corpInfo: CorpInfo) = evict(corpInfo._id){
        runBlocking{
            corpInfoCol.save(corpInfo)
        }
    }

    fun updateStatus(id: String, status: Int) = evict(id){
        runBlocking {
            corpInfoCol.updateOneById(id, status)
        }
    }

    fun findCorpInfoList(suiteId: String) = runBlocking{
        corpInfoCol.find(CorpInfo::suiteId eq suiteId, CorpInfo::status eq CorpInfo.STATUS_ENABLED).toList()
    }
    fun findCorpInfoList(status: Int?) = runBlocking {
        if (status != null) {
            corpInfoCol.find(CorpInfo::status eq status).projection(exclude(CorpInfo::dealerInfo, CorpInfo::registerInfo, CorpInfo::permanentCode, CorpInfo::time)).toList()
        }else
            corpInfoCol.find().toList()
    }
    fun findCorpInfoListOnlySuiteIdCorpId(status: Int?) = runBlocking {
        if (status != null) {
            corpInfoCol.find(CorpInfo::status eq status).projection(include(CorpInfo::_id, CorpInfo::suiteId, CorpInfo::corpId, CorpInfo::status)).toList()
        }else
            corpInfoCol.find().toList()
    }
    fun findOneCorpInfo(suiteId: String, corpId: String) = cacheable(CorpInfo.id(suiteId, corpId)){
        runBlocking {
            corpInfoCol.findOneById(CorpInfo.id(suiteId, corpId))
        }
    }

    fun addAllowUser(suiteId: String, corpId: String, userId: String) = evict(CorpInfo.id(suiteId, corpId)){
        runBlocking {
            corpInfoCol.updateOneById(CorpInfo.id(suiteId, corpId),
                addToSet((CorpInfo::authInfo / AuthInfo::agent).allPosOp / AgentInfo::privilege / AgentPrivilege::allow_user, userId))
        }
    }

    fun removeAllowUser(suiteId: String, corpId: String, userId: String) = evict(CorpInfo.id(suiteId, corpId)){
        runBlocking {
            corpInfoCol.updateOneById(CorpInfo.id(suiteId, corpId),
                pull(
                    (CorpInfo::authInfo / AuthInfo::agent).allPosOp / AgentInfo::privilege / AgentPrivilege::allow_user,
                    userId
                )
            )
        }
    }

    fun saveUserDetail3rd(doc: UserDetail3rd) = runBlocking {
        userDetail3rdCol.save(doc.apply { _id="${doc.corpId}/${doc.userId}" })
    }
    fun findUserDetail3rd(corpId: String, userId: String) = runBlocking {
        userDetail3rdCol.findOneById("$corpId/$userId")
    }


    /**
     * 不提供状态，则查询所有；否则指定状态
     * */
    fun findSuiteConfigList(status: Int? = null) = runBlocking {
        if(status == null)
            suiteConfigCol.find().toList()
        else
            suiteConfigCol.find(SuiteConfig::status eq status).toList()
    }


}