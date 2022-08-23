/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-10 15:46
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.wxOA.fan

import com.github.rwsbillyang.ktorKit.apiBox.UmiPagination

import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxOA.wxOaAppModule
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.koin.core.component.inject

import org.koin.core.qualifier.named
import org.litote.kmongo.bson

import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.include
import org.litote.kmongo.setValue


class FanService(cache: ICache) : CacheService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxOaAppModule.dbName!!))

    /**
     * 关注的粉丝
     * */
    private val fanCol: CoroutineCollection<Fan> by lazy {
        dbSource.mongoDb.getCollection()
    }

    /**
     * 用户信息表
     * */
    private val guestCol: CoroutineCollection<Guest> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val oauthTokenCol: CoroutineCollection<OauthToken> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun saveFan(doc: Fan) = evict("fan/${doc._id}"){
        runBlocking { fanCol.save(doc) }
    }

    //fun delFan(openId: String) = runBlocking { fanCol.deleteOneById(openId) }
    fun subscribeOrUnsubscribe(openId: String, value: Int) = evict("fan/$openId"){
        runBlocking { fanCol.updateOneById(openId, setValue(Fan::sub, value)) }
    }

    fun findFan(openId: String) = cacheable("fan/$openId"){
        runBlocking {
            fanCol.findOneById(openId)
        }
    }
    fun countFan(filter: Bson) = runBlocking{
        fanCol.countDocuments(filter)
    }
    fun findFanList(filter: Bson, pagination: UmiPagination) = runBlocking{
        val sort = pagination.sortJson.bson
        //TODO: skip分页排序性能慢
        fanCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
    }


    fun saveGuest(doc: Guest) = evict("guest/${doc._id}"){
        runBlocking { guestCol.save(doc) }
    }
    fun findGuest(openId: String) = cacheable("guest/$openId"){
        runBlocking { guestCol.findOneById(openId) }
    }


    fun saveOauthToken(doc: OauthToken) = runBlocking { oauthTokenCol.save(doc) }

    fun getSubscribedList(page: Int, pageSize: Int) = runBlocking{
        if(pageSize < 0){
            fanCol.find(Fan::sub eq 1).projection(include(Fan::_id)).toList().map { it._id }
        }else{
            fanCol.find(Fan::sub eq 1).projection(include(Fan::_id)).skip(page * pageSize).limit(pageSize).toList().map { it._id }
        }
    }

}
