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


enum class CheckInDataType(val value: Int){
    WORK(1),
    OUTGOING(2),
    ALL(3)
}

class CheckinApi(corpId: String) : WorkBaseApi(corpId){
    constructor(suiteId: String, corpId: String) : this(corpId) {
        this.suiteId = suiteId
    }
    constructor(corpId: String, agentId: Int) : this(corpId) {
        this.agentId = agentId
    }

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
            = doPost3(
            "getcheckindata", mapOf("opencheckindatatype" to openCheckInDataType.value, "" to startTime,
        "endtime" to endTime, "useridlist" to userIdList))

    /**
     * 获取打卡规则
     *
     * datetime	是	需要获取规则的日期当天0点的Unix时间戳
     * useridlist	是	需要获取打卡规则的用户列表
     * */
    fun getCheckInRule(datetime: Long, userIdList: List<String>) = doPost3(
            "getcheckinoption",
        mapOf("datetime" to datetime, "useridlist" to userIdList))
 }

class OaApi(corpId: String) : WorkBaseApi(corpId){
    constructor(suiteId: String, corpId: String) : this(corpId) {
        this.suiteId = suiteId
    }
    constructor(corpId: String, agentId: Int) : this(corpId) {
        this.agentId = agentId
    }

    override val group = "oa"
    companion object{

    }
}