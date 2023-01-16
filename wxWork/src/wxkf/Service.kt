/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-05 12:00
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

package com.github.rwsbillyang.wxWork.wxkf

import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoCRUDService
import com.github.rwsbillyang.wxWork.wxWorkModule
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.setValue
import org.litote.kmongo.upsert

class WxkfService(cache: ICache): MongoCRUDService(cache, wxWorkModule.dbName!!) {

    val wxkfCursorCol: CoroutineCollection<WxkfMsgCursor> by lazy {
        dbSource.mongoDb.getCollection()
    }
    val wxkfMsgCol: CoroutineCollection<WxkfMsg> by lazy {
        dbSource.mongoDb.getCollection()
    }
    val wxkfServicerCol: CoroutineCollection<WxkfServicer> by lazy {
        dbSource.mongoDb.getCollection()
    }
    fun insertMsgList(list: List<WxkfMsg>) = runBlocking {
        wxkfMsgCol.insertMany(list)
    }
    fun findCursor(kfid: String) = runBlocking {
        val c = cache[kfid]
        if(c != null) c as String
        else wxkfCursorCol.findOneById(kfid)?.cursor
    }
    fun upsertCursor(kfid: String, cursor: String) = runBlocking {
        cache.put(kfid, cursor)
        wxkfCursorCol.updateOneById(kfid, setValue(WxkfMsgCursor::cursor, cursor), upsert())
    }

    fun findWxkfServicerCol(kfid: String) = runBlocking {
        wxkfServicerCol.findOneById(kfid)
    }

}