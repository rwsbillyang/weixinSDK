/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-07-31 18:04
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

package com.github.rwsbillyang.wxSDK.work.isv

import org.slf4j.Logger
import org.slf4j.LoggerFactory


interface ISuiteInfoHandler {
    fun onTicket(msg: SuiteTicket)
    fun onAuthCode(msg: AuthCode)
    fun onAuthChange(msg: AuthChange)
    fun onAuthCancel(msg: AuthCancel)
    fun onContactAdd(msg: ContactAdd)
    fun onContactUpdate(msg: ContactUpdate)
    fun onContactDel(msg: ContactDel)
    fun onDepartmentAdd(msg: DepartmentAdd)
    fun onDepartmentUpdate(msg: DepartmentUpdate)
    fun onDepartmentDel(msg: DepartmentDel)
    fun onTagContactChange(msg: TagContactChange)
    fun onAgentShareChange(msg: AgentShareChange)

    fun onDefault(msg: SuiteInfo)
}


class DefaultSuiteInfoHandler: ISuiteInfoHandler {
    private val log: Logger = LoggerFactory.getLogger("DefaultSuiteInfoHandler")
    override fun onTicket(msg: SuiteTicket) {
        log.info("onTicket: Not yet implemented")
    }

    override fun onAuthCode(msg: AuthCode) {
        log.info("onAuthCode: Not yet implemented")
    }

    override fun onAuthChange(msg: AuthChange) {
        log.info("onAuthChange: Not yet implemented")
    }

    override fun onAuthCancel(msg: AuthCancel) {
        log.info("onAuthCancel: Not yet implemented")
    }

    override fun onContactAdd(msg: ContactAdd) {
        log.info("onContactAdd: Not yet implemented")
    }

    override fun onContactUpdate(msg: ContactUpdate) {
        log.info("onContactUpdate: Not yet implemented")
    }

    override fun onContactDel(msg: ContactDel) {
        log.info("onContactDel: Not yet implemented")
    }

    override fun onDepartmentAdd(msg: DepartmentAdd) {
        log.info("onDepartmentAdd: Not yet implemented")
    }

    override fun onDepartmentUpdate(msg: DepartmentUpdate) {
        log.info("onDepartmentUpdate: Not yet implemented")
    }

    override fun onDepartmentDel(msg: DepartmentDel) {
        log.info("onDepartmentDel: Not yet implemented")
    }

    override fun onTagContactChange(msg: TagContactChange) {
        log.info("onTagContactChange: Not yet implemented")
    }

    override fun onAgentShareChange(msg: AgentShareChange) {
        log.info("onAgentShareChange:  Not yet implemented")
    }

    override fun onDefault(msg: SuiteInfo) {
        log.info("onDefault: Not yet implemented")
    }

}