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
    fun onTicket(suiteId:String, msg: SuiteTicket)
    fun onAuthCode(suiteId:String, msg: AuthCode)
    fun onAuthChange(suiteId:String, msg: AuthChange)
    fun onAuthCancel(suiteId:String, msg: AuthCancel)
    fun onContactAdd(suiteId:String, msg: ContactAdd)
    fun onContactUpdate(suiteId:String, msg: ContactUpdate)
    fun onContactDel(suiteId:String, msg: ContactDel)
    fun onDepartmentAdd(suiteId:String, msg: DepartmentAdd)
    fun onDepartmentUpdate(suiteId:String, msg: DepartmentUpdate)
    fun onDepartmentDel(suiteId:String, msg: DepartmentDel)
    fun onTagContactChange(suiteId:String, msg: TagContactChange)
    fun onAgentShareChange(suiteId:String, msg: AgentShareChange)

    fun onDefault(suiteId:String, msg: SuiteInfo)
}


class DefaultSuiteInfoHandler: ISuiteInfoHandler {
    private val log: Logger = LoggerFactory.getLogger("DefaultSuiteInfoHandler")
    override fun onTicket(suiteId:String, msg: SuiteTicket) {
        log.info("onTicket: Not yet implemented")
    }

    override fun onAuthCode(suiteId:String, msg: AuthCode) {
        log.info("onAuthCode: Not yet implemented")
    }

    override fun onAuthChange(suiteId:String, msg: AuthChange) {
        log.info("onAuthChange: Not yet implemented")
    }

    override fun onAuthCancel(suiteId:String, msg: AuthCancel) {
        log.info("onAuthCancel: Not yet implemented")
    }

    override fun onContactAdd(suiteId:String, msg: ContactAdd) {
        log.info("onContactAdd: Not yet implemented")
    }

    override fun onContactUpdate(suiteId:String, msg: ContactUpdate) {
        log.info("onContactUpdate: Not yet implemented")
    }

    override fun onContactDel(suiteId:String, msg: ContactDel) {
        log.info("onContactDel: Not yet implemented")
    }

    override fun onDepartmentAdd(suiteId:String, msg: DepartmentAdd) {
        log.info("onDepartmentAdd: Not yet implemented")
    }

    override fun onDepartmentUpdate(suiteId:String, msg: DepartmentUpdate) {
        log.info("onDepartmentUpdate: Not yet implemented")
    }

    override fun onDepartmentDel(suiteId:String, msg: DepartmentDel) {
        log.info("onDepartmentDel: Not yet implemented")
    }

    override fun onTagContactChange(suiteId:String, msg: TagContactChange) {
        log.info("onTagContactChange: Not yet implemented")
    }

    override fun onAgentShareChange(suiteId:String, msg: AgentShareChange) {
        log.info("onAgentShareChange:  Not yet implemented")
    }

    override fun onDefault(suiteId:String, msg: SuiteInfo) {
        log.info("onDefault: Not yet implemented")
    }

}