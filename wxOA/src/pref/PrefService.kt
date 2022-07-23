/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-11 16:58
 *
 */

package com.github.rwsbillyang.wxOA.pref


import com.github.rwsbillyang.ktorKit.apiJson.UmiPagination
import com.github.rwsbillyang.ktorKit.apiJson.toObjectId
import com.github.rwsbillyang.ktorKit.DataSource
import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.wxOA.wxOaAppModule
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class PrefService(cache: ICache) : CacheService(cache){
    private val dbSource: DataSource by inject(qualifier = named(wxOaAppModule.dbName!!))


    private val officialAccountCol: CoroutineCollection<PrefOfficialAccount> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val prefReInMsgCol: CoroutineCollection<PrefReInMsg> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val prefMenuCol: CoroutineCollection<PrefMenu> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun findOfficialAccount(appId: String): PrefOfficialAccount? = cacheable("oa/${appId}"){
        runBlocking{
            officialAccountCol.findOneById(appId)
        }
    }
    fun findOfficialAccounts() = runBlocking{
        officialAccountCol.find(PrefOfficialAccount::enable eq true).toList()
    }
    fun delOfficialAccount(appId: String)= evict("oa/${appId}"){
        runBlocking{
            officialAccountCol.deleteOneById(appId)
        }
    }

    fun saveOfficialAccount(doc: PrefOfficialAccount) = evict("oa/${doc._id}") {
        runBlocking {
            officialAccountCol.save(doc)
        }
    }

    fun findPrefMenus(filter: Bson?)  = runBlocking {
        prefMenuCol.find(filter).toList()
    }

    fun findPrefMenuByAppId(appId: String) = runBlocking { prefMenuCol.find(PrefMenuTree::appId eq appId).toList() }
    fun savePrefMenu(doc: PrefMenu) = runBlocking {
        prefMenuCol.save(doc)
    }
    fun delPrefMenu(id: ObjectId) = runBlocking { prefMenuCol.deleteOneById(id) }
    fun delPrefMenuChildren(pId: ObjectId, appId: String) = runBlocking {
        prefMenuCol.deleteMany(and(PrefMenu::pId eq pId, PrefMenu::appId eq appId)  )
    }


    fun savePrefReInMsg(doc: PrefReInMsg) = evict("reInMsg/${doc.appId}/${doc.type}") {
        runBlocking {
            prefReInMsgCol.save(doc)
        }
    }


    fun delPrefReInMsg(id: String) = runBlocking {
        prefReInMsgCol.deleteOneById(id.toObjectId())
    }
    fun findPrefReInMsg(appId: String, type: String) =  runBlocking { prefReInMsgCol.findOne(and(PrefReInMsg::appId eq appId, PrefReInMsg::type eq type))  }


    fun countPrefReMsgList(f: Bson): Long  = runBlocking {
        prefReInMsgCol.countDocuments(f)
    }

    fun findPrefReMsgList(f: Bson, pagination: UmiPagination) = runBlocking {
        //val sort = pagination.sKey?.let { "{${pagination.sKey}:${pagination.sort}}".bson }?:"{_id:-1}".bson
        val sort = pagination.sortJson.bson
        prefReInMsgCol.find(f).skip(pagination.pageSize * (pagination.current-1)).limit(pagination.pageSize).sort(sort).toList()
    }
    fun findPrefReMsgList(appId: String, cat: Int) = runBlocking {
        prefReInMsgCol.find(and(PrefReInMsg::appId eq appId, PrefReInMsg::cat eq cat) ).toList()
    }



}
