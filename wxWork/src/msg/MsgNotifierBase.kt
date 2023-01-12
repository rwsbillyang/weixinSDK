/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 16:28
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

package com.github.rwsbillyang.wxWork.msg

import com.github.rwsbillyang.ktorKit.util.DatetimeUtil
import com.github.rwsbillyang.wxSDK.work.MsgApi
import com.github.rwsbillyang.wxSDK.work.outMsg.IOutWxWorkMsg
import com.github.rwsbillyang.wxWork.account.WxWorkAccount
import com.github.rwsbillyang.wxWork.agent.AgentService
import com.github.rwsbillyang.wxWork.config.ConfigService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

open class MsgNotifierBase : KoinComponent {
    private val log = LoggerFactory.getLogger("MsgNotifierBase")

    private val configService: ConfigService by inject()
    private val agentService: AgentService by inject()

    fun notifyMsg(corpId: String, agentId: String, msg: IOutWxWorkMsg) = runBlocking{
        launch {
            val api =  MsgApi(corpId, agentId)
            api.send(msg)
        }
    }

    // 点击后跳转的链接。最长2048字节，请确保包含了协议头(http/https)
    fun url(corpId: String, agentId: String?, type: String?): String{
        val defaultUrl = if(agentId == null) null else{
            agentService.findAgent(agentId, corpId)?.url ?: configService.findWxWorkAgentConfigByAgentId(corpId, agentId)?.url
        }
        val url = if(type == null) defaultUrl else configService.findMsgNotifyConfig(corpId, agentId)?.pathMap?.get(type)?:defaultUrl
        if(url == null){
            log.warn("not found url in agent and agentConfig: corpId=$corpId, agentId=$agentId")
        }
        return "$url?corpId=${corpId}&agentId=$agentId"
    }

    fun div(text: String, className: String) = "<div class=\"$className\">${text}</div>"
    fun grayDiv(text: String) = div(text, "gray")
    fun normalDiv(text: String) = div(text, "normal")
    fun highlightDiv(text: String) = div(text, "highlight")

    fun setupDescription(normal: String, highlight: String?, date: String? = null): String{
        val grayHtml = div(date?: DatetimeUtil.format(System.currentTimeMillis()),"gray")
        val normalHtml = div(normal, "normal")
        val highlightHtml = if(highlight != null) div(highlight, "highlight")  else ""

        return grayHtml + normalHtml + highlightHtml
    }
    fun setupDescription(normals: List<String?>, highlight: String?, date: String? = null): String{
        val grayHtml = div(date?: DatetimeUtil.format(System.currentTimeMillis()),"gray")
        val highlightHtml = if(highlight != null) div(highlight, "highlight")  else ""

        return grayHtml + normals.filterNotNull().joinToString(" ") { div(it, "normal") } + highlightHtml
    }
}