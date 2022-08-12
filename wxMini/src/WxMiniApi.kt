/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-02-02 13:46
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

package com.github.rwsbillyang.wxSDK.wxMini

import com.github.rwsbillyang.ktorKit.apiJson.KHttpClient
import org.slf4j.LoggerFactory
import com.github.rwsbillyang.wxSDK.IBase


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.Logger


class WxMiniApi: KHttpClient() {
    companion object {
        private val log: Logger = LoggerFactory.getLogger("WxMiniApi")
    }
    fun code2Session(appId: String, jsCode: String):ResponseCode2Session?{
        val secret = WxMini.ApiContextMap[appId]?.secret
        if(secret == null){
            log.warn("not config Wx Mini Program for appId=$appId")
            return null
        }
        return get( "https://api.weixin.qq.com/sns/jscode2session?appid=$appId&secret=$secret&js_code=$jsCode&grant_type=authorization_code")
    }
}

@Serializable
class ResponseCode2Session(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    @SerialName("openid")
    val openId: String? = null,
    @SerialName("unionid")
    val unionId: String? = null, //用户在开放平台的唯一标识符，若当前小程序已绑定到微信开放平台帐号下会返回
    @SerialName("session_key")
    val sessionKey: String? = null //会话密钥
) : IBase