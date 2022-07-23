/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 15:56
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

package com.github.rwsbillyang.wxOA.msg

import com.github.rwsbillyang.wxOA.pref.PrefService
import com.github.rwsbillyang.wxSDK.officialAccount.MsgApi
import com.github.rwsbillyang.wxSDK.officialAccount.outMsg.ColoredValue
import com.github.rwsbillyang.wxSDK.officialAccount.outMsg.TemplateMsg
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

open class TemplateMsgBase: KoinComponent {
    private val log = LoggerFactory.getLogger("TemplateMsgBase")

    protected val color = "#173177"

    private val msgService: MsgService by inject()
    private val prefService: PrefService by inject()

    /**
     * @param openId 消息接收者
     * @param appId 用于找模板消息配置
     * @param key 用于得到配置中的模板消息ID，以及点击后的跳转Url
     * @param data 消息正文数据
     *
     * @return 成功进行发送，返回true，否则返回false
     * */
    fun sendTemplateMsg(openId: String, appId: String?, key: String, data: MutableMap<String, ColoredValue>): Boolean {
        if(appId == null){
            log.warn("no appId")
            return false
        }
        val cfg = msgService.findTemplateMsgConfig(appId)
        if (cfg == null) {
            log.warn("not config TemplateMsgConfig for appId=$appId?")
            return false
        }

        val templateId = cfg.templateIdMap[key]
        if (templateId == null) {
            log.warn("not config templateId for key=$key ?")
            return false
        }
        val host = prefService.findOfficialAccount(appId)?.host?:""
        GlobalScope.launch {
            val templateMsg = TemplateMsg(
                openId,
                templateId,
                data,
                host+cfg.urlMap?.get(key)
            )
            MsgApi(appId).sendTemplateMsg(templateMsg)
        }
        return true
    }
}