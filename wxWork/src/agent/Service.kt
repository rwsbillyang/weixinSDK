/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-27 15:58
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

package com.github.rwsbillyang.wxWork.agent

import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxSDK.work.ContactsApi
import com.github.rwsbillyang.wxWork.wxWorkModule
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject

import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class AgentService(cache: ICache) : CacheService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxWorkModule.dbName!!))

    private val agentCol: CoroutineCollection<Agent> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun findAgent(id: Int, corpId: String): Agent? = cacheable("agent/$corpId/$id") {
        runBlocking{ agentCol.findOne(Agent::id eq id, Agent::corpId eq corpId) }
    }

    fun saveAgent(doc: Agent) = evict("agent/${doc.corpId}/${doc.id}") {
        runBlocking {
            val old = findAgent(doc.id, doc.corpId)
            if(old == null) agentCol.insertOne(doc)
            else{
                agentCol.replaceOneById(old._id, doc)
            }
        }
    }

    fun addAllowUser(corpId: String, userId: String, agentId: Int) = evict("agent/$corpId/$agentId"){
        runBlocking {
            agentCol.updateOne(and(Agent::corpId eq corpId, Agent::id eq agentId), addToSet(Agent::userList, userId))
        }
    }
    fun addAllowUsers(corpId: String, userIds: List<String>, agentId: Int) = evict("agent/$corpId/$agentId"){
        runBlocking {
            agentCol.updateOne(and(Agent::corpId eq corpId, Agent::id eq agentId), addEachToSet(Agent::userList, userIds))
        }
    }
    fun removeAllowUsers(corpId: String, userIds: List<String>, agentId: Int) = evict("agent/$corpId/$agentId"){
        runBlocking {
            agentCol.updateOne(and(Agent::corpId eq corpId, Agent::id eq agentId), pullAll(Agent::userList, userIds))
        }
    }
    fun removeAllowUser(corpId: String, userId: String, agentId: Int) = evict("agent/$corpId/$agentId"){
        runBlocking {
            agentCol.updateOne(and(Agent::corpId eq corpId, Agent::id eq agentId), pull(Agent::userList, userId))
        }
    }

    fun departmentsToUsers(contactsApi: ContactsApi, departments: Set<Int>?): Set<String>?{
        if(departments == null) return null
        val users = mutableSetOf<String>()
        departments.forEach {
            //是否递归获取子部门下面的成员：1-递归获取，0-只获取本部门
            val res = contactsApi.simpleList(it,1)
            if(res.isOK()){
                res.userList?.forEach{ users.add(it.userId)}
            }
        }
        return users.toSet()
    }
}