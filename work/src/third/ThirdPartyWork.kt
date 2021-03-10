/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-03-07 20:35
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

package com.github.rwsbillyang.wxSDK.work.third


import com.github.rwsbillyang.wxSDK.accessToken.*

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



object ThirdPartyWork {
    /**
     * key -> value: suiteId -> SuiteApiContext
     * 一个服务商，可以有多个应用，这里的map是多个应用
     * */
    val ApiContextMap = hashMapOf<String, SuiteApiContext>()
}


@Serializable
open class SuiteParameters(
    @SerialName("suite_id")
    val id: String, //suiteId
    @SerialName("suite_secret")
    val secret: String,
    @SerialName("suite_ticket")
    val ticket: String
)
class SuiteApiContext(
    val id: String, //suiteId
    val secret: String,
    var ticket: String,//suite_ticket由企业微信后台定时推送给“指令回调URL”，每十分钟更新一次,suite_ticket实际有效期为30分钟
    val suiteAccessToken: ITimelyRefreshValue = TimelyRefreshSuiteToken(id),

    //一个服务商应用有多个企业使用，corpId -> accessToken TODO:可能此map很大
    val map: HashMap<String, TimelyRefreshAccessToken3rd> = hashMapOf()
)

class SuiteRefresher(id: String): PostRefresher<SuiteParameters>(
    "suite_access_token",
    ThirdPartyWork.ApiContextMap[id]?.let {
        SuiteParameters(id, it.secret, it.ticket)
    },
    "https://qyapi.weixin.qq.com/cgi-bin/service/get_suite_token")

/**
 * 动态suite_access_token
 *
 * @param suiteId      以ww或wx开头应用id（对应于旧的以tj开头的套件id）
 * @param refresher      刷新器
 */
class TimelyRefreshSuiteToken @JvmOverloads constructor(
    suiteId: String,
    refresher: IRefresher = SuiteRefresher(suiteId)
) : TimelyRefreshValue(suiteId, refresher), ITimelyRefreshValue {
    init {
        get() //第一次加载
    }
    /**
     * 获取accessToken，若过期则会自动刷新
     */
    override fun get() = getRefreshedValue(UpdateType.WX_WORK_SUITE_TOKEN)
}



class AccessToken3rdRefresher(auth_corpid: String, permanent_code: String, suiteAccessToken: ITimelyRefreshValue?):PostRefresher<Map<String, String>>(
    "access_token",
    mapOf("auth_corpid" to auth_corpid, "permanent_code" to permanent_code),
    " https://qyapi.weixin.qq.com/cgi-bin/service/get_corp_token?suite_access_token=${suiteAccessToken?.get()}"
)

class TimelyRefreshAccessToken3rd @JvmOverloads constructor(
    suiteId: String, auth_corpid: String, permanent_code: String,
    refresher: IRefresher = AccessToken3rdRefresher(auth_corpid, permanent_code,
        ThirdPartyWork.ApiContextMap[suiteId]?.suiteAccessToken)
) : TimelyRefreshValue("$suiteId/$auth_corpid", refresher), ITimelyRefreshValue {
    init {
        get() //第一次加载
    }
    /**
     * 获取accessToken，若过期则会自动刷新
     */
    override fun get() = getRefreshedValue(UpdateType.WX_WORK_SUITE_TOKEN)
}




/**
 * @param corpid	是	服务商的corpid
 * @param provider_secret	是	服务商的secret，在服务商管理后台可见
 * */
class ProviderTokenRefresher(corpid: String, provider_secret: String):PostRefresher<Map<String, String>>(
    "provider_access_token",
    mapOf("corpid" to corpid, "provider_secret" to provider_secret),
    "https://qyapi.weixin.qq.com/cgi-bin/service/get_provider_token"
)

/**
 * 服务商的provider_access_token
 * 以corpid、provider_secret（获取方法为：登录服务商管理后台->标准应用服务->通用开发参数，可以看到）
 * 换取provider_access_token，代表的是服务商的身份，而与应用无关。
 * 请求单点登录、注册定制化等接口需要用到该凭证。
 * */
class TimelyRefreshProviderToken @JvmOverloads constructor(
    corpId: String, secret: String,
    refresher: IRefresher = ProviderTokenRefresher(corpId, secret))
    : TimelyRefreshValue(corpId, refresher), ITimelyRefreshValue{
    init {
        get() //第一次加载
    }
    /**
     * 获取accessToken，若过期则会自动刷新
     */
    override fun get() = getRefreshedValue(UpdateType.WX_WORK_PROVIDER_TOKEN)
}