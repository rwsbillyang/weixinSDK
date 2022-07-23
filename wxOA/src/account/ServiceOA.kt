/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-10-07 20:06
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


import com.github.rwsbillyang.ktorKit.apiJson.to64String
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.util.DatetimeUtil
import com.github.rwsbillyang.wxUser.account.Account
import com.github.rwsbillyang.wxUser.account.AccountServiceBase
import com.github.rwsbillyang.wxUser.fakeRpc.EditionLevel
import com.mongodb.client.model.FindOneAndUpdateOptions
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.*


//适合于微信公众号
class AccountServiceOA(cache: ICache): AccountServiceBase(cache) {

    fun findByUnionId(unionId: String) = cacheable("u/unionId/$unionId") {
        runBlocking {
            accountCol.findOne(Account::unionId eq unionId)
        }
    }

    fun findByOpenId(openId: String) = cacheable("u/openId/$openId") {
        runBlocking {
            accountCol.findOne(Account::openId1 eq openId)
        }
    }

    /**
     * insert时将返回null
     * */
    fun findUpsertByOpenId(openId: String) = runBlocking {
        accountCol.findOneAndUpdate(
            Account::openId1 eq openId,
            combine(
                setOnInsert(Account::state, Account.STATE_ENABLED),
                setOnInsert(Account::time, System.currentTimeMillis())
            ), FindOneAndUpdateOptions().upsert(true)
        )
    }

    fun insertAccount(doc: Account) = runBlocking{
        accountCol.insertOne(doc)
    }

    fun getOpenId(uId: String): String? {
        return findById(uId)?.openId1
    }

    /**
     * 通过openId获取有效用户的uId
     * */
    fun getUserId(openId: String): String? {
        val a = findByOpenId(openId)
        if(a != null && a.state == Account.STATE_ENABLED)
            return  a._id.to64String()
        return null
    }

//    fun getUserInfo(uId: String): UserInfo? {
//        return findById(uId)?.let {
//            UserInfo(uId, it.oId, permittedLevel(it), it.expire)
//        }
//    }




    private fun upsertLevelAndExpire(_id: String, level: Int, newExpire: Long) = evict("u/id/$_id"){
        runBlocking {
            val modified = accountCol.updateOneById(
                _id, set(
                    SetTo(Account::level, level),
                    SetTo(Account::expire, newExpire)
                ), upsert()
            ).modifiedCount

            modified
        }
    }


    /**
     * days天内过期的列表
     * @param days 0: 今天过期的，1： 明天过期的，2后天过期的，-1：昨天过期的，-2前天过期的
     * */
    fun findExpireInDays(days: Int) = runBlocking {
        val from = DatetimeUtil.getStartMilliSeconds(-days)
        val end = from + DatetimeUtil.msOneDay
        accountCol.find(and(Account::level gt EditionLevel.Free,Account::state eq Account.STATE_ENABLED, Account::expire lt end, Account::expire gte from))
            .toList()
    }




}