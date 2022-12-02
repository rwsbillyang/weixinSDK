/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-29 11:44
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

package com.github.rwsbillyang.wxWork.account

import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxUser.account.ExpireInfo
import com.github.rwsbillyang.wxUser.account.Recommend
import com.github.rwsbillyang.wxWork.wxWorkModule
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

class WxWorkAccountService(cache: ICache): MongoGenericService(cache) {

    private val dbSource: MongoDataSource by inject(qualifier = named(wxWorkModule.dbName!!))

    private val wxWorkAccountCol: CoroutineCollection<WxWorkAccount> by lazy {
        dbSource.mongoDb.getCollection()
    }

    private val wxWorkAccountRecommendCol: CoroutineCollection<Recommend> by lazy {
        dbSource.mongoDb.getCollection("wxWorkRecommend")
    }

    fun findWxWorkAccount(id: ObjectId) = runBlocking { wxWorkAccountCol.findOneById(id) }
    fun findWxWorkAccount(corpId: String, userId: String, openId: String?) = runBlocking {
        if(openId != null)
            wxWorkAccountCol.findOne(WxWorkAccount::openId eq openId, WxWorkAccount::corpId eq corpId)
        else
            wxWorkAccountCol.findOne(WxWorkAccount::userId eq userId, WxWorkAccount::corpId eq corpId)
    }

    fun insertWorkAccount(doc: WxWorkAccount) = runBlocking {
        wxWorkAccountCol.insertOne(doc)
    }

    fun updateSystemAccountId(id: ObjectId, systemAccountId: String) = runBlocking {
        wxWorkAccountCol.updateOneById(id,  setValue(WxWorkAccount::sysId, systemAccountId.toObjectId()),)
    }

    fun updateExpireInfo(id: ObjectId, expire: ExpireInfo) = runBlocking {
        wxWorkAccountCol.updateOneById(id,  setValue(WxWorkAccount::expire, expire),)
    }


    fun insertRecommend(doc: Recommend) = runBlocking {
        wxWorkAccountRecommendCol.insertOne(doc)
    }

    fun findWxAccountList(params: WxAccountListParams) = findPage(wxWorkAccountCol, params)
}