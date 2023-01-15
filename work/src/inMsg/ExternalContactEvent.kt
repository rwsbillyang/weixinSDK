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


import org.w3c.dom.Element



//外部联系人添加了配置了客户联系功能且开启了免验证的成员时（此时成员尚未确认添加对方为好友），回调该事件
//class ExternalContactHalfAddEvent(changeEvent: ExternalContactChangeEvent) : ExternalContactAddEvent(changeEvent)

/**
 * 外部联系人变更事件，适合内建应用，不适合第三方
 * @param baseInfo 将已读取的BaseInfo数据传递过来
 * @param agentEvent 将已读取的数据AgentEvent传递过来
 *
 * */
open class ExternalContactChangeEvent(xml: String, rootDom: Element) : AgentEvent(xml, rootDom) {
    val changeType = get(rootDom, "ChangeType")
    val userId = get(rootDom, "UserId")
    val externalUserId = get(rootDom, "ExternalUserId")
}

//<xml><ToUserName><![CDATA[wwb...f1c]]></ToUserName><FromUserName><![CDATA[sys]]></FromUserName><CreateTime>1631778573</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[change_external_contact]]></Event><ChangeType><![CDATA[add_external_contact]]></ChangeType><UserID><![CDATA[Yg]]></UserID><ExternalUserID><![CDATA[wmNHB1CgAAelLahs7gi-AWi5z7qtHSsQ]]></ExternalUserID><WelcomeCode><![CDATA[S0Aspe-wGaOFXK-gojQfXS7WgkSK0S_ymE_WIcM9aqk]]></WelcomeCode></xml>
open class ExternalContactAddEvent(xml: String, rootDom: Element) : ExternalContactChangeEvent(xml, rootDom) {
    val state = get(rootDom, "State")
    val welcomeCode = get(rootDom, "WelcomeCode")

}

//<xml><ToUserName><![CDATA[ww5f4c472a66331eeb]]></ToUserName><FromUserName><![CDATA[sys]]></FromUserName><CreateTime>1634117861</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[change_external_contact]]></Event><ChangeType><![CDATA[del_follow_user]]></ChangeType><UserID><![CDATA[ycg]]></UserID><ExternalUserID><![CDATA[wmFOeKDQAAuIIoxInKmpVmhxjrN3tHqA]]></ExternalUserID><State><![CDATA[YVbXo-8Rd3kMyAeF]]></State></xml>
class ExternalContactUpdateEvent(xml: String, rootDom: Element) : ExternalContactChangeEvent(xml, rootDom)

class ExternalContactDelEvent(xml: String, rootDom: Element) : ExternalContactChangeEvent(xml, rootDom) {
    val source = get(rootDom, "Source")
}

class ExternalContactTransferFailEvent(xml: String, rootDom: Element) : ExternalContactChangeEvent(xml, rootDom) {
    val failReason = get(rootDom, "FailReason")
}


/**
 * 外部联系人变更事件，适合内建应用，不适合第三方
 * */
open class ExternalChatChangeEvent(xml: String, rootDom: Element) : ExternalContactChangeEvent(xml, rootDom) {
    val chatId = get(rootDom, "ChatId")
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
class ExternalChatUpdateEvent(xml: String, rootDom: Element) : ExternalContactChangeEvent(xml, rootDom) {
    val updateDetail = get(rootDom, "UpdateDetail")
    val joinScene = get(rootDom, "JoinScene")
    val quitScene = get(rootDom, "QuitScene")
    val memChangeCnt = get(rootDom, "MemChangeCnt")
}

//TODO: 企业客户标签事件