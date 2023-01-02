/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-31 21:58
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

package com.github.rwsbillyang.wxWork.channel


import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxWork.wxWorkModule
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.SetTo
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.set


class ChannelService(cache: ICache) : MongoGenericService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxWorkModule.dbName!!))

    private val channelCol: CoroutineCollection<Channel> by lazy {
        dbSource.mongoDb.getCollection()
    }
    /**
     * 某个内部成员用户的所有渠道
     * @param corpId: 企业id
     * @param userId 特定某个成员用户 为null时表示不指定成员，也就是所有渠道
     * */
    fun list(corpId: String, userId: String?) = runBlocking{
        if(userId == null)
            channelCol.find(Channel::corpId eq corpId).toList()
        else
            channelCol.find(Channel::corpId eq corpId, Channel::userId eq userId).toList()
    }

    fun findList(params: ChannelListParams): List<Channel> = findPage(channelCol, params)

    /**
     * 获取总数量用于分页
     * */
    fun count(filter: Bson) = runBlocking {
        channelCol.countDocuments(filter)
    }

    fun findOne(id: String) = cacheable(id){
        runBlocking {
            channelCol.findOneById(id.toObjectId())
        }
    }

    fun addChannel(doc: Channel) = runBlocking{
        doc._id = ObjectId()
        channelCol.insertOne(doc)
        doc
    }

    fun updateChannel(info: Channel) = evict(info._id!!.to64String()){
        runBlocking {
            channelCol.updateOneById(info._id!!, set(
                SetTo(Channel::name, info.name),
                SetTo(Channel::tel, info.tel),
                SetTo(Channel::remark, info.remark)
            ))
            info
        }
    }

    fun updateQrcode(id: ObjectId, qrcode: String, configId: String) = evict(id.to64String()){
        runBlocking {
            channelCol.updateOneById(id, set(
                SetTo(Channel::qrCode, qrcode),
                SetTo(Channel::configId, configId)
            ))
        }
    }

    fun delChannel(id: String) = evict(id){
        runBlocking {
            channelCol.deleteOneById(id.toObjectId())
        }
    }


}