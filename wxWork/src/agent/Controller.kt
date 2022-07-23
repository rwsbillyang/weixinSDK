/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-27 18:04
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

package com.github.rwsbillyang.wxWork.agent


import com.github.rwsbillyang.wxSDK.work.AgentApi
import com.github.rwsbillyang.wxSDK.work.ContactsApi
import com.github.rwsbillyang.wxSDK.work.ExternalContactsApi
import com.github.rwsbillyang.wxSDK.work.TagApi
import com.github.rwsbillyang.wxWork.contacts.ContactHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException


/**
 * 内建应用中，用于同步最新的agent信息，以及查看oauth认证后的用户是否有可见性权限
 * 有权限的话后续login时颁发token，否则禁止访问
 * */
class AgentController : KoinComponent {
    private val log = LoggerFactory.getLogger("AgentController")
    private val service: AgentService by inject()
    private val contactHelper: ContactHelper by inject()

    /**
     * 同步最新agent信息，同时把部门和tag中的用户以及部门，等等所有用户添加到agent中的可见用户范围内
     * 管理员手工调用
     * @param agentId 待获取的agent的id
     * @return 返回可见范围的用户数量
     * TODO: 支持isv multi多应用
     * */
    fun syncAgent(corpId: String, agentId: Int): Int {
        try {
            val agentApi = AgentApi(corpId, agentId, null)

            val res = agentApi.detail(agentId)
            if (res.isOK()) {
                val agent = Agent(
                    corpId,
                    agentId,
                    res.name,
                    res.logo,
                    res.description,
                    res.allowUsers?.list?.map { it.id },
                    res.allowDepartment?.list,
                    res.allowTags?.list,
                    res.close,
                    res.redirectDomain,
                    res.reportLocationFlag,
                    res.isReportEnter,
                    res.homeUrl
                )
                service.saveAgent(agent)

                syncAllowUsersDetail(agent)

                return 1
            }
        } catch (e: IllegalArgumentException) {
            log.warn("syncAgent IllegalArgumentException")
        }
        return 0
    }

    //系统启动时调用，若无agent则初始化
    fun syncAgentIfNotExit(corpId: String, agentId: Int){
        val agent = service.findAgent(agentId, corpId)
        if(agent == null)
            syncAgent(corpId, agentId)
    }

    //同步agent中所有可见用户详情及openId
    fun syncAllowUsersDetail(agent: Agent) = GlobalScope.launch {

        //记录下tags标记的部门和成员
        val departments = mutableSetOf<Int>()
        val users = mutableSetOf<String>()

        val corpId = agent.corpId
        val agentId = agent.id
        val tagApi = TagApi(corpId, agentId, null)

        agent.tagList?.forEach {
            val res1 = tagApi.detail(it)//获取tag标签的所有部门和成员
            if (res1.isOK()) {
                res1.partyList?.let { departments.addAll(it) }
                res1.userList?.let { users.addAll(it.map { it.userId }) }
            }
        }

        agent.depList?.let {
            departments.addAll(it)
        }

        val contactsApi = ContactsApi(corpId, agentId, null)
        val users2 = service.departmentsToUsers(contactsApi, departments)//获取部门的所有成员
        if (!users2.isNullOrEmpty()) {
            users += users2
        }

        val externalContactsApi = ExternalContactsApi(corpId, agentId, null)

        //tags和部门标识的可见成员
        if (!users.isNullOrEmpty()) {
            service.addAllowUsers(corpId, users.toList(), agentId)//更新agent可见范围成员

            //获取用户详情，及其外部联系人详情
            users.forEach {
                contactHelper.syncContactDetail(contactsApi, it, corpId)
                contactHelper.syncExternalsOfUser(externalContactsApi, corpId, it, 0)
            }
        }

        //获取用户详情，及其外部联系人详情
        agent.userList?.forEach {
            contactHelper.syncContactDetail(contactsApi, it, corpId)
            contactHelper.syncExternalsOfUser(externalContactsApi, corpId, it, 0)
        }
    }

}