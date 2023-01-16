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
import com.github.rwsbillyang.wxSDK.work.KfAccountListResponse
import com.github.rwsbillyang.wxSDK.work.WxKefuApi
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.slf4j.LoggerFactory

class WxkfController:KoinComponent {
    private val log = LoggerFactory.getLogger("WxkfController")
    private val service:WxkfService by inject()

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
}