/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-13 17:24
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

package com.github.rwsbillyang.wxSDK.work.inMsg

import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import javax.xml.stream.XMLEventReader


//外部联系人添加了配置了客户联系功能且开启了免验证的成员时（此时成员尚未确认添加对方为好友），回调该事件
//class ExternalContactHalfAddEvent(changeEvent: ExternalContactChangeEvent) : ExternalContactAddEvent(changeEvent)

/**
 * 外部联系人变更事件，适合内建应用，不适合第三方
 * @param baseInfo 将已读取的BaseInfo数据传递过来
 * @param agentEvent 将已读取的数据AgentEvent传递过来
 *
 * */
open class ExternalContactChangeEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
    var changeType: String? = null
    var userId: String? = null
    var externalUserId: String? = null

    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 1) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when (e.asStartElement().name.toString()) {
                    "ChangeType" -> {
                        changeType = reader.elementText; count++
                    }
                }
            }
        }
    }
}

//<xml><ToUserName><![CDATA[wwb...f1c]]></ToUserName><FromUserName><![CDATA[sys]]></FromUserName><CreateTime>1631778573</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[change_external_contact]]></Event><ChangeType><![CDATA[add_external_contact]]></ChangeType><UserID><![CDATA[Yg]]></UserID><ExternalUserID><![CDATA[wmNHB1CgAAelLahs7gi-AWi5z7qtHSsQ]]></ExternalUserID><WelcomeCode><![CDATA[S0Aspe-wGaOFXK-gojQfXS7WgkSK0S_ymE_WIcM9aqk]]></WelcomeCode></xml>
open class ExternalContactAddEvent(baseInfo: BaseInfo, agentEvent: AgentEvent) : ExternalContactChangeEvent(baseInfo, agentEvent) {
    init{
        changeType = WorkEventType.EXTERNAL_CONTACT_ADD
    }
    var state: String? = null
    var welcomeCode: String? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 4) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when (e.asStartElement().name.toString()) {
                    "UserID" -> {
                        userId = reader.elementText; count++
                    }
                    "ExternalUserID" -> {
                        externalUserId = reader.elementText; count++
                    }
                    "State" -> {
                        state = reader.elementText; count++
                    }
                    "WelcomeCode" -> {
                        welcomeCode = reader.elementText; count++
                    }
                }
            }
        }
    }
}
//<xml><ToUserName><![CDATA[ww5f4c472a66331eeb]]></ToUserName><FromUserName><![CDATA[sys]]></FromUserName><CreateTime>1634117861</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[change_external_contact]]></Event><ChangeType><![CDATA[del_follow_user]]></ChangeType><UserID><![CDATA[ycg]]></UserID><ExternalUserID><![CDATA[wmFOeKDQAAuIIoxInKmpVmhxjrN3tHqA]]></ExternalUserID><State><![CDATA[YVbXo-8Rd3kMyAeF]]></State></xml>
class ExternalContactUpdateEvent(baseInfo: BaseInfo, agentEvent: AgentEvent) : ExternalContactChangeEvent(baseInfo, agentEvent)
{
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when (e.asStartElement().name.toString()) {
                    "UserID" -> {
                        userId = reader.elementText; count++
                    }
                    "ExternalUserID" -> {
                        externalUserId = reader.elementText; count++
                    }
                }
            }
        }
    }
}
class ExternalContactDelEvent(baseInfo: BaseInfo, agentEvent: AgentEvent) : ExternalContactChangeEvent(baseInfo, agentEvent) {
    var source: String? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 3) {
        while (reader.hasNext()) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when (e.asStartElement().name.toString()) {
                    "UserID" -> {
                        userId = reader.elementText; count++
                    }
                    "ExternalUserID" -> {
                        externalUserId = reader.elementText; count++
                    }
                    "Source" -> {
                        source = reader.elementText; count++
                    }
                }
            }
        }
    }
}
}

class ExternalContactTransferFailEvent(baseInfo: BaseInfo, agentEvent: AgentEvent) : ExternalContactChangeEvent(baseInfo, agentEvent){

    var failReason: String? = null

    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 3) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when (e.asStartElement().name.toString()) {
                    "FailReason" -> {
                        failReason = reader.elementText; count++
                    }
                    "UserID" -> {
                        userId = reader.elementText; count++
                    }
                    "ExternalUserID" -> {
                        externalUserId = reader.elementText; count++
                    }
                }
            }
        }
    }
}




/**
 * 外部联系人变更事件，适合内建应用，不适合第三方
 * */
open class ExternalChatChangeEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }

    var changeType: String? = null
    var chatId: String? = null

    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when (e.asStartElement().name.toString()) {
                    "ChatId" -> {
                        chatId = reader.elementText; count++
                    }
                    "ChangeType" -> {
                        changeType = reader.elementText; count++
                    }
                }
            }
        }
    }
}

/**
 * @property updateDetail 变更详情。目前有以下几种：
add_member : 成员入群
del_member : 成员退群
change_owner : 群主变更
change_name : 群名变更
change_notice : 群公告变更

 * @property joinScene 当是成员入群时有值。表示成员的入群方式
0 - 由成员邀请入群（包括直接邀请入群和通过邀请链接入群）
3 - 通过扫描群二维码入群

 * @property quitScene 当是成员退群时有值。表示成员的退群方式
0 - 自己退群
1 - 群主/群管理员移出

 * @property memChangeCnt 当是成员入群或退群时有值。表示成员变更数量
 *
 * */
class ExternalChatUpdateEvent(baseInfo: BaseInfo, agentEvent: AgentEvent) : ExternalChatChangeEvent(baseInfo, agentEvent) {
    var updateDetail: String? = null
    var joinScene: String? = null
    var quitScene: Int? = null
    var memChangeCnt: Int? = null

    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 4) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when (e.asStartElement().name.toString()) {
                    "UpdateDetail" -> {
                        updateDetail = reader.elementText; count++
                    }
                    "JoinScene" -> {
                        joinScene = reader.elementText; count++
                    }
                    "QuitScene" -> {
                        quitScene = reader.elementText?.toInt(); count++
                    }
                    "MemChangeCnt" -> {
                        memChangeCnt = reader.elementText?.toInt(); count++
                    }
                }
            }
        }
    }
}

//TODO: 企业客户标签事件