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
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkSingle
import com.github.rwsbillyang.wxWork.account.WxWorkAccount
import com.github.rwsbillyang.wxWork.config.ConfigService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

open class MsgNotifierBase : KoinComponent {
    private val log = LoggerFactory.getLogger("MsgNotifierBase")

    private val configService: ConfigService by inject()
    private val defaultUrl = "http://zhike.niukid.com/#!/admin/tab/material"


    // 点击后跳转的链接。最长2048字节，请确保包含了协议头(http/https)
    fun url(account: WxWorkAccount, corpId: String, agentId: Int?, type: String): String{
        // val url = urlConfigMap?.get(type)?:defaultUrl
        val url = configService.findMsgNotifyConfig(corpId, agentId)?.pathMap?.get(type)?:defaultUrl
        return "$url?corpId=${account.corpId}&agentId=$agentId"
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


    fun msgApi(account: WxWorkAccount, appId: String?, agentId: Int?): MsgApi?{
        if(appId == null || agentId == null || account.userId == null)
        {
            log.warn("appId, agentId or userId is null in account, do nothing")
            return null
        }
        return if(Work.isIsv){
            if(Work.isMulti){
                log.warn("Not support isv multi, do nothing")
                null
            }else{
                MsgApi(appId, agentId, IsvWorkSingle.suiteId)
            }
        }else
            MsgApi(appId, agentId, null)
    }
}