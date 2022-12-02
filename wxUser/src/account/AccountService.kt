/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-09-23 10:59
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

package com.github.rwsbillyang.wxUser.account


import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.ktorKit.util.plusTime
import com.github.rwsbillyang.wxUser.wxUserAppModule
import com.mongodb.client.model.FindOneAndUpdateOptions


import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.component.inject

import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection


class AccountService(cache: ICache) : MongoGenericService(cache) {

    private val dbSource: MongoDataSource by inject(qualifier = named(wxUserAppModule.dbName!!))

    private val accountCol: CoroutineCollection<Account> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val groupCol: CoroutineCollection<Group> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun insertOne(doc: Account) = runBlocking { accountCol.insertOne(doc) }

    fun findAccountList(params: AccountListParams): List<AccountBean> {
        return findPage(accountCol, params).map {
            AccountBean(it._id.to64String(), it.state, it.time, it.tel, it.name,
                it.gId?.mapNotNull {
                    val id = it.to64String()
                    findGroup(id)?.let { IdName(id, it.name) }
                }, it.expire, it.profile
            )
        }
    }

    fun findById(id: String) = cacheable("u/id/$id") {
        runBlocking {
            accountCol.findOneById(id.toObjectId())
        }
    }

    fun findOne(id: ObjectId) = cacheable("u/id/${id.to64String()}") {
        runBlocking {
            accountCol.findOneById(id)
        }
    }

    fun findByTel(tel: String) = cacheable("u/tel/$tel") {
        runBlocking {
            accountCol.findOne(Account::tel eq tel)
        }
    }

    fun findByName(name: String) = cacheable("u/name/$name") {
        runBlocking {
            accountCol.findOne(Account::name eq name)
        }
    }



    //for update openId1, openId2 etc.
    fun updateOneById(id: String, update: Bson) = evict("u/id/$id") {
        runBlocking {
            accountCol.updateOneById(id.toObjectId(), update)//setValue(Account::openId1, "")
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



    /**
     * 若未找到用户，返回false
     * 若原用户过期或没有值，则直接在当前基础上添加期限，以及版本level
     * 若原用户未过期，新期限在原基础上相加；但新版本低于原版本，则按新版本更新；
     * 若新购版本高于原版本，在当前时间上添加期限 TODO: 剩余期限折算
     *
     * @param edition 版本level
     * @param year 添加的年限
     * @param month 添加的月
     * @param bonusDays 添加的天数
     * */
    fun calculateNewExpireInfo(
        oldExpireInfo: ExpireInfo?, edition: Int, year: Int,
        month: Int, bonusDays: Int
    ): ExpireInfo {
        val months = (year * 12 + month).toLong()

        val now = System.currentTimeMillis()
        val oldExpire = oldExpireInfo?.expire
        val newExpire = if (oldExpire == null) {
            now.plusTime(months = months, days = bonusDays.toLong())
        } else {
            if (oldExpire < now) {
                now.plusTime(months = months, days = bonusDays.toLong())
            } else {
                //还有剩余期限
                when {
                    edition == oldExpireInfo.level -> oldExpire.plusTime(
                        months = months,
                        days = bonusDays.toLong()
                    )

                    //原仍有剩余期限，期限在原基础上相加 TODO: 折算期限
                    edition < oldExpireInfo.level -> oldExpire.plusTime(
                        months = months,
                        days = bonusDays.toLong()
                    )

                    //新购买版本高于原版本: 原期限作废，按新版本从当前时间计算期限 TODO: 折算期限
                    else -> now.plusTime(
                        months = months,
                        days = bonusDays.toLong()
                    )
                }
            }
        }

        return ExpireInfo(newExpire,  edition)
    }




    fun joinGroup(uId: String, groupId: String) = evict("u/id/$uId") {
        runBlocking {
            accountCol.updateOneById(
                uId.toObjectId(),
                addToSet(Account::gId, groupId.toObjectId())
            ).matchedCount//setValue(Account::openId1, "")
        }
    }


    private fun accountIdToIdName(id: ObjectId?): IdName? {
        if (id == null) return null
        val idStr = id.to64String()
        val a = findById(idStr)
        val name = a?.profile?.nick ?: a?.name
        return IdName(idStr, name)
    }

    fun findGroupList(params: GroupListParams): List<GroupBean> {
        return findPage(groupCol, params).map {
            GroupBean(it._id.to64String(),
                it.name, it.appId,
                it.status,
                it.creator?.to64String(),
                accountIdToIdName(it.creator)?.name,
                it.admins?.mapNotNull { accountIdToIdName(it) },
                it.time
            )
        }
    }

    fun findGroup(id: String) = cacheable("group/$id") {
        runBlocking { groupCol.findOneById(id.toObjectId()) }
    }

    //前端实际传递的数据结构与
    fun saveGroup(bean: GroupBean) = evict("group/${bean._id}") {
        runBlocking {
            if (bean._id == null) {
                val g = Group(ObjectId(), bean.name,bean.appId, bean.status, bean.creator?.toObjectId())
                groupCol.insertOne(g)
                bean._id = g._id.to64String()
            } else {
                groupCol.updateOneById(bean._id!!.toObjectId(), setValue(Group::name, bean.name))
            }
            bean
        }
    }

    fun delGroup(id: String) = evict("group/$id") {
        runBlocking { groupCol.deleteOneById(id.toObjectId()) }
    }


}
