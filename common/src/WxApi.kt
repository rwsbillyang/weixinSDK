/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 15:09
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

package com.github.rwsbillyang.wxSDK


abstract class WxApi(private val accessTokenKey: String = "access_token") : Api() {
    /**
     * API前面的公共部分，后面无斜杠，如：https://api.weixin.qq.com/cgi-bin
     * 公众号、企业微信等API在不同的域名下，故base不同
     *
     * 如https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN中前面的是所有API的共同部分
     * */
    abstract val base: String

    /**
     * API中的组，前后都无斜杠，如menu，意味着 各MenuAPI
     * */
    abstract val group: String

    /**
     * 由各个公众号、企业微信中各个不同模块提供
     * */
    abstract fun accessToken(): String?

    /**
     * 拼接API
     * @param name API的名称 如create： 如"https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN"
     * @param requestParams 请求参数
     * @param needAccessToken 是否需要accessToken
     * */
    override fun url(name: String, requestParams: Map<String, String?>?, needAccessToken: Boolean): String {
        val params = requestParams?.entries?.filter { it.value != null }?.joinToString("&") { "${it.key}=${it.value}" }
        return if (needAccessToken)
        {
            val token = accessToken()
            requireNotNull(token){"accessToken should NOT null"}
            if (params.isNullOrBlank())
                "$base/$group/$name?$accessTokenKey=${token}"
            else
                "$base/$group/$name?$accessTokenKey=${token}&$params"
        }else {
            if (params.isNullOrBlank())
                "$base/$group/$name"
            else
                "$base/$group/$name?$params"
        }

    }
}

