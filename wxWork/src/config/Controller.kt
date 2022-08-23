/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-25 23:25
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

package com.github.rwsbillyang.wxWork.config

import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkMsgHandler
import com.github.rwsbillyang.wxWork.agent.AgentController
import com.github.rwsbillyang.wxWork.configWxWorkMulti
import io.ktor.server.application.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


import org.koin.ktor.ext.inject


/**
 * 用于多应用的管理后台配置
 * */
class ConfigController : KoinComponent {
   // private val log = LoggerFactory.getLogger("ConfigController")
    private val service: ConfigService by inject()
    private val agentController: AgentController by inject()
    private val application: Application by inject()
   // private val suiteHandler: MySuiteInfoHandler by inject()
   private val eventHandler: IWorkEventHandler by application.inject()
    private val msgHandler: IWorkMsgHandler by application.inject()

    fun saveWxWorkConfig(doc: WxWorkConfig): DataBox<Int> {
        service.saveWxWorkConfig(doc)

        configWxWorkMulti(doc, msgHandler, eventHandler, agentController, true)

        return DataBox.ok(1)
    }

    fun findWxWorkConfig(corpId: String) = service.findWxWorkConfigByCorpId(corpId)
}