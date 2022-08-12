/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-20 22:06
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

import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.apiJson.UmiPagination
import com.github.rwsbillyang.ktorKit.apiJson.toObjectId
import com.github.rwsbillyang.wxSDK.work.DepartmentApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


import org.slf4j.LoggerFactory


/**
 * TODO: 临时放置于此，用于控制所使用的客服数量
 * */
var ContactsNumLimit = 200


class ContactController: KoinComponent {
    private val log = LoggerFactory.getLogger("ContactController")

    private val service: ContactService by inject()
    private val helper: ContactHelper by inject()
    //用于查询msg数量
   // private val chatMsgController: ChatMsgController by inject()

    /**
     * 获取内部联系人列表，非分页， 通常用于获取同意会话存档的所有联系人
     *
     * 默认排序，最终显示顺序由前端排序
     * */
    fun getSupportChatArchiveContactList(param: ContactListParams): DataBox<List<ContactBean>>{
        val pagination = if(param.umi == null)
            UmiPagination(ContactsNumLimit)
        else{
            param.pagination.apply{pageSize = ContactsNumLimit}
        }

        if(param.archive == 1){//要获取支持会话存档的成员列表
            val list = service.findContactArchiveList(param.toExtraFilter())
            val list2 = if(param.children > 0){ //获取子列表，也就是其客户列表
                list.mapNotNull {
                    val contactExtra = it
                    val customers = it.ids?.mapNotNull {
                        service.findExternalContact(it, param.corpId)?.toBean(contactExtra.userId,
                            0  /*chatMsgController.countChatMsg(param.corpId, contactId, it)*/,
                            service.findRelationChanges(contactExtra.corpId, contactExtra.userId, contactExtra._id, it)
                        )
                    }
                    service.findContact(contactExtra._id)?.toBean(customers)
                }
            }else{
                list.mapNotNull {
                    service.findContact(it._id)?.toBean(null)
                }
            }
            return DataBox.ok(list2)
        }else{//所有成员列表
            val list = service.findAllContactList(param.toFilter())
            val list2 = if(param.children > 0){ //是否获取其客户列表,默认不获取
                list.map {
                    val contact = it
                    val customers = service.findContactExtra(contact._id!!)?.ids?.mapNotNull {
                        service.findExternalContact(it, param.corpId)?.toBean(
                            contact.userId,
                            0  /*chatMsgController.countChatMsg(param.corpId, contactId, it)*/,
                            service.findRelationChanges(contact.corpId, contact.userId,contact._id, it))
                    }
                    it.toBean(customers)
                }
            }else{
                list.map { it.toBean(null, service.findContactExtra(it._id!!)?.ids?.size?.toLong()?:0L)}
            }

            return DataBox.ok(list2)
        }
    }

    /**
     * 从数据库中获取 contactDetail
     * @param id 即Contact._id
     * */
    fun getContactDetail(id: String?): DataBox<Contact> {
        if(id.isNullOrBlank()) {
            log.warn("getContactDetail: invalid parameter: corpId or contact._id is null")
            return DataBox.ko("invalid parameter: corpId or contact._id is null")
        }
        return DataBox.ok(service.findContact(id))
    }


    /**
     * 第三方/自建应用调用时，返回的跟进人follow_user仅包含应用可见范围之内的成员。
     * @param id ExternalContact._id 即：corpId/external_userId
     * */
    fun getExternalDetail(id: String?): DataBox<ExternalContact> {
        if(id.isNullOrBlank()) {
            log.warn("getExternalContactDetail: invalid parameter: corpId or contactId is null")
            return DataBox.ko("invalid parameter: corpId or contactId is null")
        }
        return DataBox.ok(service.findExternalContact(id.toObjectId()))
    }


    /**
     * 获取指定成员的客户列表。客户是指配置了客户联系功能的成员所添加的外部联系人。
     *
     * 没有配置客户联系功能的成员，所添加的外部联系人将不会作为客户返回。
     * 企业需要使用“客户联系”secret或配置到“可调用应用”列表中的自建应用secret所获取的accesstoken来调用
     * TODO: 需要配置，参见 https://work.weixin.qq.com/api/doc/90000/90135/92109#%E5%BC%80%E5%A7%8B%E5%BC%80%E5%8F%91
     * 第三方应用需拥有“企业客户”权限。
     * 注意：第三方/自建应用只能获取到可见范围内的配置了客户联系功能的成员。
     *
     * @param refreshType 0 不刷新客户信息，即已存在不刷新；1 刷新客户信息
     * */
    fun syncExternalsOfUser(corpId: String?, agentId: Int?, suiteId: String?, userId: String?, refreshType: Int?): DataBox<String>{
        if(corpId == null || userId == null){
            log.warn("no corpId or userId, corpId=$corpId, userId=$userId")
            return DataBox.ko("invalid parameter, corpId or userId")
        }

        val err = helper.syncExternalsOfUser(corpId, agentId, suiteId, userId, refreshType?:1)
        if(err != null)
        {
            return DataBox.ko(err)
        }
        return DataBox.ok("刷新客户列表成功")
    }


    /**
     * 获取某个企业corpId的某个用户的userId的渠道channelId的外部客户列表
     * 查询渠道带来的客户，只以ExternalContactDetail.follower.state为准，只要有过被客户添加记录，都会返回（哪怕任何一方拉黑了对方）
     *
     * 相互添加对方时，都会更新ContactExtra.ids，删除时则不一定更新ContactExtra.ids：
     * 若成员删除了客户，则将客户从自己的ContactExtra.ids里面移除，但ExternalContactDetail仍存在且仍follow了自己
     * 若客户删除了自己，则自己的ContactExtra.ids里面内容不变，但将自己从ExternalContactDetail的follow里面移除
     *
     *
     *
     * @param params 里面包含了channelId 渠道id
     * @param userId 某个用户的渠道
     * @param corpId 用户所在企业
     * */
    fun getExternalListByPage(params: ExternalListParams, userId: String?, corpId: String?)
    : DataBox<List<ContactBean>> = DataBox.ok(helper.getExternalListByPage(params))

    /**
     * 查询特定客服id的所有客户(Contact.ids为客户id集合)
     * @param _id 即Contact._id
     * */
    fun getExternalListOfContact(_id: String?):  DataBox<List<ContactBean>> {
        if( _id.isNullOrBlank()) {
            log.warn("getExternalContactList: invalid parameter: corpId or contactId is null")
            return DataBox.ko("invalid parameter: corpId or contactId is null")
        }

        val extra = service.findContactExtra(_id)
        val customers = extra?.ids?.mapNotNull {
            service.findExternalContact(it, extra.corpId)?.toBean(
                extra.userId,
                0 /*chatMsgController.countChatMsg(corpId, id, it)*/,
                service.findRelationChanges(extra.corpId, extra.userId, extra._id, it)
            )
        }
        return DataBox.ok(customers)
    }

    fun getRelationChanges(externalId: String?,userId: String?, corpId: String?): DataBox<List<RelationChange>> {
        if( externalId.isNullOrBlank() || userId.isNullOrBlank() || corpId.isNullOrBlank()) {
            log.warn("getRelationChanges: invalid parameter: corpId or externalId or userId is null")
            return DataBox.ko("invalid parameter: corpId or externalId or userId is null")
        }

        return DataBox.ok(service.findRelationChanges(corpId, userId, null, externalId))
    }

    //主动获取部门列表
    fun syncDepartment(corpId: String?, agentId: Int?): DataBox<Int> {
        if(corpId == null ) return DataBox.ko("invalid parameter: corpId is null")

        val res = DepartmentApi(corpId, agentId, null).list()
        var count = 0
        if(res.isOK() && res.department != null){
            res.department!!.forEach {
                service.upsertDepartment(corpId, it)
                count++
            }
        }
        return DataBox.ok(count)
    }

}