/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-02-02 16:01
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

import org.slf4j.LoggerFactory

import com.github.rwsbillyang.ktorKit.AppModule
import com.github.rwsbillyang.ktorKit.LifeCycle
import com.github.rwsbillyang.wxSDK.wxMini.account.AccountController
import com.github.rwsbillyang.wxSDK.wxMini.account.AccountServiceMini
import com.github.rwsbillyang.wxSDK.wxMini.account.account
import com.github.rwsbillyang.wxSDK.wxMini.config.MiniConfigService
import com.github.rwsbillyang.wxSDK.wxMini.config.wxMiniConfigApi
import io.ktor.application.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject


val wxMiniProgramModule = AppModule(
    listOf(
        module(createdAtStart = true) {
            single { OnStartConfigWxMini(get()) }
        },
        module {
            single { MiniConfigService(get()) }
            single { AccountController(get()) }
            single { AccountServiceMini(get()) }
        },
        ),
    "WxMini"
) {
    wxMiniConfigApi()
    account()
}

object WxMini {
    val ApiContextMap = hashMapOf<String, WxMiniContext>()

    /**
     * 配置参数
     * */
    fun config(block: WxMiniConfiguration.() -> Unit) {
        val config = WxMiniConfiguration().apply(block)

        val ctx = WxMiniContext(
            config.appId,
            config.secret
        )
        ApiContextMap[config.appId] = ctx
    }

}

class WxMiniConfiguration {
    var appId: String = "your_app_id"
    var secret: String = "your_app_secret_key"
}

class WxMiniContext(
    val appId: String,
    val secret: String
)


/**
 * 从数据库中加载企业微信配置信息
 * 可能存在多个应用，每个应用存在于多个企业
 * */
class OnStartConfigWxMini(application: Application) : LifeCycle(application) {
    private val log = LoggerFactory.getLogger("OnStartConfigWxMini")


    init {
        onStarted {
            val configService: MiniConfigService by application.inject()

            var count = 0
            //只加载enabled的
            configService.findConfigList(true).forEach {
                WxMini.config {
                    appId = it._id
                    secret = it.secret
                }
                count++

                log.info("mini program is initialized: ${it.name}, appId=${it._id}")
            }


            if (count == 0) {
                log.warn("no mini program context initialized, please check MiniConfig in DB")
            } else {
                log.info("$count mini program context initialized from DB")
            }
        }
    }
}


