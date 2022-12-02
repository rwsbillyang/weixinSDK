/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-02-02 17:14
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


package com.github.rwsbillyang.wxSDK.wxMini.account

import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxSDK.wxMini.wxMiniProgramModule
import com.github.rwsbillyang.wxUser.account.ExpireInfo
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

class WxMiniAccountService(cache: ICache): MongoGenericService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxMiniProgramModule.dbName!!))

    private val wxMiniAccountCol: CoroutineCollection<WxMiniAccount> by lazy {
        dbSource.mongoDb.getCollection()
    }

//    private val wxMiniAccountRecommendCol: CoroutineCollection<Recommend> by lazy {
//        dbSource.mongoDb.getCollection("wxMiniRecommend")
//    }

    fun findWxMiniAccount(id: ObjectId) = runBlocking { wxMiniAccountCol.findOneById(id) }
    fun findWxMiniAccount(unionId: String?, openId: String?, appId: String) = runBlocking {
        if(unionId != null)
            wxMiniAccountCol.findOne(WxMiniAccount::unionId eq unionId)
        else
            wxMiniAccountCol.findOne(WxMiniAccount::openId eq openId, WxMiniAccount::appId eq appId)
    }

    fun insertMiniAccount(doc: WxMiniAccount) = runBlocking {
        wxMiniAccountCol.insertOne(doc)
    }

    fun updateSystemAccountId(id: ObjectId, systemAccountId: String) = runBlocking {
        wxMiniAccountCol.updateOneById(id,  setValue(WxMiniAccount::sysId, systemAccountId.toObjectId()),)
    }

    fun updateExpireInfo(id: ObjectId, expire: ExpireInfo) = runBlocking {
        wxMiniAccountCol.updateOneById(id,  setValue(WxMiniAccount::expire, expire),)
    }


//    fun insertRecommend(doc: Recommend) = runBlocking {
//        wxMiniAccountRecommendCol.insertOne(doc)
//    }

    fun findWxAccountList(params: WxMiniAccountListParams) = findPage(wxMiniAccountCol, params)
}