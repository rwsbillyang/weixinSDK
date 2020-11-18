/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:37
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

package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.security.SignUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

data class JsApiSignature(
    val noncestr: String? = null,
    val timestamp: Long = 0,
    val url: String? = null,
    val signature: String? = null
)

object JsAPI {
    private val log: Logger = LoggerFactory.getLogger("JsAPI")

    /**
     * 获取js-sdk所需的签名JsApiSignature
     * @param url 当前网页的URL，不包含#及其后面部分
     * @param nonceStr 随机字符串
     * @param timestamp 时间戳
     *
     * @return 签名以及相关参数
     */
    fun getSignature(url: String, nonceStr: String? = null, timestamp: Long? = null): JsApiSignature? {
        if(url.contains("#"))
        {
            log.error("cannot include # in url")
            return null
        }
        val jsApiTicket = OfficialAccount.OA.ticket.get()
        if(jsApiTicket == null){
            log.error("jsApiTicket is null, does you config correctly?")
            return null
        }

        val nonce = nonceStr?:UUID.randomUUID().toString().replace("-".toRegex(), "")
        val time = timestamp?:System.currentTimeMillis() / 1000
        val signature = SignUtil.jsApiSignature(jsApiTicket, nonce, time, url)

        return JsApiSignature(nonceStr, time,url, signature)
    }
}