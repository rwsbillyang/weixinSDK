/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-30 22:02
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

package com.github.rwsbillyang.wxUser.order

import com.github.rwsbillyang.ktorKit.apiJson.Sort
import com.github.rwsbillyang.ktorKit.apiJson.UmiPagination
import com.github.rwsbillyang.ktorKit.apiJson.to64String
import com.github.rwsbillyang.ktorKit.apiJson.toObjectId

import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxUser.wxUserAppModule
import com.github.rwsbillyang.wxSDK.wxPay.OrderPayDetail
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class AccountOrderService(cache: ICache) : CacheService(cache){
    private val dbSource: MongoDataSource by inject(qualifier = named(wxUserAppModule.dbName!!))

    private val orderCol: CoroutineCollection<AccountOrder> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun count(filter: Bson) = runBlocking { orderCol.countDocuments(filter) }
    fun findList(filter: Bson, pagination: UmiPagination, lastId: ObjectId?) = runBlocking {
        val sort = pagination.sortJson.bson
        if(lastId == null)
            orderCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
        else{
            val f = if(pagination.sort == Sort.ASC) AccountOrder::_id gt lastId
            else AccountOrder::_id lt lastId
            orderCol.find(and(filter,f)).limit(pagination.pageSize).sort(sort).toList()
        }
    }

    fun insert(order: AccountOrder) = cacheable("order/${order._id.to64String()}") {
        runBlocking {
            orderCol.insertOne(order)
            order
        }
    }

    fun findOne(id: String) = cacheable("order/$id"){
        runBlocking { orderCol.findOneById(id.toObjectId()) }
    }
    fun updateOrder(id: ObjectId, status: Int) = evict("order/${id.to64String()}"){
        runBlocking{
            orderCol.updateOneById(id, setValue(AccountOrder::status, status))
        }
    }
    fun updateOrder(id: ObjectId, status: Int?, orderPayDetail: OrderPayDetail)  = evict("order/${id.to64String()}")
    {
        runBlocking {
            if (status != null) {
                orderCol.updateOneById(
                    id, set(
                        SetTo(AccountOrder::status, status), SetTo(
                            AccountOrder::orderPayDetail,
                            orderPayDetail
                        )
                    )
                )
            } else {
                orderCol.updateOneById(id, setValue(AccountOrder::orderPayDetail, orderPayDetail))
            }
        }
    }
}