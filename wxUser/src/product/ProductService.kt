/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-30 20:16
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

package com.github.rwsbillyang.wxUser.product

import com.github.rwsbillyang.ktorKit.apiJson.to64String
import com.github.rwsbillyang.ktorKit.apiJson.toObjectId
import com.github.rwsbillyang.ktorKit.DataSource
import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.wxUser.wxUserAppModule
import kotlinx.coroutines.runBlocking
import org.koin.core.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.and
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

class ProductService(cache: ICache) : CacheService(cache){
    private val dbSource: DataSource by inject(qualifier = named(wxUserAppModule.dbName!!))

    private val col: CoroutineCollection<Product> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun list(tag: String?, type: Int, status: Int?) = runBlocking {
        val f0 = if(tag != null ) Product::tag eq tag else null
        val f1 = if(status != null)  Product::status eq status else null
        val f2 = Product::type eq type
        col.find(and(f0, f1,f2)).toList()
    }
    fun add(doc: Product) = runBlocking {
        col.insertOne(doc)
    }
    fun update(doc: Product) = evict("product/${doc._id.to64String()}"){
        runBlocking {
            col.save(doc)
        }
    }
    fun del(id: String) = evict("product/$id"){
        runBlocking { col.deleteOneById(id.toObjectId()) }
    }

    fun findOne(id: String) = cacheable("product/$id"){
        runBlocking { col.findOneById(id.toObjectId())}
    }
}