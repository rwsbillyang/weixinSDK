/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-11 09:43
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

package com.github.rwsbillyang.wxWork.isv

import com.github.rwsbillyang.ktorKit.server.LifeCycle
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkMsgHandler
import com.github.rwsbillyang.wxSDK.work.isv.ISuiteInfoHandler
import com.github.rwsbillyang.wxSDK.work.isv.IsvWork
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkMulti

import io.ktor.server.application.*

import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory


internal fun configWxWork3rdMulti(config: SuiteConfig,
                                  msgHandler: IWorkMsgHandler?,
                                  eventHandler: IWorkEventHandler?,
                                  suiteInfoHandler: ISuiteInfoHandler?, enableReset: Boolean = false):Int
{
    val log = LoggerFactory.getLogger("configWxWork3rd")
    var count = 0
    if(config.status == SuiteConfig.STATUS_ENABLED){
        IsvWorkMulti.config(config._id, config.secret, config.token, config.encodingAESKey, config.enableJsSdk, true, config.privateKeyFilePath,
            suiteInfoHandler,msgHandler, eventHandler)
        count++
    }else{
        if(enableReset){
            IsvWorkMulti.removeSuite(config._id)
            log.warn("TODO: unload routing for all agents in corpID=${config._id}")
        }
    }
    return count
}

class OnStartConfigWxWork3rd(application: Application): LifeCycle(application)  {
    private val log = LoggerFactory.getLogger("WxWork3rdConfigInstallation")
    private val suiteHandler: ISuiteInfoHandler? by application.inject()
    private val eventHandler: IWorkEventHandler? by application.inject()
    private val msgHandler: IWorkMsgHandler? by application.inject()

    init {
        onStarted {
            val configService: IsvCorpService by application.inject()
            val list = configService.findSuiteConfigList(SuiteConfig.STATUS_ENABLED)
            var count = 0
            list.forEach {
                count = configWxWork3rdMulti(it, msgHandler,eventHandler, suiteHandler)
            }
            if(count == 0){
                log.warn("no agent context initialized, please check wxWorkConfig in DB")
            }else{
                log.warn("$count suite context initialized from DB")
            }
        }

        logConfig()
    }

    private fun logConfig(){
        log.info("==========================ISV config start====================================")
        log.info("ThirdPartyWork.msgNotifyPath=${IsvWork.msgNotifyPath}, please use suiteId value instead of {suiteId}")
        log.info("ThirdPartyWork.authFromOutsidePath=${IsvWork.authFromOutsidePath}")
        log.info("ThirdPartyWork.oauthNotifyPath=${IsvWork.oauthNotifyPath}")
        log.info("ThirdPartyWork.permanentWebNotifyPath=${IsvWork.permanentWebNotifyPath}?ret=OK or ?ret=KO&msg=no_suiteIdInCache")
        log.info("==========================ISV config end====================================")
    }
}