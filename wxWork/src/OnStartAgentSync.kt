/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-06 16:09
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

import com.github.rwsbillyang.ktorKit.server.LifeCycle
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.WorkSingle
import com.github.rwsbillyang.wxWork.agent.AgentController
import io.ktor.server.application.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/**
 * 系统启动时同步agent信息，只支持内建单应用，ISV单应用在授权后会自动同步agent
 * */
class AgentSyncOnStartNonIsvSingle(application: Application) : LifeCycle(application) {
    private val log = LoggerFactory.getLogger("AgentSyncOnStartNonIsvSingle")
    private val agentController: AgentController by application.inject()
    init {
        onStarted {
            if(!Work.isIsv && !Work.isMulti){ //build构建过程中，未曾config，不需要同步
                agentController.syncAgentIfNotExit(WorkSingle.corpId, WorkSingle.agentId)
            }else{
                log.warn("only support non-Isv single")
            }
        }
    }
}