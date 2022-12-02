/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-30 00:19
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

package com.github.rwsbillyang.wxOA.account

import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxOA.wxOaAppModule
import com.github.rwsbillyang.wxUser.account.ExpireInfo
import com.github.rwsbillyang.wxUser.account.Recommend
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

class WxOaAccountService(cache: ICache): MongoGenericService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxOaAppModule.dbName!!))

    private val wxOaAccountCol: CoroutineCollection<WxOaAccount> by lazy {
        dbSource.mongoDb.getCollection()
    }

    private val wxOaAccountRecommendCol: CoroutineCollection<Recommend> by lazy {
        dbSource.mongoDb.getCollection("wxOaRecommend")
    }

    fun findWxOaAccount(id: ObjectId) = runBlocking { wxOaAccountCol.findOneById(id) }
    fun findWxOaAccount(unionId: String?, openId: String?, appId: String) = runBlocking {
        if(unionId != null)
            wxOaAccountCol.findOne(WxOaAccount::unionId eq unionId)
        else
            wxOaAccountCol.findOne(WxOaAccount::openId eq openId, WxOaAccount::appId eq appId)
    }

    fun insertOaAccount(doc: WxOaAccount) = runBlocking {
        wxOaAccountCol.insertOne(doc)
    }

    fun updateSystemAccountId(id: ObjectId, systemAccountId: String) = runBlocking {
        wxOaAccountCol.updateOneById(id,  setValue(WxOaAccount::sysId, systemAccountId.toObjectId()),)
    }

    fun updateExpireInfo(id: ObjectId, expire: ExpireInfo) = runBlocking {
        wxOaAccountCol.updateOneById(id,  setValue(WxOaAccount::expire, expire),)
    }


    fun insertRecommend(doc: Recommend) = runBlocking {
        wxOaAccountRecommendCol.insertOne(doc)
    }

    fun findWxAccountList(params: WxOaAccountListParams) = findPage(wxOaAccountCol, params)
}