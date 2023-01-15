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

import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.WxXmlMsg

import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

//每个公众号都拥有自己的context，对应着不同的MsgbHub、msgHandler、EventHandler，通常情况下它们都共用一个handler
// 这些EventHandler和MsgHandler只处理属于自己的消息或事件
class MsgHandler: DefaultOAMsgHandler(), KoinComponent {
    private val statsService: StatsService by inject()
    private val reMsgChooser: ReMsgChooser by inject()
    override fun onDefault(appId: String, msg: WxXmlMsg): ReBaseMSg? {
        runBlocking {
            launch {
                val statMsg = StatsMsg(appId, msg.toUserName, msg.fromUserName, msg.createTime, msg.msgId, msg.xml)
                statsService.insertMsg(statMsg)
            }
        }

        return reMsgChooser.tryReMsg(appId, msg)
    }

}
