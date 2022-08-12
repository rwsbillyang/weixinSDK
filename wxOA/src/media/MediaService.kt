/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-21 21:07
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

package com.github.rwsbillyang.wxOA.media

import com.github.rwsbillyang.ktorKit.apiJson.UmiPagination

import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxOA.wxOaAppModule
import com.github.rwsbillyang.wxSDK.officialAccount.Article
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.bson
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.exclude


class MediaService  (cache: ICache) : CacheService(cache){

    private val dbSource: MongoDataSource by inject(qualifier = named(wxOaAppModule.dbName!!))


    private val newsCol: CoroutineCollection<MaterialNews> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val videoCol: CoroutineCollection<MaterialVideo> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val mediaCol: CoroutineCollection<MaterialMedia> by lazy {
        dbSource.mongoDb.getCollection()
    }

    private val articleDetailCol: CoroutineCollection<ArticleDetail> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun dropNewsCol() = runBlocking { newsCol.drop() }
    fun dropVideoCol() = runBlocking { videoCol.drop() }
    fun dropMediaCol() = runBlocking { mediaCol.drop() }

    fun insertMediaList(list: List<MaterialMedia>) = runBlocking{
        mediaCol.insertMany(list)
    }
    fun insertVideoList(list: List<MaterialVideo>) = runBlocking{
        videoCol.insertMany(list)
    }
    fun insertNewsMediaList(list: List<MaterialNews>) = runBlocking{
        newsCol.insertMany(list)
    }
    fun insertArticleDetail(doc: ArticleDetail) = runBlocking {
        articleDetailCol.insertOne(doc)
    }
    fun findArticleDetail(sha1: String) = cacheable(sha1){
        runBlocking { articleDetailCol.findOne(ArticleDetail::sha1 eq sha1) }
    }


    fun findAllNews() = runBlocking{
        newsCol.find().projection(exclude(Article::content)).toList()
    }
    fun findAllVideo() = runBlocking{
        videoCol.find().toList()
    }
    fun findAllByType(type: String) = runBlocking{
        mediaCol.find(MaterialMedia::type eq type).toList()
    }

    fun countNews(filter: Bson) = runBlocking  {
        newsCol.countDocuments(filter)
    }
    fun countVideo(filter: Bson) = runBlocking  {
        videoCol.countDocuments(filter)
    }
    fun countMedia(filter: Bson) = runBlocking  {
        mediaCol.countDocuments(filter)
    }

    fun findNewsPage(filter: Bson, pagination: UmiPagination) = runBlocking {
        val sort = pagination.sortJson.bson
        //TODO: skip分页排序性能慢
        newsCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
    }

    fun findVideoPage(filter: Bson, pagination: UmiPagination) = runBlocking {
        val sort = pagination.sortJson.bson
        //TODO: skip分页排序性能慢
        videoCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
    }

    fun findMediaPage(filter: Bson, pagination: UmiPagination) = runBlocking {
        val sort = pagination.sortJson.bson
        //TODO: skip分页排序性能慢
        mediaCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
    }

    fun delNewsById(id: String) = runBlocking{
        newsCol.deleteOneById(id)
    }

    fun delVideoById(id: String) = runBlocking {
        videoCol.deleteOneById(id)
    }

    fun delMediaById(id: String) = runBlocking{
        mediaCol.deleteOneById(id)
    }

}
