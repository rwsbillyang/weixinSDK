/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-11 16:51
 *
 */

package com.github.rwsbillyang.wxOA.msg

import com.github.rwsbillyang.ktorKit.apiBox.UmiPagination
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.ktorKit.toObjectId

import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxOA.wxOaAppModule
import com.github.rwsbillyang.wxSDK.msg.MsgBody
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class MsgService (cache: ICache) : CacheService(cache){
    private val dbSource: MongoDataSource by inject(qualifier = named(wxOaAppModule.dbName!!))


    private val msgCol: CoroutineCollection<MyMsg> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val templateMsgConfigCol: CoroutineCollection<TemplateMsgConfig> by lazy {
        dbSource.mongoDb.getCollection()
    }
    fun findTemplateMsgConfig(appId: String) = cacheable("templateCfg/$appId"){
        runBlocking {
            templateMsgConfigCol.findOneById(appId)
        }
    }


    fun findMyMsg(id: ObjectId) = cacheable("oa.msg/${id.to64String()}"){
        runBlocking {
            msgCol.findOneById(id)
        }
    }

    fun saveMyMsg(doc: MyMsg) = evict("oa.msg/${doc._id.to64String()}") {
        runBlocking {
            msgCol.save(doc)
        }
    }


    fun delMyMsg(id: String) = evict("oa.msg/${id}") {
        runBlocking {
            msgCol.deleteOneById(id.toObjectId())
        }
    }

    fun countMyMsg(filter: Bson) = runBlocking {
        msgCol.countDocuments(filter)
    }

    fun findMyMsgList(filter: Bson, pagination: UmiPagination, lastId: String? ) = runBlocking {
        val sort =  pagination.sortJson.bson
        if(lastId == null)
            msgCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
        else{
            msgCol.find(and(filter,pagination.lastIdFilter(lastId))).limit(pagination.pageSize).sort(sort).toList()
        }
    }
    
    fun findMyMsgListByFlag(appId: String, flag: Long) = runBlocking {
        msgCol.find(MyMsg::appId eq appId, MyMsg::msg / MsgBody::flag bitsAnySet flag ).toList()
    }
}
