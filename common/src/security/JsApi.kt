/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-12-12 13:14
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

package com.github.rwsbillyang.wxSDK.security

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.util.*

/**
 * @param objectId 用于做share或relay的id,不需要时可以忽略
 * */
@Serializable
data class JsApiSignature(
    val appId: String,
    val nonceStr: String,
    val timestamp: Long,
    val signature: String,
    val objectId: String, //通常在需要OAuthInfo的时候，是因为需要分享，顺便给其一个分享id，无需再请求获取
)

object JsAPI {
    //private val log: Logger = LoggerFactory.getLogger("JsAPI")

    /**
     * 获取js-sdk所需的签名JsApiSignature
     * @param appId 公众号appId或企业corpId
     * @param jsApiTicket 通过appId或corpId与secret获取的js ticket
     * @param url 当前网页的URL，不包含#及其后面部分
     * @param nonceStr 随机字符串，可不提供
     * @param timestamp 时间戳， 可不提供
     *
     * @return 签名以及相关参数
     */
    fun getSignature(appId: String, jsApiTicket: String, url: String,  nonceStr: String? = null, timestamp: Long? = null): JsApiSignature {
        require(!url.contains("#")){"url cannot contains #"}

        val nonce = nonceStr?:UUID.randomUUID().toString().replace("-".toRegex(), "")
        //val nonce: String = nonceStr?:RandomStringUtils.randomAlphanumeric(32)
        val time = timestamp?:System.currentTimeMillis() / 1000
        val signature = SignUtil.jsApiSignature(jsApiTicket, nonce, time, url)

        return JsApiSignature(appId, nonce, time, signature, Base64.getUrlEncoder().encodeToString(ObjectId().toByteArray()))
    }
}