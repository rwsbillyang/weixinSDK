/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-11-23 13:45
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

package com.github.rwsbillyang.wxOA.qrcodeChannel

import com.github.rwsbillyang.ktorKit.apiBox.UmiPagination
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.ktorKit.toObjectId

import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.wxOA.wxOaAppModule

import com.mongodb.client.model.ReturnDocument
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.component.inject

import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class QrCodeChannelService(cache: ICache) : CacheService(cache){
    private val dbSource: MongoDataSource by inject(qualifier = named(wxOaAppModule.dbName!!))


    private val channelCol: CoroutineCollection<QrCodeChannel> by lazy {
        dbSource.mongoDb.getCollection()
    }



    fun findList(filter: Bson, pagination: UmiPagination, lastId: String? ): List<QrCodeChannel> = runBlocking {
        val sort =  pagination.sortJson.bson
        if(lastId == null)
            channelCol.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
        else{
            channelCol.find(and(filter,pagination.lastIdFilter(lastId))).limit(pagination.pageSize).sort(sort).toList()
        }
    }

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

    fun addChannel(bean: ChannelBean) = runBlocking{
        val doc = QrCodeChannel(ObjectId(), bean.appId, bean.name, bean.code, bean.type, bean.remark,null, null,System.currentTimeMillis())
        channelCol.insertOne(doc)
        doc
    }

    /**
     * @param isUpdateType 如果qrCode没有值，才更新type，一旦生成二维码将不再更新type
     * */
    fun updateChannel(info: ChannelBean, isUpdateType: Boolean) = evict(info._id!!){
        runBlocking {
            var update = set(
                SetTo(QrCodeChannel::name, info.name),
                SetTo(QrCodeChannel::code, info.code),
                SetTo(QrCodeChannel::remark, info.remark)
            )
            if(isUpdateType) update = combine(update, setValue(QrCodeChannel::type, info.type))
            channelCol.findOneAndUpdate(QrCodeChannel::_id eq info._id.toObjectId(), update,
                findOneAndUpdateUpsert().returnDocument(ReturnDocument.AFTER))
        }
    }

    fun updateQrcode(id: ObjectId, qrcode: String, imgUrl: String?) = evict(id.to64String()){
        runBlocking {
            channelCol.updateOneById(id, set(
                SetTo(QrCodeChannel::qrCode, qrcode),
                SetTo(QrCodeChannel::imgUrl, imgUrl)
            )
            )
        }
    }

    fun delChannel(id: String, appId: String?) = evict(id){
        runBlocking {
            if(appId == null)
                channelCol.deleteOneById(id.toObjectId())
            else
                channelCol.deleteOne(QrCodeChannel::_id eq id.toObjectId(), QrCodeChannel::appId eq appId)
        }
    }
}