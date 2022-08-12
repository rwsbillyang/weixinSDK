/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-17 21:11
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


import com.github.rwsbillyang.ktorKit.LifeCycle
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.WorkMulti
import com.github.rwsbillyang.wxSDK.work.WorkSingle
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkMsgHandler
import com.github.rwsbillyang.wxWork.agent.AgentController
import com.github.rwsbillyang.wxWork.config.ConfigService
import com.github.rwsbillyang.wxWork.config.WxWorkConfig
import io.ktor.server.application.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory


/**
 * 单个应用的多企业配置
 * @param config 企业级配置
 * @param agentController 调用者注入后传递过来，用于同步agent信息
 * @param enableReset 当修改配置后保存时，需求reset配置，即去掉map中的apiContext。初始化时不需要reset
 * */
internal fun configWxWorkMulti(
    config: WxWorkConfig,
    msgHandler: IWorkMsgHandler?,
    eventHandler: IWorkEventHandler?,
    agentController: AgentController,
    enableReset: Boolean = false
): Int {
    var count = 0
    if (config.enable) {
        WorkMulti.config(
            config.corpId, config.agentId, config.secret, config.token, config.aesKey,
            config.private, config.enableJsSdk, config.enableMsg,
            customMsgHandler = msgHandler,
            customEventHandler = eventHandler
        )
        count++

        //企业应用的可见用户更新，需在调用Work.config配置完work后进行同步调用
        agentController.syncAgentIfNotExit(config.corpId, config.agentId) //syncContacts改由管理员手工同步

        config.systemAccessTokenKeyMap?.forEach { (k, v) -> WorkMulti.config(config.corpId, k, v) }
    } else if (enableReset) {
        WorkMulti.reset(config.corpId)
        println("TODO: unload routing for all agents in corpID=$config._id")
    }

    return count
}

/**
 * 单个应用的单企业配置
 * */
internal fun configWxWorkSingle(
    config: WxWorkConfig,
    msgHandler: IWorkMsgHandler?,
    eventHandler: IWorkEventHandler?,
    agentController: AgentController
): Int {
    var count = 0
    with(config) {
        WorkSingle.config(
            corpId, agentId, secret, token, aesKey,
            private, enableJsSdk, enableMsg,
            customMsgHandler = msgHandler, //所有应用使用同一个msgHandler（无特殊处理）和eventHandler（主要用于通讯录等变化通知接收处理）
            customEventHandler = eventHandler  //多个应用时可以只有一个接收通讯录变化即可
        )

        count++

        //企业应用的可见用户更新，需在调用Work.config配置完work后进行同步调用
        agentController.syncAgent(corpId, agentId)//同步agent，以及agent的可见成员详情，可见成员的外部联系人详情

        systemAccessTokenKeyMap?.forEach { (k, v) -> WorkSingle.config(k, v) }
    }
    return count
}

//internal fun configMsgNotifyUrl(application: Application) {
//    val configService: ConfigService by application.inject()
//    val cfg = configService.findMsgNotifyConfig()
//    if(cfg != null){
//        val msgNotifier: MsgNotifier by application.inject()
//        val urlConfigMap = hashMapOf<NotifierType, String>()
//        cfg.pathMap.forEach { (t, u) ->
//            urlConfigMap[t] = cfg.host+u
//        }
//        msgNotifier.configUrl(urlConfigMap)
//    }
//}


/**
 * 从数据库中加载企业微信配置信息
 * 可能存在多个应用，每个应用存在于多个企业
 * */
class OnStartConfigWxWork(application: Application) : LifeCycle(application) {
    private val log = LoggerFactory.getLogger("WxWorkConfig")
    private val agentController: AgentController by application.inject()
    private val eventHandler: IWorkEventHandler? by application.inject()
    private val msgHandler: IWorkMsgHandler? by application.inject()

    init {
        onStarted {
            val configService: ConfigService by application.inject()
            val list = configService.findAgentConfigList(true)//只加载enabled的

            var count = 0
            //数据库中有配置时的情形
            if (Work.isMulti) {
                list.forEach {
                    count += configWxWorkMulti(it, msgHandler, eventHandler, agentController)
                }
            } else {
                list.firstOrNull { it.enable }?.let {
                    count = configWxWorkSingle(it, msgHandler, eventHandler, agentController)
                }
            }

            //configMsgNotifyUrl(application)

            if (count == 0) {
                log.warn("no agent context initialized, please check wxWorkConfig in DB")
            } else {
                log.info("$count agents context initialized from DB")
            }
            logConfig()
        }

    }

    private fun logConfig() {
        log.info("================config WxWork=======================")
        WorkMulti.ApiContextMap.forEach { (t, _) ->
            log.info("Work corpId=${t}")
        }
        log.info("Work.oauthInfoPath=${Work.oauthInfoPath}?agentId={AgentId}&corpId={corpId}&host={host?}")
        log.info("Work.oauthNotifyPath=${Work.oauthNotifyPath}?code=CODE&state=STATE")
        log.info("Work.oauthNotifyWebAppUrl=${Work.oauthNotifyWebAppUrl}?code=OK&state=STATE&corpId={corpId?}&agentId={agentId?}&openId={openId?}&userId={userId?}&externalUserId={externalUserId?}")
        log.info("Work.jsSdkSignaturePath=${Work.jsSdkSignaturePath}?corpId=corpId&agentId=agentId")
        log.info("====================config WxWork=====================")
    }
}


