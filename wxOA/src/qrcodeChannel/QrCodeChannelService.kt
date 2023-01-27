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

import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxOA.wxOaAppModule
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class QrCodeChannelService(cache: ICache) : MongoGenericService(cache){
    private val dbSource: MongoDataSource by inject(qualifier = named(wxOaAppModule.dbName!!))


    private val channelCol: CoroutineCollection<QrCodeChannel> by lazy {
        dbSource.mongoDb.getCollection()
    }



    fun findList(params: ChannelListParams) = findPage(channelCol, params)

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



    fun saveChannel(doc: QrCodeChannel) = runBlocking {
        if(doc._id == null){
            doc._id = ObjectId()
            channelCol.insertOne(doc)
        }else{
            var update = set(
                SetTo(QrCodeChannel::name, doc.name),
                SetTo(QrCodeChannel::code, doc.code),
                SetTo(QrCodeChannel::remark, doc.remark),
                SetTo(QrCodeChannel::msgId, doc.msgId)
            )
            if(doc.qrCode == null) update = combine(update, setValue(QrCodeChannel::type, doc.type))
            channelCol.updateOneById(doc._id!!, update)
        }
        doc
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

    fun findMsgId(appId: String, code: String) = runBlocking {
        channelCol.findOne(and(QrCodeChannel::appId eq appId, QrCodeChannel::code eq code))?.msgId
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