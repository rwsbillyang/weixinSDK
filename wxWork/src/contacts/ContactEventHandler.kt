/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-05 23:13
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

package com.github.rwsbillyang.wxWork.contacts


import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.work.ContactsApi
import com.github.rwsbillyang.wxSDK.work.ExternalContactsApi
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.inMsg.*
import com.github.rwsbillyang.wxWork.agent.AgentService
import com.github.rwsbillyang.wxWork.isv.IsvCorpService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class ContactEventHandler: DefaultWorkEventHandler(), KoinComponent {
    private val log = LoggerFactory.getLogger("ContactEventHandler")
    private val agentService: AgentService by inject()
    private val isvCorpService: IsvCorpService by inject()
    private val contactHelper: ContactHelper by inject()
    private val contactService: ContactService by inject()


    //内建应用+第三方应用： 成员已经加入企业，管理员添加成员到应用可见范围(或移除可见范围)时; 成员已经在应用可见范围，成员加入(或退出)企业时
    override fun onSubscribeEvent(appId: String, agentId: String?, e: WorkSubscribeEvent): ReBaseMSg? {
        val userId = e.fromUserName //FromUserName 成员UserID
        val corpId = e.toUserName //ToUserName	企业微信CorpID
        val agentIdInMsg = e.agentId
        val agentId_ = agentIdInMsg ?: agentId
        if (userId.isNullOrBlank() || corpId == null || agentId_ == null) {
            log.warn("onsubscribeEvent: userId/corpId/agentId2 isNull: userId=$userId, appId=$appId, agentId_=$agentId_")
            return null
        }


        if (Work.isIsv) {
            //ISV模式下corpId是suiteId，agentId为空，来自于后台设置的接收消息路径中的参数
            isvCorpService.addAllowUser(appId, corpId, userId)
            agentService.addAllowUser(corpId, userId, agentId_) //TODO：agentId_为空的话通过suiteId和corpId找到agentId
        } else {
            if (appId != corpId || agentId != agentIdInMsg) {
                log.warn("onSubscribeEvent: corpId or agentId not equal: userId=$userId, appId=$appId, corpId=$corpId, agentId=$agentId,agentIdInMsg=$agentIdInMsg, please check settings in wxwork admin")
            }
            agentService.addAllowUser(corpId, userId, agentId_)
        }

        return null
    }

    //内建应用+第三方应用 会有重复通知
    override fun onUnsubscribeEvent(appId: String, agentId: String?, e: WorkUnsubscribeEvent): ReBaseMSg? {
        val userId = e.fromUserName //FromUserName 成员UserID
        val corpId = e.toUserName //ToUserName	企业微信CorpID
        val agentIdInMsg = e.agentId
        val agentId_ = agentIdInMsg ?: agentId
        if (userId.isNullOrBlank() || corpId == null || agentId_ == null) {
            log.warn("onUnsubscribeEvent: userId/corpId/agentId2 isNull: userId=$userId, appId=$appId, agentId_=$agentId_")
            return null
        }

        if (Work.isIsv) {
            //ISV模式下corpId是suiteId，agentId为空，来自于后台设置的接收消息路径中的参数
            isvCorpService.removeAllowUser(appId, corpId, userId)
            agentService.removeAllowUser(corpId, userId, agentId_)
        } else {
            if (appId != corpId || agentId != agentIdInMsg) {
                log.warn("onUnsubscribeEvent: corpId or agentId not equal: userId=$userId, appId=$appId, corpId=$corpId, agentId=$agentId,agentIdInMsg=$agentIdInMsg, please check settings in wxwork admin")
            }
            agentService.removeAllowUser(corpId, userId, agentId_)
        }

        return null
    }

    //内建应用：成员添加外部联系人， 将客户添加到自己的ContactExtra.ids列表里，同时获取ExternalContactDetail
    //关于重试的消息排重，有msgid的消息推荐使用msgid排重。事件类型消息推荐使用FromUserName + CreateTime排重。
    //客户扫码添加我时，也发生ExternalContactAddEvent事件，而非ExternalContactAddEvent。 <xml><ToUserName><![CDATA[ww5f4c472a66331eeb]]></ToUserName><FromUserName><![CDATA[sys]]></FromUserName><CreateTime>1632820219</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[change_external_contact]]></Event><ChangeType><![CDATA[add_external_contact]]></ChangeType><UserID><![CDATA[ycg]]></UserID><ExternalUserID><![CDATA[wmFOeKDQAAPQofu3BfgwNDZFc1vQXGjg]]></ExternalUserID><State><![CDATA[YTxj4yDKAE_NFbc_]]></State><WelcomeCode><![CDATA[Ar_p4_nvAR-RdjEUVWWOnfChYLO31PAVOdAkeZHxg4o]]></WelcomeCode></xml>
    override fun onExternalContactAddEvent(appId: String, agentId: String?, e: ExternalContactAddEvent): ReBaseMSg? = runBlocking{
        //ToUserName	企业微信CorpID
        val corpId = e.toUserName ?: appId
        if (corpId != appId) {
            log.warn("onExternalContactAddEvent: appId=$appId, corpId=$corpId, not equal, agentId=$agentId,e.agentId=${e.agentId} ")
        }
        val externalUserId = e.externalUserId
        val userId = e.userId
        if (externalUserId == null || userId == null) {
            log.warn("onExternalContactAddEvent: externalUserId=${externalUserId}, e.userId=${userId}, do nothing")
            return@runBlocking null
        }

        launch {
            contactService.upsertContactCustomerId(corpId, userId, externalUserId)
            contactHelper.syncExternalDetail(corpId, e.agentId ?: agentId, null, externalUserId)
            val c = contactService.findContact(userId, corpId)
            contactService.addRelationChange(
                e.createTime,
                corpId,
                userId,
                c?._id,
                externalUserId,
                RelationChange.TypeUserAddExternal,
                null
            )
        }

        return@runBlocking null
    }

    //内建应用：外部联系人添加了配置了客户联系功能且开启了免验证的成员时（此时成员尚未确认添加对方为好友）
    //将客户添加到自己的ContactExtra.ids列表里，同时获取ExternalContactDetail
    override fun onExternalContactHalfAddEvent(appId: String, agentId: String?, e: ExternalContactAddEvent): ReBaseMSg? = runBlocking {
        //ToUserName	企业微信CorpID
        val corpId = e.toUserName ?: appId
        if (corpId != appId) {
            log.warn("onExternalContactHalfAddEvent: appId=$appId, corpId=$corpId, not equal, agentId=$agentId,e.agentId=${e.agentId} ")
        }
        val externalUserId = e.externalUserId
        val userId = e.userId
        if (externalUserId == null || userId == null) {
            log.warn("onExternalContactHalfAddEvent: externalUserId=${externalUserId}, e.userId=${userId}, do nothing")
            return@runBlocking null
        }

        launch {
            contactService.upsertContactCustomerId(corpId, userId, externalUserId)
            contactHelper.syncExternalDetail(corpId, e.agentId ?: agentId, null, externalUserId)
            val c = contactService.findContact(userId, corpId)
            contactService.addRelationChange(
                e.createTime,
                corpId,
                userId,
                c?._id,
                externalUserId,
                RelationChange.TypeExternalAddUser,
                e.state
            )
        }

        return@runBlocking null
    }

    //内建应用：配置了客户联系功能的成员删除外部联系人时,将客户从自己的ContactExtra.ids列表里移除，但ExternalContactDetail里的follow信息不变
    override fun onExternalContactDelEvent(appId: String, agentId: String?, e: ExternalContactDelEvent): ReBaseMSg? = runBlocking{
        //ToUserName	企业微信CorpID
        val corpId = e.toUserName ?: appId
        if (corpId != appId) {
            log.warn("onExternalContactDelEvent: appId=$appId, corpId=$corpId, not equal, agentId=$agentId,e.agentId=${e.agentId} ")
        }
        val externalUserId = e.externalUserId
        val userId = e.userId
        if (externalUserId == null || userId == null) {
            log.warn("onExternalContactDelEvent: externalUserId=${externalUserId}, e.userId=${userId}, do nothing")
            return@runBlocking null
        }

        launch {
            contactService.removeContactCustomerId(corpId, userId, externalUserId)//成员删除客户
            //contactHelper.syncExternalContactDetail(corpId, e.agentId?:agentId, null, externalUserId)
            val c = contactService.findContact(userId, corpId)
            contactService.addRelationChange(
                e.createTime,
                corpId,
                userId,
                c?._id,
                externalUserId,
                RelationChange.TypeUserDelExternal,
                null
            )
        }

        return@runBlocking null
    }

    //内建应用：配置了客户联系功能的成员被外部联系人删除时
    //ExternalContactDetail里的follow列表将自己移除，但自己的ContactExtra.ids列表仍有客户
    override fun onExternalContactDelFollowEvent(
        appId: String,
        agentId: String?,
        e: ExternalContactUpdateEvent
    ): ReBaseMSg?  = runBlocking{
        //ToUserName	企业微信CorpID
        val corpId = e.toUserName ?: appId
        if (corpId != appId) {
            log.warn("onExternalContactDelFollowEvent: appId=$appId, corpId=$corpId, not equal, agentId=$agentId,e.agentId=${e.agentId} ")
        }
        val externalUserId = e.externalUserId
        val userId = e.userId
        if (externalUserId == null || userId == null) {
            log.warn("onExternalContactDelFollowEvent: externalUserId=${externalUserId}, e.userId=${userId}, do nothing")
            return@runBlocking null
        }

        launch {
            val c = contactService.findContact(userId, corpId)
            contactService.addRelationChange(
                e.createTime,
                corpId,
                userId,
                c?._id,
                externalUserId,
                RelationChange.TypeExternalDelUser,
                null
            )
            //成员并未删除客户
            //contactService.removeContactCustomerId(corpId, userId, externalUserId)
            //删除其对应的成员
            contactService.removeExternalContactFollow(corpId, externalUserId, userId)
        }

        return@runBlocking null
    }


    //内建应用：企业将客户分配给新的成员接替后，客户添加失败时回调该事件 接替失败的原因, customer_refused-客户拒绝， customer_limit_exceed-接替成员的客户数达到上限
    override fun onExternalContactTransferFailEvent(
        appId: String,
        agentId: String?,
        e: ExternalContactTransferFailEvent
    ): ReBaseMSg?  = runBlocking{

        log.info("onExternalContactTransferFailEvent: appId=$appId, agentId=$agentId, e.failReason=${e.failReason}, userId=${e.userId}, externalId=${e.externalUserId}")

        //ToUserName	企业微信CorpID
        val corpId = e.toUserName ?: appId
        if (corpId != appId) {
            log.warn("onExternalContactTransferFailEvent: appId=$appId, corpId=$corpId, not equal, agentId=$agentId,e.agentId=${e.agentId} ")
        }
        val externalUserId = e.externalUserId
        val userId = e.userId
        if (externalUserId == null || userId == null) {
            log.warn("onExternalContactTransferFailEvent: externalUserId=${externalUserId}, e.userId=${userId}, do nothing")
            return@runBlocking null
        }

        launch {
            val c = contactService.findContact(userId, corpId)
            contactService.addRelationChange(
                e.createTime,
                corpId,
                userId,
                c?._id,
                externalUserId,
                RelationChange.TypeTransferFail,
                e.failReason
            )
        }


        return@runBlocking null
    }

    //内建应用：成员修改外部联系人的备注、手机号或标签时
    override fun onExternalContactUpdateEvent(appId: String, agentId: String?, e: ExternalContactUpdateEvent): ReBaseMSg? = runBlocking{
        //ToUserName	企业微信CorpID
        val corpId = e.toUserName ?: appId
        if (corpId != appId) {
            log.warn("onExternalContactUpdateEvent: appId=$appId, corpId=$corpId, not equal, agentId=$agentId,e.agentId=${e.agentId} ")
        }
        val externalUserId = e.externalUserId
        val userId = e.userId
        if (externalUserId == null || userId == null) {
            log.warn("onExternalContactUpdateEvent: externalUserId=${externalUserId}, e.userId=${userId}, do nothing")
            return@runBlocking null
        }

        launch {
            contactHelper.syncExternalDetail(corpId, e.agentId ?: agentId, null, externalUserId)
        }

        return@runBlocking null
    }


    //内建应用：
    override fun onUserCreateEvent(appId: String, agentId: String?, e: WorkUserCreateEvent): ReBaseMSg?  = runBlocking{
        //ToUserName	企业微信CorpID
        val corpId = e.toUserName ?: appId
        if (corpId != appId) {
            log.warn("onUserCreateEvent: appId=$appId, corpId=$corpId, not equal, agentId=$agentId,e.agentId=${e.agentId} ")
        }
        launch {
            //val attrs: List<Attr>? = e.extAttrs?.mapNotNull { it.toAttr() }
            val contact = Contact(
                null, corpId, e.userId ?: "", e.name, e.mobile,
                e.department?.split(",")?.map { it.toInt() }, null,
                e.position, e.gender, e.email, e.isLeaderInDept?.split(",")?.map { it.toInt() },
                e.avatar, null, e.telephone, e.alias,  null,//attrs,
                e.status?.toInt()
            )
            contactService.insertContact(contact)
        }
        return@runBlocking null
    }

    //内建应用
    override fun onUserUpdateEvent(appId: String, agentId: String?, e: WorkUserUpdateEvent): ReBaseMSg? = runBlocking{
        //ToUserName	企业微信CorpID
        val corpId = e.toUserName ?: appId
        if (corpId != appId) {
            log.warn("onUserUpdateEvent: appId=$appId, corpId=$corpId, not equal, agentId=$agentId,e.agentId=${e.agentId} ")
        }
        launch {
            //val attrs: List<Attr>? = e.extAttrs?.mapNotNull { it.toAttr() }
            val newContact = Contact(
                null, corpId, e.userId ?: "", e.name, e.mobile,
                e.department?.split(",")?.map { it.toInt() }, null,
                e.position, e.gender, e.email, e.isLeaderInDept?.split(",")?.map { it.toInt() },
                e.avatar, null, e.telephone, e.alias, null,//attrs,
                e.status?.toInt()
            )
            contactService.updateContact(newContact, e.newUserID)

            //TODO: 更新ExternalContact中的follow并不是最新的情况
            // if(!e.newUserID.isNullOrBlank()){ }

        }
        return@runBlocking null
    }

    //内建应用：
    override fun onTagUpdateEvent(appId: String, agentId: String?, e: WorkTagUpdateEvent): ReBaseMSg? {
        //如果标签是agent对应的标签才更新可见范围
        val userId = e.fromUserName //FromUserName 成员UserID
        val corpId = e.toUserName //ToUserName	企业微信CorpID
        val agentIdInMsg = e.agentId
        val agentId_ = agentIdInMsg ?: agentId
        if (userId.isNullOrBlank() || corpId == null || agentId_ == null) {
            log.warn("onTagUpdateEvent: userId/corpId/agentId2 isNull: userId=$userId, appId=$appId, agentId_=$agentId_")
            return null
        }

        if (Work.isIsv) {
            //ISV模式下corpId是suiteId，agentId为空，来自于后台设置的接收消息路径中的参数
            isvCorpService.addAllowUser(appId, corpId, userId)
            agentService.addAllowUser(corpId, userId, agentId_) //TODO：agentId_为空的话通过suiteId和corpId找到agentId
        } else {
            if (appId != corpId || agentId != agentIdInMsg) {
                log.warn("onTagUpdateEvent: corpId or agentId not equal: userId=$userId, appId=$appId, corpId=$corpId, agentId=$agentId,agentIdInMsg=$agentIdInMsg, please check settings in wxwork admin")
            }
            val agent = agentService.findAgent(agentId_, corpId)
            val isExist = e.tagId?.toInt()?.let { agent?.tagList?.contains(it) } ?: false
            if (!isExist) return null


            val addUsers = mutableSetOf<String>()
            if (!e.addUserItems.isNullOrEmpty()) {
                addUsers.addAll(e.addUserItems!!)
            }
            val contactsApi = ContactsApi(corpId, agentId_, null)
            if (!e.addPartyItems.isNullOrEmpty()) {
                val set =
                    agentService.departmentsToUsers(contactsApi, e.addPartyItems?.map { it.toInt() }?.toSet())
                if (!set.isNullOrEmpty()) {
                    addUsers.addAll(set)
                }
            }
            if (addUsers.isNotEmpty()) {
                agentService.addAllowUsers(corpId, addUsers.toList(), agentId_)
                val externalContactsApi = ExternalContactsApi(corpId, agentId_, null)
                addUsers.forEach {
                    contactHelper.syncContactDetail(contactsApi, it, corpId)
                    contactHelper.syncExternalsOfUser(externalContactsApi, corpId, it, 0)
                }
            }

            val delUsers = mutableSetOf<String>()
            if (!e.delUserItems.isNullOrEmpty()) {
                delUsers.addAll(e.delUserItems!!)
            }
            if (!e.delPartyItems.isNullOrEmpty()) {
                val set =
                    agentService.departmentsToUsers(contactsApi, e.delPartyItems?.map { it.toInt() }?.toSet())
                if (!set.isNullOrEmpty()) {
                    delUsers.addAll(set)
                }
            }
            if (delUsers.isNotEmpty())
                agentService.removeAllowUsers(corpId, delUsers.toList(), agentId_)
        }
        return null
    }

    //内建应用：
    override fun onUserDelEvent(appId: String, agentId: String?, e: WorkUserDelEvent): ReBaseMSg? = runBlocking{
        log.info("onUserDelEvent: appId=$appId, agentId=$agentId, e.userId=${e.userId}")
        launch {
            e.userId?.let {
                contactService.delContact(it, e.toUserName ?: appId)
                //contactService.delContactExtra(it, e.baseInfo.toUserName ?: appId)
            }
        }
        return@runBlocking null
    }

    //内建应用：
    override fun onPartyCreateEvent(appId: String, agentId: String?, e: WorkPartyCreateEvent): ReBaseMSg? = runBlocking{
        val corpId = e.toUserName ?: appId
        val id = e.id
        if (id == null) {
            log.warn("onPartyCreateEvent: no id, corpId=$corpId")
            return@runBlocking null
        }
        launch {
            contactService.insertDepartment(
                Department(
                    null,
                    corpId,
                    id.toInt(),
                    e.name,
                    e.order?.toInt(),
                    e.parentId?.toInt()
                )
            )
        }
        return@runBlocking null
    }

    //内建应用：
    override fun onPartyDelEvent(appId: String, agentId: String?, e: WorkPartyDelEvent): ReBaseMSg? = runBlocking {
        val corpId = e.toUserName ?: appId
        val id = e.id
        if (id == null) {
            log.warn("onPartyDelEvent: no id, corpId=$corpId")
            return@runBlocking null
        }
        launch {
            contactService.delDepartment(corpId, id.toInt())
        }
        return@runBlocking null
    }

    //内建应用：
    override fun onPartyUpdateEvent(appId: String, agentId: String?, e: WorkPartyUpdateEvent): ReBaseMSg? = runBlocking {
        val corpId = e.toUserName ?: appId
        val id = e.id
        if (id == null) {
            log.warn("onPartyUpdateEvent: no id, corpId=$corpId")
            return@runBlocking null
        }
        launch {
            contactService.upsertDepartment(corpId, id.toInt(), e.name, e.parentId?.toInt())
        }
        return@runBlocking null
    }





}