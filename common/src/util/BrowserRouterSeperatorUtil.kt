/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-03 11:48
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

package com.github.rwsbillyang.wxSDK.util


/**
 * 前端若是SPA，通知路径可能需要添加browserHistorySeparator: /wxoa/authNotify  or /#!/wxoa/authNotify or /#/wxoa/authNotify
 * 因# #！等字符不能直接在path中(会报404)，故不能直接notify后端seperator所使用的分隔符，
 * 因而采用数字编码， 前端和后端需约定好都遵守该编码，前端使用0，1, 2等参数，后端解析出来，得到对应的分隔符
 * "$notifyPath1/{appId}/{needUserInfo}/{separator}/{owner?}"
 * "$notifyPath2/{appId}/{separator}"
 *
 * IsvWork.oauthNotifyPath + "/{suiteId}/{needUserInfo}/{separator}"
 * Work.oauthNotifyPath + "/{corpId}/{agentId}/{needUserInfo}/{separator}"
 * */
object BrowserRouterSeperatorUtil {
    fun getUri(seperatorType: String?, notifyWebAppUrl: String) = when(seperatorType){
        "1" -> {
            "/#!${notifyWebAppUrl}"
        }
        "2" -> {
            "/#${notifyWebAppUrl}"
        }
        "0", null -> {
            notifyWebAppUrl
        }
        else -> {
            notifyWebAppUrl
        }
    }
}