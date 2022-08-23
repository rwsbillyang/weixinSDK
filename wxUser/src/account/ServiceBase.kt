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
import com.github.rwsbillyang.ktorKit.apiBox.UmiPagination
import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.util.plusTime
import com.github.rwsbillyang.wxUser.fakeRpc.EditionLevel


import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.component.inject

import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection


abstract class AccountServiceBase(cache: ICache) : CacheService(cache) {
    companion object{
        /**
         * 用于支持配置到单独的db中，如各个agent共享的account。注：登录统计数据配置到agent自己的库中
         * */
        var AccountDbName = "user"
    }
    protected val dbSource: MongoDataSource by inject(qualifier = named(AccountDbName))

    protected val accountCol: CoroutineCollection<Account> by lazy {
        dbSource.mongoDb.getCollection()
    }
    protected val groupCol: CoroutineCollection<Group> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun findAccountList(filter: Bson, pagination: UmiPagination, lastId: String?): List<AccountBean> = runBlocking {
        val sort =  pagination.sortJson.bson
        if(lastId == null)
            accountCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
        else{
            accountCol.find(and(filter,pagination.lastIdFilter(lastId))).limit(pagination.pageSize).sort(sort).toList()
        }.map { AccountBean(it._id.to64String(),it.state,it.time,it.tel,it.name,it.openId1,it.needUserInfo,
            it.appId,it.userId, it.corpId, it.suiteId, it.openId2, it.expire, it.role, it.level,
            it.gId?.mapNotNull {
                val id = it.to64String()
                findGroup(id)?.let { IdName(id, it.name) }
            }, it.ext) }
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
    /**
     * 通过前端企业微信jssdk中的选人，得到内部联系人userId，然后再获取account及其_id
     * */
    fun findByCorpIdAndUserId(corpId: String, userId: String) = runBlocking {
        accountCol.findOne(Account::userId eq userId, Account::corpId eq corpId)
    }

    //for update openId1, openId2 etc.
    fun updateOneById(id: String, update: Bson) = evict("u/id/$id"){
        runBlocking {
            accountCol.updateOneById(id.toObjectId(), update)//setValue(Account::openId1, "")
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
    fun updateAccountExpiration(
        accountId: String, agentId: Int?, edition: Int, year: Int,
        month: Int, bonusDays: Int
    ): Long {
        val oldAccountExpire = getExpireInfo(accountId, agentId)
        val months = (year * 12 + month).toLong()

        val now = System.currentTimeMillis()
        val oldExpire = oldAccountExpire?.expire
        val newExpire = if (oldExpire == null) {
            now.plusTime(months = months, days = bonusDays.toLong())
        } else {
            if (oldExpire < now) {
                now.plusTime(months = months, days = bonusDays.toLong())
            } else {
                //还有剩余期限
                when {
                    edition == oldAccountExpire.level -> oldExpire.plusTime(
                        months = months,
                        days = bonusDays.toLong()
                    )

                    //原仍有剩余期限，期限在原基础上相加 TODO: 折算期限
                    edition < oldAccountExpire.level -> oldExpire.plusTime(
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


        updateExpireInfo(accountId, agentId, edition, newExpire)
        return newExpire
    }

    //wxWork各agent若有自己的权限信息需要重载
    open fun getExpireInfo(accountId: String, agentId: Int?): ExpireInfo? {
        return findById(accountId)?.toExpireInfo()
    }

    //wxWork各agent若有自己的权限信息需要重载
    open fun updateExpireInfo(accountId: String, agentId: Int?, edition: Int, newExpire: Long)= evict("u/id/$accountId"){
        runBlocking {
            accountCol.updateOneById(accountId.toObjectId(), combine(
                setValue(Account::level, edition),
                setValue(Account::expire, newExpire)
            ))//setValue(Account::openId1, "")
        }
    }


    /**
     * 检查用户权限后的重新设置后的level
     * */
    fun permittedLevel(uId: String?, agentId: Int?): Int {
        if (uId == null) return EditionLevel.Free

        val ae = getExpireInfo(uId, agentId)
        if(ae == null) return EditionLevel.Free
        else {
            if (ae.expire == null) return EditionLevel.Free
            val now = System.currentTimeMillis()
            return if (ae.expire < now) EditionLevel.Free else ae.level
        }
    }
    fun joinGroup(uId:String, groupId: String)  = evict("u/id/$uId"){
        runBlocking {
            accountCol.updateOneById(uId.toObjectId(), addToSet(Account::gId, groupId.toObjectId())).matchedCount//setValue(Account::openId1, "")
        }
    }


    fun findGroupList(filter: Bson, pagination: UmiPagination, lastId: String?): List<GroupBean> = runBlocking {
        val sort =  pagination.sortJson.bson
        if(lastId == null)
            groupCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
        else{
            groupCol.find(and(filter,pagination.lastIdFilter(lastId))).limit(pagination.pageSize).sort(sort).toList()
        }.map { GroupBean(it._id.to64String(), it.name, it.status, it.appId, it.corpId,
            it.creator?.to64String(), accountIdToIdName(it.creator)?.name, it.admins?.mapNotNull { accountIdToIdName(it) }, it.time ) }
    }


    private fun accountIdToIdName(id: ObjectId?):IdName?{
        if(id == null) return null
        val idStr = id.to64String()
        val a = findById(idStr)
        //TODO: OfficialAccount时获取fanInfo得到name
        val name = if(a!=null) a.name?:a.userId?:a.openId1?:"" else ""
        return IdName(idStr, name)
    }

    fun findGroup(id: String) = cacheable("group/$id"){
        runBlocking { groupCol.findOneById(id.toObjectId()) }
    }

    //前端实际传递的数据结构与
    fun saveGroup(bean: GroupBean) = evict("group/${bean._id}"){
        runBlocking {
            if(bean._id == null){
                val g = Group(ObjectId(), bean.name, bean.status,bean.appId, bean.corpId, bean.creator?.toObjectId())
                groupCol.insertOne(g)
                bean._id = g._id.to64String()
            }else{
                groupCol.updateOneById(bean._id!!.toObjectId(), setValue(Group::name, bean.name))
            }
            bean
        }
    }
    fun delGroup(id: String)= evict("group/$id"){
        runBlocking { groupCol.deleteOneById(id.toObjectId()) }
    }


}
