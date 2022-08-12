/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-08 17:26
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


import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.isv.*
import com.github.rwsbillyang.wxWork.agent.AgentController
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

//suiteId来自于接收消息路径中的参数
class WxWorkIsvSuiteInfoHandler: ISuiteInfoHandler, KoinComponent
{
    private val log = LoggerFactory.getLogger("MySuiteInfoHandler")

    private val service: IsvCorpService by inject()
    private val agentController: AgentController by inject()

    /**
     * 推送suite_ticket
     * 企业微信服务器会定时（每十分钟）推送ticket。ticket会实时变更，并用于后续接口的调用。
     * */
    override fun onTicket(suiteId: String, msg: SuiteTicket) {
        val suiteId2 = msg.suiteInfo.suiteId?:suiteId
        val ticket = msg.ticket
        if (ticket.isNullOrBlank()) {
            log.warn("suiteId=$suiteId or ticket=$ticket is null")
            return
        }

        //配置IsvWork之step2
        //必须系统启动时配置好ThirdPartyWork中的suiteId和secret
        //第三个参数是提供corpId和permanentCode数据对列表，目的是更新ticket时，若存在永久授权码，则可同时配置accessToken，无需步骤3了
        if (Work.isMulti) {
            IsvWorkMulti.configSuiteToken(suiteId2, ticket) {
                service.findCorpInfoList(suiteId2)
                    .filter { it.corpInfo?.corpid != null && it.permanentCode != null }
                    .map { Pair(it.corpInfo!!.corpid, it.permanentCode!!) }
            }
        } else {
            IsvWorkSingle.configSuiteToken(suiteId2, ticket) {
                service.findCorpInfoList(suiteId2)
                    .filter { it.corpInfo?.corpid != null && it.permanentCode != null }
                    .map { Pair(it.corpInfo!!.corpid, it.permanentCode!!) }
            }
        }
    }

    override fun onAuthCancel(suiteId: String, msg: AuthCancel) {
        val suiteId2 = msg.suiteInfo.suiteId?:suiteId
        val corpId = msg.corpId
        if(corpId.isNullOrBlank()){
            log.warn("onAuthCancel: no corpId")
            return
        }
        if(Work.isMulti){
            IsvWorkMulti.removeCorpAuth(suiteId2, corpId)
        }else{
            IsvWorkSingle.removeCorpAuth(corpId)
        }
        
        service.updateStatus(CorpInfo.id(suiteId2, corpId), CorpInfo.STATUS_CANCELED)
    }

    override fun onAuthChange(suiteId: String,msg: AuthChange) {
        log.info("ignore onAuthChange: suiteId=${msg.suiteInfo.suiteId}, corpId=${msg.corpId}.")
    }

    override fun onAgentShareChange(suiteId: String,msg: AgentShareChange) {
        log.info("ignore onAgentShareChange: suiteId=${msg.suiteInfo.suiteId}, corpId=${msg.corpId}, agentId=${msg.agentId}")
    }

    /**
     * 从企业微信应用市场发起授权时，企业微信后台会推送授权成功通知。
     * */
    override fun onAuthCode(suiteId: String,msg: AuthCode) {
        val suiteId2 = msg.suiteInfo.suiteId?:suiteId
        val authCode = msg.authCode //得到临时授权码
        if(authCode.isNullOrBlank()){
            log.warn("onAuthCode: no  authCode")
        }else{
            //获取永久授权码，同时得到accessToken、授权企业信息
            val permanentCodeInfo = ThirdPartyApi(suiteId2).getPermanentCode(authCode)
            if(permanentCodeInfo.isOK()){
                runBlocking {
                    launch {
                        handlePermanentAuthInfo(suiteId2, permanentCodeInfo)
                    }
                }
            }else{
                log.warn("onAuthCode: fail to get permanentCode: " + permanentCodeInfo.errMsg)
            }
        }
    }
    /**
     * 当管理员或成员授权后，接到授权通知（得到预授权码），再用预授权码换取永久授权码以及授权信息和accessToken，
     *  本函数用于对永久授权码和授权信息的处理，保存更新等
     * */
    fun handlePermanentAuthInfo(suiteId: String,info: ResponsePermanentCodeInfo){
        val corpId = info.auth_corp_info?.corpid
        val permanentCode = info.permanent_code
        if(corpId.isNullOrBlank() || permanentCode.isNullOrBlank()){
            log.warn("no corpId or permanentCode in onGetPermanentAuthInfo")
            return
        }
        val corpInfo = CorpInfo(CorpInfo.id(suiteId, corpId),suiteId, corpId, CorpInfo.STATUS_ENABLED,
            System.currentTimeMillis(), info.permanent_code,info.dealer_corp_info,
            info.auth_corp_info, info.auth_info, info.register_code_info)

        service.save(corpInfo)

        //更新agent信息
        val agentId = corpInfo.authInfo?.agent?.firstOrNull()?.agentid
        if(agentId != null){
            agentController.syncAgent(corpId, agentId)
        }

        //配置ThirdPartyWork之step3
        if(Work.isIsv){
            IsvWorkMulti.configAccessToken(suiteId, corpId, permanentCode, info.access_token)
        }else{
            IsvWorkSingle.configAccessToken(suiteId, corpId, permanentCode, info.access_token)
        }
    }



    override fun onContactAdd(suiteId: String,msg: ContactAdd) {
        log.info("onContactAdd: Not yet implemented")
    }

    override fun onContactDel(suiteId: String,msg: ContactDel) {
        log.info("onContactDel: Not yet implemented")
    }

    override fun onContactUpdate(suiteId: String,msg: ContactUpdate) {
        log.info("onContactUpdate: Not yet implemented")
    }

    override fun onDefault(suiteId: String,msg: SuiteInfo) {
        log.info("onDefault: Not yet implemented")
    }

    override fun onDepartmentAdd(suiteId: String,msg: DepartmentAdd) {
        log.info("onDepartmentAdd: Not yet implemented")
    }

    override fun onDepartmentDel(suiteId: String,msg: DepartmentDel) {
        log.info("onDepartmentDel:Not yet implemented")
    }

    override fun onDepartmentUpdate(suiteId: String,msg: DepartmentUpdate) {
        log.info("onDepartmentUpdate: Not yet implemented")
    }

    override fun onTagContactChange(suiteId: String,msg: TagContactChange) {
        log.info("onTagContactChange: Not yet implemented")
    }


}