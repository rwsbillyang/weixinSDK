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


import com.github.rwsbillyang.ktorKit.server.LifeCycle
import com.github.rwsbillyang.wxSDK.work.SysAgentKey
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.WorkMulti
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkEventHandler
import com.github.rwsbillyang.wxWork.agent.AgentController
import com.github.rwsbillyang.wxWork.config.ConfigService
import com.github.rwsbillyang.wxWork.config.WxWorkAgentConfig
import io.ktor.server.application.*
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory


/**
 * 单个应用的多企业配置
 * @param config 企业级配置
 * @param application 用于注入eventHandler
 * @param isSysAgent true：企业微信官方应用 false：自己创建的agent
 * @param agentController 调用者注入后传递过来，用于同步agent信息
 * */
internal fun configWxWork(
    config: WxWorkAgentConfig,
    application: Application,
    isSysAgent: Boolean = false,
    agentController: AgentController? = null
) {
    val log = LoggerFactory.getLogger("configWxWork")
    if(isSysAgent){
        if(config.enable){
            val handler = when(config.agentId){
                SysAgentKey.Contact.name -> application.inject<IWorkEventHandler>(named(config.agentId))
                SysAgentKey.WxKeFu.name -> application.inject<IWorkEventHandler>(named(config.agentId))
                else -> null
            }?.value

            WorkMulti.config(
                config.corpId, config.agentId, config.secret, config.token, config.aesKey,
                config.private, config.enableJsSdk, config.enableMsg,
                null, handler
            )
            log.info("enable sysAgent: corpId=${config.corpId}, agentId=${config.agentId}")
        }else{
            WorkMulti.reset(config.corpId, config.agentId)
            log.info("remove sysAgent: corpId=${config.corpId}, agentId=${config.agentId}")
        }
    }else{
        if(config.enable){
            if(WorkMulti.msgHandlerCount.addAndGet(1) == 1)
                WorkMulti.defaultWorkMsgHandler = WxWorkMsgHandler()
            if(WorkMulti.eventHandlerCount.addAndGet(1) == 1)
                WorkMulti.defaultWorkEventHandler = WxWorkEventHandler()

            WorkMulti.config(
                config.corpId, config.agentId, config.secret, config.token, config.aesKey,
                config.private, config.enableJsSdk, config.enableMsg
            )

            if(agentController != null){
                log.info("syncAgent: corpId=${config.corpId}, agentId=${config.agentId}")
                //企业应用的可见用户更新，需在调用Work.config配置完work后进行同步调用
                agentController.syncAgentIfNotExit(config.corpId, config.agentId) //syncContacts改由管理员手工同步
            }
            log.info("enable agent: corpId=${config.corpId}, agentId=${config.agentId}")
        }else{
            WorkMulti.reset(config.corpId, config.agentId)
            if(WorkMulti.msgHandlerCount.addAndGet(-1) == 0)
                WorkMulti.defaultWorkMsgHandler = null
            if(WorkMulti.eventHandlerCount.addAndGet(-1) == 0)
                WorkMulti.defaultWorkEventHandler = null

            log.info("remove agent: corpId=${config.corpId}, agentId=${config.agentId}")
        }
    }
}


/**
 * 从数据库中加载企业微信配置信息
 * 可能存在多个应用，每个应用存在于多个企业
 * */
class OnStartConfigWxWork(application: Application) : LifeCycle(application) {
    private val log = LoggerFactory.getLogger("WxWorkConfig")
    private val agentController: AgentController by application.inject()

    init {
        onStarted {
            val configService: ConfigService by application.inject()
            val agentList = configService.findAgentConfigList(true)//只加载enabled的
            val sysAgentList = configService.findSysAgentConfigList(true)//只加载enabled的

            //数据库中有配置时的情形
            agentList.forEach {
                configWxWork(it, application,false, agentController)
            }

            sysAgentList.forEach {
                configWxWork(it, application, true)
            }

            //configMsgNotifyUrl(application)

            logConfig()
        }

    }

    private fun logConfig() {
        log.info("=====config WxWork start=====")
        WorkMulti.ApiContextMap.forEach { (corpId, corpCtx) ->
            corpCtx.agentMap.forEach { agentId, _ ->
                log.info("msgNotifyUri(if enabled): ${Work.msgNotifyUri}/${corpId}/${agentId}")
            }
        }

        log.info("Work.oauthNotifyPath=${Work.oauthNotifyPath}?code=CODE&state=STATE")
        log.info("Work.oauthNotifyWebAppUrl=${Work.oauthNotifyWebAppUrl}?code=OK&state=STATE&corpId={corpId?}&agentId={agentId?}&openId={openId?}&userId={userId?}&externalUserId={externalUserId?}")
        log.info("Work.jsSdkSignaturePath=${Work.jsSdkSignaturePath}?corpId=corpId&agentId=agentId")

        log.info("WorkMulti.eventHandlerCount: ${WorkMulti.eventHandlerCount}")
        log.info("WorkMulti.msgHandlerCount: ${WorkMulti.msgHandlerCount}")

        log.info("=====config WxWork end=====")
    }
}


//
///**
// * 单个应用的单企业配置
// * */
//internal fun configWxWorkSingle(
//    config: WxWorkAgentConfig,
//    msgHandler: IWorkMsgHandler?,
//    eventHandler: IWorkEventHandler?,
//    agentController: AgentController
//): Int {
//    var count = 0
//    with(config) {
//        WorkSingle.config(
//            corpId, agentId, secret, token, aesKey,
//            private, enableJsSdk, enableMsg,
//            customMsgHandler = msgHandler, //所有应用使用同一个msgHandler（无特殊处理）和eventHandler（主要用于通讯录等变化通知接收处理）
//            customEventHandler = eventHandler  //多个应用时可以只有一个接收通讯录变化即可
//        )
//
//        count++
//
//        //企业应用的可见用户更新，需在调用Work.config配置完work后进行同步调用
//        agentController.syncAgent(corpId, agentId)//同步agent，以及agent的可见成员详情，可见成员的外部联系人详情
//
//       // systemAccessTokenKeyMap?.forEach { (k, v) -> WorkSingle.config(k, v) }
//    }
//    return count
//}

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
