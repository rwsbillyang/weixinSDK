/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:38
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

package com.github.rwsbillyang.wxSDK.work

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * userId userid	是	日历共享成员的id
 * */
@Serializable
data class ShareUser(@SerialName("userid") val userId: String)

/**
 * organizer	是	指定的组织者userid。注意该字段指定后不可更新
 * summary	是	日历标题。1 ~ 128 字符
 * color	是	日历在终端上显示的颜色，RGB颜色编码16进制表示，例如：”#0000FF” 表示纯蓝色
 * description	否	日历描述。0 ~ 512 字符
 * shares	否	日历共享成员列表。最多2000人
 * */
@Serializable
data class Calendar(
    val organizer: String,
    val summary: String,
    val color: String,
    val description: String?,
    val shares: List<ShareUser>?,
    @SerialName("cal_id") val id: String? = null
)

class CalendarApi(corpId: String?, agentId: String?, suiteId: String?)
    : WorkBaseApi(corpId, agentId,suiteId)
{

    override val group = "oa/calendar"

    fun add(calendar: Calendar) = doPostRaw("add", mapOf("calendar" to calendar))

    /**
     * 更新日历
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/92619
     * */
    fun update(calendar: Calendar) = doPostRaw("update", mapOf("calendar" to calendar))


    /**
     * 获取日历
     * */
    fun getList(idList: List<String>) = doPostRaw("get", mapOf("cal_id_list" to idList))

    /**
     * 删除日历
     * */
    fun delete(id: String) = doPostRaw("del", mapOf("cal_id" to  id))
}

class Schedule(corpId: String?, agentId: String?, suiteId: String?)
    : WorkBaseApi(corpId, agentId,suiteId)
{

    override val group = "oa/schedule"
    companion object{
        const val ADD = "add"
        const val UPDATE = "update"
        const val GET_LIST = "get"
        const val DELETE = "del"
        const val GET_BY_CALENDAR = "get_by_calendar"
    }

    fun add(body: Map<String, Any?>) = doPostRaw(ADD, mapOf("schedule" to body))

    /**
     * 更新日程
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/92619
     * */
    fun update(body: Map<String, Any?>) = doPostRaw(UPDATE, mapOf("schedule" to body))


    /**
     * 获取日程
     * */
    fun getList(idList: List<String>) = doPostRaw(GET_LIST, mapOf("schedule_id_list" to idList))

    /**
     * 删除日程
     * */
    fun delete(id: String) = doPostRaw(DELETE, mapOf("schedule_id" to  id))

    fun getByCalendar(catId: String, offset: Int, limit: Int) = doPostRaw(
        GET_BY_CALENDAR,
        mapOf("cal_id" to  catId, "offset" to offset, "limit" to limit))
}