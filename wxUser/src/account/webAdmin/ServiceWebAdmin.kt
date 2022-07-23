/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-10-07 20:11
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

package com.github.rwsbillyang.wxUser.account.webAdmin

import com.github.rwsbillyang.ktorKit.apiJson.toObjectId
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.wxUser.account.Account
import com.github.rwsbillyang.wxUser.account.AccountServiceBase
import com.github.rwsbillyang.wxUser.account.Profile
import com.mongodb.client.model.FindOneAndUpdateOptions
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection


/**
 * 普通网页的账户密码，手机号
 * */
class AccountServiceWebAdmin(cache: ICache): AccountServiceBase(cache) {
    private val profileCol: CoroutineCollection<Profile> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun findByTel(tel: String) = cacheable("u/tel/$tel") {
        runBlocking {
            accountCol.findOne(Account::tel eq tel)
        }
    }

    /**
     * insert时将返回null
     * */
    fun findUpsertByTel(tel: String) = runBlocking {
        accountCol.findOneAndUpdate(
            Account::tel eq tel,
            combine(
                setOnInsert(Account::state, Account.STATE_ENABLED),
                setOnInsert(Account::time, System.currentTimeMillis())
            ), FindOneAndUpdateOptions().upsert(true)
        )
    }

    fun findByName(name: String) = cacheable("u/name/$name") {
        runBlocking {
            accountCol.findOne(Account::name eq name)
        }
    }

    /**
     * insert时将返回null
     * */
    fun findUpsertByName(name: String, encryptPwd: String?, salt: String?) = runBlocking {
        accountCol.findOneAndUpdate(
            Account::name eq name,
            combine(
                setOnInsert(Account::pwd, encryptPwd),
                setOnInsert(Account::salt, salt),
                setOnInsert(Account::state, Account.STATE_ENABLED),
                setOnInsert(Account::time, System.currentTimeMillis())
            ), FindOneAndUpdateOptions().upsert(true)
        )
    }



    fun updatePwdAndSalt(account: Account, pwd: String?, salt: String?) = evict("u/name/${account.name}") {
        runBlocking {
            accountCol.updateOneById(account._id, set(SetTo(Account::pwd, pwd), SetTo(Account::salt, salt)))
        }
    }


    fun findProfile(uId: String) = cacheable("profile/$uId") {
        runBlocking { profileCol.findOneById(uId.toObjectId()) }
    }



}