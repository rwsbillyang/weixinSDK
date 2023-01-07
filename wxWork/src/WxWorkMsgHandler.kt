/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-10 15:57
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
import com.github.rwsbillyang.wxSDK.msg.ReTextMsg
import com.github.rwsbillyang.wxSDK.work.inMsg.*
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import javax.xml.stream.XMLEventReader

class WxWorkMsgHandler: DefaultWorkMsgHandler(), KoinComponent {
    companion object{
        /**
         * 为简单，默认回复消息作为静态变量，各业务app可指定自己的默认回复消息
         * */
        var defaultReTMsgText: String? = null
    }

    private val log = LoggerFactory.getLogger("MyMsgHandler")
    override fun onDefault(appId: String, agentId: String?, msg: WorkBaseMsg): ReBaseMSg? {
        log.info("onDefault:appId=$appId, agentId=$agentId Not yet implemented")
        if(defaultReTMsgText == null) return null
        return ReTextMsg(defaultReTMsgText!!, msg.base.fromUserName, msg.base.toUserName, System.currentTimeMillis())
    }

    override fun onDispatch(appId: String, agentId: String?, reader: XMLEventReader, base: BaseInfo): ReBaseMSg? {
        log.info("onDispatch:appId=$appId, agentId=$agentId Not yet implemented")
        return onDefault(appId, agentId, WorkBaseMsg(base))
    }

}