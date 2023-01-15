/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-04 16:24
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

package com.github.rwsbillyang.wxWork


import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.WxXmlEvent
import com.github.rwsbillyang.wxSDK.work.inMsg.DefaultWorkEventHandler
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import org.w3c.dom.Element


//corpId,agentId来自于接收消息路径中指定的参数
//对于isv，corpId为其suiteId， agentId为空
//每个agent都拥有自己的context，对应着不同的MsgbHub、msgHandler、EventHandler，通常情况下它们都共用一个handler或按类别区分
// 这些EventHandler和MsgHandler只处理属于自己的消息或事件
class WxWorkEventHandler : DefaultWorkEventHandler(), KoinComponent {
    private val log = LoggerFactory.getLogger("WxWorkEventHandler")

    override fun onDefault(appId: String, agentId:String?, e: WxXmlEvent): ReBaseMSg? {
        log.info("onDefault: Not yet implemented,appId=$appId, agentId=$agentId, agentId=${agentId}")
        return null
    }

    override fun onDispatch(appId: String, agentId:String?, xml: String, rootDom: Element, msgOrEventType: String?): ReBaseMSg? {
        log.info("onDispatch: TODO: handle other eventType=${msgOrEventType}")
        return null
    }


}


