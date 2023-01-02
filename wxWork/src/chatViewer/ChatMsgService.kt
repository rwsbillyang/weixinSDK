/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-12-30 16:38
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

package com.github.rwsbillyang.wxWork.chatViewer



import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.wxWork.wxWorkModule
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.setValue
import org.litote.kmongo.upsert


class ChatMsgService(cache: ICache) : MongoGenericService(cache) {
    private val dbSource: MongoDataSource by inject(qualifier = named(wxWorkModule.dbName!!))

    private val seqCol: CoroutineCollection<Seq> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val chatMsgCol: CoroutineCollection<ChatMsgRecord> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val chatRoomCol: CoroutineCollection<ChatRoom> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val chatBotCol: CoroutineCollection<ChatBot> by lazy {
        dbSource.mongoDb.getCollection()
    }


    fun findSeq(corpId: String) = runBlocking { seqCol.findOneById(corpId) }
    fun updateSeq(corpId: String,  seq: Long) = runBlocking { seqCol.updateOneById(corpId, setValue(Seq::seq, seq), upsert()) }


    fun saveMsg(msg: ChatMsgRecord) = runBlocking { chatMsgCol.save(msg) }

    fun findChatRoom(id: String) = cacheable("room/$id") {
        runBlocking { chatRoomCol.findOneById(id) }
    }

    fun upsertChatRoom(room: ChatRoom) = evict("room/${room._id}") {
        runBlocking {
            chatRoomCol.replaceOneById(room._id, room, ReplaceOptions().upsert(true))
        }
    }

    fun findChatBot(id: String) = cacheable("bot/$id"){
        runBlocking { chatBotCol.findOneById(id) }
    }
    fun upsertChatBot(bot: ChatBot) = evict("bot/${bot._id}"){
        runBlocking {
            chatBotCol.replaceOneById(bot._id, bot, ReplaceOptions().upsert(true))
        }
    }

    fun countChatMsgList(filter: Bson) = runBlocking {
        chatMsgCol.countDocuments(filter)
    }

    fun findChatMsgList(param: ChatMsgListParam) = findPage(chatMsgCol, param)
}