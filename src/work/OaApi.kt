package com.github.rwsbillyang.wxSDK.work


enum class CheckInDataType(val value: Int){
    WORK(1),
    OUTGOING(2),
    ALL(3)
}

class CheckinApi: WorkBaseApi(){
    override val group = "checkin"


    /**
     * 获取打卡数据
     *
     * opencheckindatatype	是	打卡类型。1：上下班打卡；2：外出打卡；3：全部打卡
     * starttime	是	获取打卡记录的开始时间。Unix时间戳
     * endtime	是	获取打卡记录的结束时间。Unix时间戳
     * useridlist	是	需要获取打卡记录的用户列表
     * */
    fun getCheckInData(openCheckInDataType: CheckInDataType, startTime: Long, endTime: Long, userIdList: List<String>)
            = doPost(
            "getcheckindata", mapOf("opencheckindatatype" to openCheckInDataType.value, "" to startTime,
        "endtime" to endTime, "useridlist" to userIdList))

    /**
     * 获取打卡规则
     *
     * datetime	是	需要获取规则的日期当天0点的Unix时间戳
     * useridlist	是	需要获取打卡规则的用户列表
     * */
    fun getCheckInRule(datetime: Long, userIdList: List<String>) = doPost(
            "getcheckinoption",
        mapOf("datetime" to datetime, "useridlist" to userIdList))
 }

class OaApi: WorkBaseApi(){
    override val group = "oa"
    companion object{

    }
}