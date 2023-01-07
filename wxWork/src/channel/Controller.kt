/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-31 21:58
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

package com.github.rwsbillyang.wxWork.channel


import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.wxSDK.work.ContactWayConfig
import com.github.rwsbillyang.wxSDK.work.ExternalContactsApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class ChannelController: KoinComponent {
    //private val log = LoggerFactory.getLogger("ChannelController")
    private val service: ChannelService by inject()


    fun getChannelList(listParams: ChannelListParams, isAdmin: Boolean): DataBox<List<Channel>> {
         if(!isAdmin && (listParams.corpId == null || listParams.userId == null))
             return DataBox.ko( "corpId or userId is null" )

        return DataBox.ok(service.findList(listParams))
    }

    fun saveChannel(doc:Channel): DataBox<Channel>{

        val b = if(doc._id == null)
            service.addChannel(doc)
        else
            service.updateChannel(doc)
        return DataBox.ok(b)
    }

    fun delChannel(id: String?, suiteId: String?, agentId: String?): DataBox<Int> {
        if(id == null) return DataBox.ko("invalid parameter: channel id")
        val channel = service.findOne(id) ?: return DataBox.ko("not found channel")

        if(channel.configId == null){
            service.delChannel(id)
            return DataBox.ok(1)
        }

        return try {
            val res = ExternalContactsApi(channel.corpId, agentId, suiteId).delContactWay(channel.configId)
            return if(res.isOK()){
                service.delChannel(id)
                DataBox.ok(1)
            }else
                DataBox.ko(res.errCode.toString() + ":" + res.errMsg)
        }catch (e: IllegalArgumentException){
            DataBox.ko("ExternalContactsApi IllegalArgumentException "+ e.message)
        }
    }
    /**
     * 获取某成员的某个渠道推广码
     * */
    fun getChannelQrCode(channelId: String?, suiteId: String?, agentId: String?): DataBox<String> {
        if(channelId == null) return DataBox.ko("getChannelQrCode invalid parameter: channel id")
        val channel = service.findOne(channelId) ?: return DataBox.ko("not found channel")
        val url = channel.qrCode
        if(url != null) return DataBox.ok(url)

        //    val type: Int, //联系方式类型,1-单人, 2-多人  不给默认值是为了适应默认值不序列化的配置
        //    val scene: Int, //场景，1-在小程序中联系，2-通过二维码联系
        val config = ContactWayConfig(1, 2, remark = channel.name, state = channel._id!!.to64String(), user = listOf(channel.userId))

        return try {
            val res = ExternalContactsApi(channel.corpId, agentId, suiteId).addContactWay(config)
            if(res.isOK())
            {
                if(res.qrCode == null || res.configId == null)
                {
                    return DataBox.ko("no qrCode or configId")
                }

                service.updateQrcode(channel._id!!, res.qrCode!!, res.configId!!)
                return DataBox.ok(res.qrCode)
            }else
                return DataBox.ko(res.errCode.toString() + res.errMsg)
        }catch (e: IllegalArgumentException){
            DataBox.ko("ExternalContactsApi IllegalArgumentException "+ e.message)
        }
    }

    //先删除（如果有的话，再重新生成，然后返回新生成的二维码）
    fun regenerateQrCode(channelId: String?, suiteId: String?, agentId: String?): DataBox<String>{
        if(channelId == null) return DataBox.ko("regenerateQrCode invalid parameter: channel id")
        val channel = service.findOne(channelId) ?: return DataBox.ko("not found channel")
        val url = channel.qrCode

        try {
            val api = ExternalContactsApi(channel.corpId, agentId, suiteId)
            if(url != null && channel.configId != null) {
                api.delContactWay(channel.configId)
            }

            //    val type: Int, //联系方式类型,1-单人, 2-多人  不给默认值是为了适应默认值不序列化的配置
            //    val scene: Int, //场景，1-在小程序中联系，2-通过二维码联系
            val config = ContactWayConfig(1, 2, remark = channel.name, state = channel._id!!.to64String(), user = listOf(channel.userId))

            val res = api.addContactWay(config)
            if(res.isOK())
            {
                if(res.qrCode == null || res.configId == null)
                {
                    return DataBox.ko("no qrCode or configId")
                }

                service.updateQrcode(channel._id!!, res.qrCode!!, res.configId!!)
                return DataBox.ok(res.qrCode)
            }else
                return DataBox.ko(res.errCode.toString() + res.errMsg)
        }catch (e: IllegalArgumentException){
            return DataBox.ko("ExternalContactsApi IllegalArgumentException "+ e.message)
        }

    }

}
