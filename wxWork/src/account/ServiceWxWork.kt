///*
// * Copyright © 2021 rwsbillyang@qq.com
// *
// * Written by rwsbillyang@qq.com at Beijing Time: 2021-10-07 20:15
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.github.rwsbillyang.wxWork.account
//
//
//import com.github.rwsbillyang.ktorKit.toObjectId
//import com.github.rwsbillyang.ktorKit.cache.ICache
//import com.github.rwsbillyang.ktorKit.util.DatetimeUtil
//
//import com.github.rwsbillyang.wxUser.account.Account
//import com.github.rwsbillyang.wxUser.account.AccountExpire
//import com.github.rwsbillyang.wxUser.account.AccountServiceBase
//import com.github.rwsbillyang.wxUser.account.ExpireInfo
//import com.github.rwsbillyang.wxUser.fakeRpc.EditionLevel
//import com.mongodb.client.model.FindOneAndUpdateOptions
//import kotlinx.coroutines.runBlocking
//import org.litote.kmongo.*
//import org.litote.kmongo.coroutine.CoroutineCollection
//
//
////适合于企业微信
//class AccountServiceWxWork(cache: ICache): AccountServiceBase(cache) {
//    private val accountExpireCol: CoroutineCollection<AccountExpire> by lazy {
//        dbSource.mongoDb.getCollection()
//    }
//
//    //优先使用openId查找，为空的话才使用userId，因为userId可以被修改变化
//    fun findByCorpIdUserId(corpId: String, userId: String, openId: String?) = runBlocking {
//        if(openId != null)
//            accountCol.findOne(Account::openId2 eq openId,Account::corpId eq corpId)
//        else
//            accountCol.findOne(Account::userId eq userId, Account::corpId eq corpId)
//    }
//
//    fun insert(doc: Account) = runBlocking {
//        accountCol.insertOne(doc)
//    }
//
//    fun findUpsertByCorpIdUserId(corpId: String, userId: String) = runBlocking {
//        accountCol.findOneAndUpdate(
//            and(Account::corpId eq corpId,Account::userId eq userId),
//            combine(
//                setOnInsert(Account::state, Account.STATE_ENABLED),
//                setOnInsert(Account::time, System.currentTimeMillis())
//            ), FindOneAndUpdateOptions().upsert(true)
//        )
//    }
//
//    //找到则返回，未找到则返回null，同时插入
//    fun findUpsert(corpId: String, openId: String?, userId: String, deviceId: String?) = runBlocking {
//        val filter = if(openId != null){
//            and(Account::corpId eq corpId, Account::openId2 eq openId)
//        }else{
//            and(Account::corpId eq corpId, Account::userId eq userId)
//        }
//        accountCol.findOneAndUpdate(
//            filter,
//            combine(
//                setValue(Account::userId, userId),
//                setOnInsert(Account::corpId, corpId),
//                setOnInsert(Account::openId2, openId),
//                setOnInsert(Account::deviceId, deviceId),
//                setOnInsert(Account::state, Account.STATE_ENABLED),
//                setOnInsert(Account::time, System.currentTimeMillis())
//            ), FindOneAndUpdateOptions().upsert(true)
//        )
//    }
//
//
//
//
//    fun findAccountExpireById(_id: String) = cacheable("accountExpire/$_id"){
//        runBlocking {
//            accountExpireCol.findOneById(_id)
//        }
//    }
//
//    /**
//     * days天内过期的列表 只基于AccountExpire中的信息，不会搜索Account中的信息
//     * @param days 0: 今天过期的，1： 明天过期的，2后天过期的，-1：昨天过期的，-2前天过期的
//     * */
//    fun findExpireInDays(days: Int) = runBlocking {
//        val from = DatetimeUtil.getStartMilliSeconds(-days)
//        val end = from + DatetimeUtil.msOneDay
//        accountExpireCol.find(and(AccountExpire::level gt EditionLevel.Free, AccountExpire::expire lt end, AccountExpire::expire gte from))
//            .toList()
//    }
//
//
//    //优先使用AccountExpire中的权限信息，没有则使用Account中的信息
//    override fun getExpireInfo(accountId: String, agentId: Int?): ExpireInfo? {
//        return findAccountExpireById(AccountExpire.id(accountId, agentId))?.toExpireInfo()?:findById(accountId)?.toExpireInfo()
//    }
//
//    //若agentId非空，则使用AccountExpire保存，否则直接保存在Account中
//    override fun updateExpireInfo(accountId: String, agentId: Int?, edition: Int, newExpire: Long)= evict("u/id/$accountId"){
//        runBlocking {
//            if(agentId != null ){
//                accountExpireCol.updateOneById(
//                    AccountExpire.id(accountId, agentId), set(
//                        SetTo(AccountExpire::level, edition),
//                        SetTo(AccountExpire::expire, newExpire)
//                    ), upsert()
//                )
//            }else{
//                accountCol.updateOneById(accountId.toObjectId(), combine(
//                    setValue(Account::level, edition),
//                    setValue(Account::expire, newExpire)
//                ))
//            }
//
//        }
//    }
//
//}