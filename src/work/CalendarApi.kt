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

class CalendarApi : WorkBaseApi(){
    override val group = "oa/calendar"
    companion object{
        const val ADD = "add"
        const val UPDATE = "update"
        const val GET_LIST = "get"
        const val DELETE = "del"
    }

    fun add(calendar: Calendar) = doPost(ADD, mapOf("calendar" to calendar))

    /**
     * 更新日历
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/92619
     * */
    fun update(calendar: Calendar) = doPost(UPDATE, mapOf("calendar" to calendar))


    /**
     * 获取日历
     * */
    fun getList(idList: List<String>) = doPost(GET_LIST, mapOf("cal_id_list" to idList))

    /**
     * 删除日历
     * */
    fun delete(id: String) = doPost(DELETE, mapOf("cal_id" to  id))
}

class Schedule: WorkBaseApi(){
    override val group = "oa/schedule"
    companion object{
        const val ADD = "add"
        const val UPDATE = "update"
        const val GET_LIST = "get"
        const val DELETE = "del"
        const val GET_BY_CALENDAR = "get_by_calendar"
    }

    fun add(body: Map<String, Any?>) = doPost(ADD, mapOf("schedule" to body))

    /**
     * 更新日程
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/92619
     * */
    fun update(body: Map<String, Any?>) = doPost(UPDATE, mapOf("schedule" to body))


    /**
     * 获取日程
     * */
    fun getList(idList: List<String>) = doPost(GET_LIST, mapOf("schedule_id_list" to idList))

    /**
     * 删除日程
     * */
    fun delete(id: String) = doPost(DELETE, mapOf("schedule_id" to  id))

    fun getByCalendar(catId: String, offset: Int, limit: Int) = doPost(
        GET_BY_CALENDAR,
        mapOf("cal_id" to  catId, "offset" to offset, "limit" to limit))
}