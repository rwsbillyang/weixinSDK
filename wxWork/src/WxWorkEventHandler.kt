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

import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.work.inMsg.*
import com.github.rwsbillyang.wxWork.agent.AgentService
import com.github.rwsbillyang.wxWork.contacts.*
import com.github.rwsbillyang.wxWork.isv.IsvCorpService

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import javax.xml.stream.XMLEventReader

//corpId,agentId来自于接收消息路径中指定的参数
//对于isv，corpId为其suiteId， agentId为空
class WxWorkEventHandler : DefaultWorkEventHandler(), KoinComponent {
    private val log = LoggerFactory.getLogger("MyEventHandler")
    private val agentService: AgentService by inject()
    private val isvCorpService: IsvCorpService by inject()
    private val contactHelper: ContactHelper by inject()
    private val contactService: ContactService by inject()

    override fun onDefault(appId: String, agentId: String?, e: AgentEvent): ReBaseMSg? {
        log.info("onDefault: Not yet implemented,appId=$appId, agentId=$agentId, e.agentId=${e.agentId}")
        return null
    }

    override fun onDispatch(appId: String, agentId: String?, reader: XMLEventReader, base: BaseInfo): ReBaseMSg? {
        log.info("onDispatch: Not yet implemented")
        return null
    }


}


