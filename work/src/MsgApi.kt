/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-22 14:32
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

package com.github.rwsbillyang.wxSDK.work

import com.github.rwsbillyang.wxSDK.IBase
import com.github.rwsbillyang.wxSDK.work.outMsg.IOutWxWorkMsg
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MsgApi (corpId: String?, agentId: Int?, suiteId: String?)
    : WorkBaseApi(corpId, agentId,suiteId)
{
    override val group = "message"

    fun send(msg: IOutWxWorkMsg):SendMsgResponse = doPost("send", msg)
}

@Serializable
class SendMsgResponse(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    val invaliduser: String? = null,
    val invalidparty: String? = null,
    val invalidtag: String? = null,
    val msgid: String? = null,
    val response_code: String? = null,
): IBase
