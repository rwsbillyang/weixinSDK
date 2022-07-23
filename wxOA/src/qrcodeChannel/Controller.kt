/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-11-23 13:50
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

package com.github.rwsbillyang.wxOA.qrcodeChannel

import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.wxSDK.officialAccount.QrCodeApi

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException


class QrCodeChannelController: KoinComponent {
    private val log = LoggerFactory.getLogger("ChannelController")
    private val QrCodeChannelPath = "./static/qrCodeChannel" //nginx配置： upload 开始的资源映射到 static下面

    private val service: QrCodeChannelService by inject()

    fun getChannelList(listParams: ChannelListParams): DataBox<List<QrCodeChannel>> {

        val filter =listParams.toFilter()

        val list = service.findList(filter, listParams.pagination, listParams.lastId)

        return DataBox.ok(list)
    }

    fun saveChannel(info: ChannelBean): DataBox<QrCodeChannel?> {
        val doc = if(info._id == null)
            service.addChannel(info)
        else
            service.updateChannel(info, info.qrCode == null)//一旦生成二维码将不再更新type
        return DataBox.ok(doc)
    }

    fun delChannel(id: String?, appId: String?): DataBox<Int> {
        if(id == null || appId == null) return DataBox.ko("invalid parameter: channel id or appId")
        service.delChannel(id, appId)
        return DataBox.ok(1)

    }
    /**
     * 获取某成员的某个渠道推广码
     * */
    fun generateChannelQrCode(channelId: String?, appId: String?): DataBox<QrCodeInfo> {
        if(channelId == null || appId == null) return DataBox.ko("getChannelQrCode invalid parameter: channel id or appId")
        val channel = service.findOne(channelId) ?: return DataBox.ko("not found channel")
        if(channel.qrCode != null) return DataBox.ok(QrCodeInfo(channel.qrCode, channel.imgUrl))

        val api = QrCodeApi(appId)
        return try {
            val res = when(channel.type){
                0 -> api.createTmp(channel.code)
                1 -> api.create(channel.code)
                else -> {
                    val msg = "not support channel.type=${channel.type}"
                    log.warn(msg)
                    return DataBox.ko(msg)
                }
            }

            return if(res.isOK() && res.ticket != null && res.url != null) {
                val filename = channel.code + "-"+System.currentTimeMillis() + ".jpg"
                val filePath = "$QrCodeChannelPath/${channel.appId}"
                val downUrl = api.qrCodeUrl(res.ticket!!)
                log.info("download from $downUrl")
                val ok = api.download(downUrl, filePath,filename)
                if(ok){
                    val path = "$filePath/$filename".removePrefix(".")
                    service.updateQrcode(channel._id, res.url!!, path)
                    DataBox.ok(QrCodeInfo(res.url, path))
                }else{
                    log.warn("fail to download for filePath=$filePath")
                    service.updateQrcode(channel._id, res.url!!, null)
                    DataBox.ko("fail to download from $downUrl")
                }
            }else{
                val err = res.errCode.toString() + res.errMsg
                log.warn(err)
                DataBox.ko(res.errCode.toString() + res.errMsg)
            }

        }catch (e: IllegalArgumentException){
            DataBox.ko("generateChannelQrCode fail: "+ e.message)
        }
    }


}