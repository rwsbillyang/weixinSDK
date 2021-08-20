/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-07-31 16:41
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
import javax.xml.stream.XMLEventReader

/**
 * 预读取信息，因某些字段提前到SuiteInfo里面的字段之前，需要提前给读取保存下来
 * */
class PreReadInfo(
    val authCode: String? = null, //
    val authCorpId: String? = null //授权企业的CorpID
)


interface ISuiteInfo

open class SuiteInfo(
    val suiteId: String?,
    val infoType: String?,
    val timeStamp: Long?,
    val preReadInfo: PreReadInfo
) {
    companion object {
        const val INFO_TYPE_TICKET = "suite_ticket"

        const val INFO_TYPE_AUTH_CREATE = "create_auth"
        const val INFO_TYPE_AUTH_CHANGE = "change_auth"
        const val INFO_TYPE_AUTH_CANCEL = "cancel_auth"
        const val INFO_TYPE_CONTACT_CHANGE = "change_contact"
        const val INFO_TYPE_AGENT_SHARE_CHANGE = "share_agent_change"


        private val log: Logger = LoggerFactory.getLogger("SuiteInfo")

        fun fromXml(xml: String,reader: XMLEventReader): SuiteInfo
        {
            var suiteId: String? = null
            var infoType: String? = null
            var timeStamp: Long? = null

            var authCode: String? = null
            var authCorpId: String? = null

            var count = 0 //解析出的节点计数，用于报警
            while (reader.hasNext() && count < 5) {
                val event = reader.nextEvent()
                if (event.isStartElement) {
                    when(event.asStartElement().name.toString()){
                        "xml" -> count++
                        "SuiteId" -> {
                            count++
                            suiteId = reader.elementText
                        }
                        "InfoType" -> {
                            count++
                            infoType = reader.elementText
                        }
                        "AuthCode" -> authCode = reader.elementText
                        "AuthCorpId" -> authCorpId = reader.elementText
                        "TimeStamp"-> {
                            count++
                            timeStamp = reader.elementText?.toLong()
                            if(count < 4){
                                log.warn("WARN: Maybe lack of value SuiteId,InfoType,TimeStamp!!!  xml=$xml")
                            }
                        }

                    }
                }
            }

            return  SuiteInfo(suiteId,infoType,timeStamp, PreReadInfo(authCode, authCorpId))
        }
    }
}


class SuiteTicket(val suiteInfo: SuiteInfo):ISuiteInfo{
    var ticket: String? = null
    fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "SuiteTicket" -> {
                        ticket = reader.elementText
                        break
                    }
                }
            }
        }
    }
}

/**
 * 授权成功通知
 * */
class AuthCode(val suiteInfo: SuiteInfo):ISuiteInfo
{
    val authCode: String? //临时授权码,最长为512字节。用于获取企业永久授权码。10分钟内有效
        get() = suiteInfo.preReadInfo.authCode
}

/**
 * 变更授权通知
 * 当授权方（即授权企业）在企业微信管理端的授权管理中，修改了对应用的授权后，企业微信服务器推送变更授权通知。
 * 服务商接收到变更通知之后，需自行调用获取企业授权信息进行授权内容变更比对。
 * */
class AuthChange(val suiteInfo: SuiteInfo):ISuiteInfo{
    val corpId: String? //授权方的corpid
        get() = suiteInfo.preReadInfo.authCorpId
}

/**
 * 取消授权通知
 * */
class AuthCancel(val suiteInfo: SuiteInfo):ISuiteInfo
{
    val corpId: String? //授权方的corpid
        get() = suiteInfo.preReadInfo.authCorpId
}

/**
 * 联系人更新事件
 *
 * */
open class ContactChangeBase(val suiteInfo: SuiteInfo):ISuiteInfo{
    companion object{
        const val CHANGE_TYPE_CREATE_USER = "create_user"
        const val CHANGE_TYPE_UPDATE_USER = "update_user"
        const val CHANGE_TYPE_DEL_USER = "delete_user"
        const val CHANGE_TYPE_CREATE_PARTY = "create_party"
        const val CHANGE_TYPE_UPDATE_PARTY = "update_party"
        const val CHANGE_TYPE_DEL_PARTY = "delete_party"
        const val CHANGE_TYPE_UPDATE_TAG = "update_tag"

    }
    var changeType: String? = null
    open fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "ChangeType" -> {
                        changeType = reader.elementText
                        break
                    }
                }
            }
        }
    }
}

class ContactAdd(base: ContactChangeBase):ISuiteInfo
{
    //TODO：读取各字段
    fun read(reader: XMLEventReader){

    }
}
class ContactUpdate(base: ContactChangeBase):ISuiteInfo
{
    //TODO：读取各字段
    fun read(reader: XMLEventReader){

    }
}
class ContactDel(base: ContactChangeBase):ISuiteInfo
{
    //TODO：读取各字段
    fun read(reader: XMLEventReader){

    }
}

/**
 * 部门更新事件
 * */
class DepartmentAdd(base: ContactChangeBase):ISuiteInfo
{
    //TODO：读取各字段
    fun read(reader: XMLEventReader){

    }
}
class DepartmentUpdate(base: ContactChangeBase):ISuiteInfo
{
    //TODO：读取各字段
    fun read(reader: XMLEventReader){

    }
}
class DepartmentDel(base: ContactChangeBase):ISuiteInfo
{
    //TODO：读取各字段
    fun read(reader: XMLEventReader){

    }
}
/**
 * 标签成员变更事件
 * */
class TagContactChange(base: ContactChangeBase): ISuiteInfo
{
    var tagId: String? = null //标签Id
    var addUserItems: List<String>? = null //标签中新增的成员userid列表，用逗号分隔
    var delUserItems: List<String>? = null //标签中删除的成员userid列表，用逗号分隔
    var addPartyItems: List<String>? = null //标签中新增的部门id列表，用逗号分隔
    var delPartyItems: List<String>? = null //标签中删除的部门id列表，用逗号分隔
    fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "TagId" -> {
                        tagId = reader.elementText
                        break
                    }
                    "AddUserItems" -> {
                        addUserItems = reader.elementText?.split(",")
                        break
                    }
                    "DelUserItems" -> {
                        delUserItems = reader.elementText?.split(",")
                        break
                    }
                    "AddPartyItems" -> {
                        addPartyItems = reader.elementText?.split(",")
                        break
                    }
                    "DelPartyItems" -> {
                        delPartyItems = reader.elementText?.split(",")
                        break
                    }
                }
            }
        }
    }
}

/**
 * 共享应用事件变更通知
 * */
class AgentShareChange(val suiteInfo: SuiteInfo):ISuiteInfo{
    var appId: String? = null //旧的多应用套件中的对应应用id，新开发者请忽略
    var corpId: String? = null //上级企业corpid
    var agentId: String? = null //上级企业应用id
    fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "AppId" -> {
                        appId = reader.elementText
                        break
                    }
                    "CorpId" -> {
                        corpId = reader.elementText
                        break
                    }
                    "AgentId" -> {
                        agentId = reader.elementText
                        break
                    }
                }
            }
        }
    }
}