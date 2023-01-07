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
import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.WorkMulti

import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkMsgHandler
import com.github.rwsbillyang.wxWork.agent.AgentController
import com.github.rwsbillyang.wxWork.configWxWork
import io.ktor.server.application.*
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory


/**
 * 用于多应用的管理后台配置
 * */
class ConfigController : KoinComponent {
    private val log = LoggerFactory.getLogger("ConfigController")
    private val service: ConfigService by inject()
    private val agentController: AgentController by inject()
    private val application: Application by inject()
   // private val suiteHandler: MySuiteInfoHandler by inject()
   private val eventHandler: IWorkEventHandler by application.inject()
    private val msgHandler: IWorkMsgHandler by application.inject()

    fun saveWxWorkAgentConfig(doc: WxWorkAgentConfig, name: String): DataBox<WxWorkAgentConfig> {
        if(doc._id == null) doc._id = ObjectId()
        service.saveWxWorkAgentConfig(doc)

        if(!Work.isIsv){
            if(doc.enable){
                if(name == "agent")
                    configWxWork(doc, false, agentController)
                else if(name == "sysagent"){
                    configWxWork(doc, true)
                }else
                {
                    return DataBox.ko("not support name=$name")
                }
            }else{
                WorkMulti.reset(doc.corpId, doc.agentId)
            }

        }else{
            log.warn("TODO: implement config in saveWxWorkSysAgentConfig for ISV")
        }

        return DataBox.ok(doc)
    }

    fun delOne(id: String, name: String): DataBox<Long>{
        return if(name == "agent"){
            val doc = service.findWxWorkAgentConfig(id)
            if(doc != null){
                WorkMulti.reset(doc.corpId, doc.agentId)
            }
            DataBox.ok(service.delAgentConfig(id).deletedCount)
        }else if(name == "sysagent"){
            val doc = service.findWxWorkSysAgentConfig(id)
            if(doc != null){
                WorkMulti.reset(doc.corpId, doc.agentId)
            }
            DataBox.ok(service.deleteOne(service.wxWorkSysAgentCfgCol, id, false))
        }else{
            DataBox.ko<Long>("invalid parameter, not support name=$name")
        }
    }

}