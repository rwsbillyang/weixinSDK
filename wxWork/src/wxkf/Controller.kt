/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-05 11:59
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

package com.github.rwsbillyang.wxWork.wxkf

import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.util.DatetimeUtil
import com.github.rwsbillyang.wxSDK.work.EnterSessionContext
import com.github.rwsbillyang.wxSDK.work.KfAccountListResponse
import com.github.rwsbillyang.wxSDK.work.WxKefuApi
import com.github.rwsbillyang.wxWork.contacts.ContactService
import com.github.rwsbillyang.wxWork.contacts.ExternalContact
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.slf4j.LoggerFactory

class WxkfController:KoinComponent {
    private val log = LoggerFactory.getLogger("WxkfController")
    private val service:WxkfService by inject()
    private val contactService: ContactService by inject()

    //将腾讯的客服账号列表同步到自己的数据库中
    fun syncWxKfAccountList(corpId: String): DataBox<Int>{
        val api = WxKefuApi(corpId)
        var res: KfAccountListResponse
        var count = 0
        var page = 0
        do {
            res = api.accountList(page*100, 100)
            if(res.isOK()){
                res.account_list?.forEach {
                    service.save("wxkfAccount", WxkfAccount(it.open_kfid, it.name, it.avatar, it.manage_privilege, corpId))
                    syncServicesByOpenKfId(api, it.open_kfid)
                }
                page++
                count += (res.account_list?.size?:0)
            }
        }while(res.account_list != null && res.account_list!!.size >= 100)

        if(!res.isOK()){
            val msg = "syncWxKfAccountList: ${res.errCode}: ${res.errMsg}"
            log.warn(msg)
            return DataBox.ko(msg)
        }
        return DataBox.ok(count)
    }

    fun syncServicesByOpenKfId(api: WxKefuApi, open_kfid: String): Int{
        val res = api.servicerListByOpenKfId(open_kfid)
        if(res.isOK()){
            val userList = mutableListOf<String>()
            val depList = mutableListOf<Int>()
            res.servicer_list?.forEach {
                if(it.userid != null) userList.add(it.userid!!)
                if(it.department_id != null) depList.add(it.department_id!!)
            }
            service.save("wxkfServicer", WxkfServicer(open_kfid, api.corpId!!, userList, depList))
        }else{
            val msg = "syncServicesByOpenKfId: ${res.errCode}: ${res.errMsg}"
            log.warn(msg)
        }
        return res.servicer_list?.size?:0
    }
    fun getWxkfAccountList(coprId: String): List<WxkfAccount>{
        return service.findAll("wxkfAccount", WxkfAccount::corpId eq coprId)
    }

    fun saveScene(doc: WxkfScene): DataBox<WxkfScene> {
        if(doc._id == null){
            val api = WxKefuApi(doc.corpId)
            val res = api.addScene(doc.kfId, doc.scene)
            if(res.isOK()){
                if(res.url != null){
                    doc.url = res.url
                    doc._id = ObjectId()
                    service.save("wxkfScene", doc)
                    return DataBox.ok(doc)
                }else{
                    val msg = "should not come here: no url when call  api.addScene"
                    log.warn(msg)
                    return DataBox.ko(msg)
                }
            }else{
                val msg = "${res.errCode}: ${res.errMsg}, url=${res.url}"
                log.warn(msg)
                return DataBox.ko(msg)
            }
        }else{
            //update
            service.save("wxkfscene", doc)
            return DataBox.ok(doc)
        }
    }

    fun sceneList(coprId: String,kfId: String?): List<WxkfScene>{
        val f = if(kfId == null) WxkfScene::corpId eq coprId
        else and(WxkfScene::corpId eq coprId, WxkfScene::kfId eq kfId)
        return service.findAll("wxkfScene", f)
    }

    fun delScene(id: String) = service.deleteOne<WxkfScene>("wxkfScene", id)

    fun getChatSessionInf(params: WxMsgPageParams, appId: String?): DataBox<ChatSessionInfo>{
        val list = service.findPage<WxkfMsg>(service.wxkfMsgCol, params)
        val corpId = appId?:params.corpId?:list.firstOrNull()?.corpId
        if(corpId.isNullOrEmpty()){
            return DataBox.ko("no corpId in auth header or query params or wxMsgList")
        }
        val kfIds = mutableSetOf<String>()
        val servicers = mutableSetOf<String>()

        //TODO: image等消息下载media_id对应的资源
        list.forEach {
            if(it.origin == 4){
                if(it.jsonStr != null){
                    //{"event_type":"enter_session","scene":"oamenu3","open_kfid":"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w","external_userid":"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog","welcome_code":"8EW1TOYcpSP2tGD2UEWpXSM3gZS3qDHs7X2fwtk_oTc","scene_param":"%E5%9C%BA%E6%99%AF3"}
                    val json = Json.decodeFromString(JsonObject.serializer(), it.jsonStr)
                    json["open_kfid"]?.jsonPrimitive?.content?.let { kfIds.add(it) }
                    json["external_userid"]?.jsonPrimitive?.content?.let { servicers.add(it) }
                }
            }else{
                if(it.open_kfid != null)
                    kfIds.add(it.open_kfid)
                if(it.servicer_userid != null){
                    servicers.add(it.servicer_userid)
                }
            }
        }


        val externalId = params.external_userid
        val externalContact = externalId?.let {
            contactService.findExternalContact(it, corpId)
        }

        val external = if(externalContact != null){
            val enter = externalContact.enterSessions?.lastOrNull()
            ChatPeer(externalContact.externalId, externalContact.avatar, externalContact.name, null, null, "来自：" + enter?.scene_param)
        }else{
            if(externalId != null ){ //尝试再次获取客户信息, 在不接收消息事件时，新咨询者总会为空，此处试图获取其信息
                val res = WxKefuApi(corpId).getCustomerDetail(listOf(externalId))
                if (res.isOK()) {
                    res.customer_list?.forEach {
                        val enters = it.enter_session_context?.let {
                            listOf(EnterSessionContext(it.scene, it.scene_param))
                        }?: listOf()
                        contactService.upsertExternalContactByWxkf(corpId, it.external_userid, it.nickname,
                            it.avatar, it.gender, it.unionid, enters)//可能只是更新基本信息和插入enter_session信息

                    }
                    val detail = res.customer_list?.firstOrNull()
                    ChatPeer(externalId, detail?.avatar, detail?.nickname, null, null, "来自：" + detail?.enter_session_context?.scene_param)
                } else {
                    log.warn("fail to getCustomerDetail: ${res.errCode}: ${res.errMsg}")
                    null
                }
            }else{
               null
            }
        }


        val map = mutableMapOf<String, ChatPeer>()
        var me: ChatPeer? = null
        kfIds.forEach {
            val a = service.findOne<WxkfAccount>("wxkfAccount", it, false)
            if(a!=null)
            {
                me = ChatPeer(it, a.avatar, a.name)
                map[it] = me!!
            }
        }

        servicers.forEach {
            val a = contactService.findContact(it, corpId)
            if(a!=null)
            {
                map[it] = ChatPeer(it, a.avatar, a.name)
            }
        }

        return DataBox.ok(ChatSessionInfo(external, me, map.toMap(), list))
    }

    fun getExternalListInfo(corpId: String?):DataBox<List<ChatPeer>>{
        if(corpId.isNullOrEmpty()){
            return DataBox.ko("no corpId in auth header")
        }
        val list = contactService.findExternalListAll(and(ExternalContact::corpId eq corpId, ExternalContact::wxkf eq true))
            .map{
                val enter = it.enterSessions?.lastOrNull()
                ChatPeer(it.externalId, it.avatar, it.name, "from：" + enter?.scene_param, enter?.time?.let{DatetimeUtil.format(it,"yy-MM-dd HH:mm")})
            }

        return DataBox.ok(list)

    }
}