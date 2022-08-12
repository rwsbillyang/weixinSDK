/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-12-10 17:04
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

package com.github.rwsbillyang.wxOA

import com.github.rwsbillyang.wxOA.stats.StatsMsg
import com.github.rwsbillyang.wxOA.stats.StatsService
import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.WxBaseMsg
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.*
import org.koin.core.component.inject

import javax.xml.stream.XMLEventReader

class MsgHandler: IOAMsgHandler, MsgEventCommonHandler()  {
    private val statsService: StatsService by inject()

    override fun onDefault(appId: String, msg: WxBaseMsg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId)
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }

    override fun onDispatch(appId: String, agentId:Int?, reader: XMLEventReader, base: BaseInfo): ReBaseMSg? = null

    override fun onOACustomerClickMenuMsg(appId: String, msg: OACustomerClickMenuMsg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId,"${msg.menuId}, ${msg.content}")
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }

    override fun onOAImgMsg(appId: String, msg: OAImgMSg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId,"${msg.mediaId}, ${msg.picUrl}")
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }

    override fun onOALinkMsg(appId: String, msg: OALinkMsg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId,"${msg.url}, ${msg.title},${msg.title}")
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }

    override fun onOALocationMsg(appId: String, msg: OALocationMsg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId,"${msg.locationX}, ${msg.locationY},${msg.scale}, ${msg.label}")
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }

    override fun onOAShortVideoMsg(appId: String, msg: OAShortVideoMsg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId,"${msg.mediaId}, ${msg.thumbMediaId}")
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }

    override fun onOATextMsg(appId: String, msg: OATextMsg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId,msg.content)
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }

    override fun onOAVideoMsg(appId: String, msg: OAVideoMsg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId,"${msg.mediaId}, ${msg.thumbMediaId}")
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }

    override fun onOAVoiceMsg(appId: String, msg: OAVoiceMsg): ReBaseMSg? {
        val statMsg = StatsMsg(appId, msg.base.toUserName, msg.base.fromUserName, msg.base.createTime, msg.msgId,"${msg.mediaId}, ${msg.format}, ${msg.recognition}")
        statsService.insertMsg(statMsg)
        return tryReMsg(appId, msg)
    }
}
