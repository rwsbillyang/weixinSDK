/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-12 17:35
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


import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxSDK.work.ContactsApi
import com.github.rwsbillyang.wxSDK.work.ExternalContactsApi

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException

class ContactHelper: KoinComponent {
    private val log = LoggerFactory.getLogger("ContactHelper")

    private val service: ContactService by inject()



    fun getExternalListByPage(param: ExternalListParams): List<ContactBean>{

        return service.findExternalListByPage(param).map{
            if(param.userId != null){
                val contactId = param.contactId?.toObjectId()?:service.findContact(param.userId!!, it.corpId)?._id
                it.toBean(param.userId,
                    0,
                    param.channelId?.let{ service.findRelationChanges(param.userId!!, contactId, it) }
                )
            }else{
                it.toBean(param.userId, 0, null)
            }
        }
    }


    fun syncContactDetail(userId: String, corpId: String, agentId: Int?, suiteId: String?): String?{
        return try {
            val api = ContactsApi(corpId, agentId, suiteId)
            val res = api.detail(userId)
            if(res.isOK()){
                service.upsertContact(res.toDoc(corpId))
                val res2 = api.convertToOpenId(userId)
                if(res2.isOK() && res2.openId != null){
                    service.updateContactOpenId(userId, corpId, res2.openId!!)
                }
                null
            }else{
                "${res.errCode}: ${res.errMsg}"
            }
        }catch (e: IllegalArgumentException){
            log.error("syncContactDetail: IllegalArgumentException to construct api:  e.message=${e.message}")
            e.message
        }
    }

    /**
     * @return 返回失败数量
     * */
    fun syncContactDetail(contactApi: ContactsApi, userId: String, corpId: String): Int{
        return try{
            val res = contactApi.detail(userId)
            return if(res.isOK()){
                service.upsertContact(res.toDoc(corpId))
                val res2 = contactApi.convertToOpenId(userId)
                if(res2.isOK() && res2.openId != null){
                    service.updateContactOpenId(userId, corpId, res2.openId!!)
                }
                0
            }else{
                1
            }
        }catch (e:Exception){
            log.warn("contactApi.detail fail, Exception: ${e.message}} ")
           1
        }

    }


    /**
     * 获取指定成员的客户列表。客户是指配置了客户联系功能的成员所添加的外部联系人。
     * 没有配置客户联系功能的成员，所添加的外部联系人将不会作为客户返回。
     * 企业需要使用“客户联系”secret或配置到“可调用应用”列表中的自建应用secret所获取的accesstoken来调用
     * TODO: 需要配置，参见 https://work.weixin.qq.com/api/doc/90000/90135/92109#%E5%BC%80%E5%A7%8B%E5%BC%80%E5%8F%91
     * 第三方应用需拥有“企业客户”权限。
     * 注意：第三方/自建应用只能获取到可见范围内的配置了客户联系功能的成员。
     *
     * @param refreshType 0 不刷新客户信息，即已存在不刷新；1 刷新客户信息
     * */
    fun syncExternalsOfUser(corpId: String, agentId: Int?, suiteId: String?, userId: String, refreshType: Int): String?{
       try {
           val api = ExternalContactsApi(corpId, agentId, suiteId)
           val res = api.list(userId)
           return if(res.isOK()){
               val list = res.external_userid
               service.upsertContactCustomerIds(corpId, userId, list)
               when(refreshType){
                   0 -> list?.forEach {
                       val external = service.findExternalContact(it,corpId)
                       if(external?.openId == null) { syncExternalDetail(corpId, it, api) }
                   }
                   1 -> list?.forEach { syncExternalDetail(corpId, it, api) }
               }
               null
           }else{
               val msg = "ExternalContactsApi.list for userID=$userId: ${res.errCode}: ${res.errMsg}"
               log.warn(msg)
               msg
           }
       }catch (e: IllegalArgumentException){
           log.warn("upsertCustomersOfUser create ExternalContactsApi IllegalArgumentException(${e.message}), corpId=$corpId, agentId=$agentId, suiteId=$suiteId")
           return e.message
       }
    }
    fun syncExternalsOfUser(api: ExternalContactsApi, corpId: String, userId: String, refreshType: Int): String?{
        val res = api.list(userId)//有可能无权限
        return if(res.isOK()){
            val list = res.external_userid
            service.upsertContactCustomerIds(corpId, userId, list)
            when(refreshType){
                0 -> list?.forEach {
                    val external = service.findExternalContact(it,corpId)
                    if(external?.openId == null) { syncExternalDetail(corpId, it, api) }
                }
                1 -> list?.forEach { syncExternalDetail(corpId, it, api) }
            }
            null
        }else{
            val msg = "ExternalContactsApi.list for userID=$userId: ${res.errCode}: ${res.errMsg}"
            log.warn(msg)
            msg
        }
    }


    /**
     * 获取外部联系人详细信息
     * */
    fun syncExternalDetail(corpId: String, agentId: Int?, suiteId: String?, externalUserId: String){
        try {
            val api = ExternalContactsApi(corpId, agentId, suiteId)
            val res = api.detail(externalUserId)
            if(res.isOK() && res.external_contact != null){
                res.toDoc(corpId)?.let { service.upsertExternalContact(it) }
                val res2 = api.convertToOpenId(externalUserId)
                if(res2.isOK() && res2.openId != null){
                    service.updateExternalContactOpenId(externalUserId, corpId, res2.openId!!)
                }else
                    log.warn("fail to externalContactsApi.convertToOpenId: externalUserId=$externalUserId")
            }else{
                log.warn("ExternalContactsApi.detail($externalUserId): ${res.errCode}: ${res.errMsg}")
            }
        }catch (e: IllegalArgumentException){
            log.error("syncExternalContactDetail IllegalArgumentException")
        }

    }

    fun syncExternalDetail(corpId: String, externalUserId: String, externalContactsApi: ExternalContactsApi){
        val res = externalContactsApi.detail(externalUserId)
        if(res.isOK() && res.external_contact != null){
            res.toDoc(corpId)?.let { service.upsertExternalContact(it) }
            val res2 = externalContactsApi.convertToOpenId(externalUserId)
            if(res2.isOK() && res2.openId != null){
                service.updateExternalContactOpenId(externalUserId, corpId, res2.openId!!)
            }else
                log.warn("fail to externalContactsApi.convertToOpenId: externalUserId=$externalUserId")
        }else{
            log.warn("ExternalContactsApi.detail($externalUserId): ${res.errCode}: ${res.errMsg}")
        }
    }
}