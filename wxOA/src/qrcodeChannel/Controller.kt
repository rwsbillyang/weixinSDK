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


import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.client.doDownload
import com.github.rwsbillyang.ktorKit.util.NginxStaticRootUtil
import com.github.rwsbillyang.wxSDK.officialAccount.QrCodeApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.io.File


class QrCodeChannelController: KoinComponent {
    private val log = LoggerFactory.getLogger("ChannelController")

    private val service: QrCodeChannelService by inject()

    fun getChannelList(listParams: ChannelListParams) = DataBox.ok(service.findList(listParams))
    fun saveChannel(info: ChannelBean): DataBox<QrCodeChannel?> {
        val doc = if(info._id == null)
            service.addChannel(info)
        else
            service.updateChannel(info, info.qrCode == null)//一旦生成二维码将不再更新type
        return DataBox.ok(doc)
    }

    fun delChannel(id: String?, appId: String?): DataBox<Int> {
        if(id == null) return DataBox.ko("invalid parameter: channel id or appId")

        //删除本地二维码文件
        val channel = service.findOne(id)
        if(channel?.imgUrl != null){
            File("${NginxStaticRootUtil.nginxRoot()}/${channel.imgUrl}").delete()
        }

        service.delChannel(id, appId)
        return DataBox.ok(1)

    }
    /**
     * 获取某成员的某个渠道推广码
     * */
    fun generateChannelQrCode(channelId: String?, appId: String?): DataBox<QrCodeInfo> {
        if(channelId == null || appId == null) return DataBox.ko("getChannelQrCode invalid parameter: channel id or appId")
        val channel = service.findOne(channelId) ?: return DataBox.ko("not found channel")
        if(channel.qrCode != null){
            if(channel.type == 1){
                return if(channel.imgUrl == null){ //重新下载
                    log.info("TODO: re-download")
                    DataBox.ok(QrCodeInfo(channel.qrCode, null))
                }else
                    DataBox.ok(QrCodeInfo(channel.qrCode, channel.imgUrl))
            }else if(channel.type == 0){//临时重新生成
                log.info("TODO: re-generate ")
                return DataBox.ok(QrCodeInfo(channel.qrCode, channel.imgUrl))
            }
        }

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
                service.updateQrcode(channel._id, res.url!!, null)

                val filename = channel.code + "-"+System.currentTimeMillis() + ".jpg"

                //生成的公众号渠道二维码路径，nginx配置需要对应上
                val myPath = "qrCodeChannel/${channel.appId}"
                val filePath = NginxStaticRootUtil.getTotalPath(myPath)

                val downUrl = api.qrCodeUrl(res.ticket!!)
                log.info("download from $downUrl")
                val ok = doDownload(downUrl, filePath,filename)
                if(ok != null){
                    val url = "${NginxStaticRootUtil.getUrlPrefix(myPath)}/$filename"
                    service.updateQrcode(channel._id, res.url!!, url)
                    DataBox.ok(QrCodeInfo(res.url, url))
                }else{
                    log.warn("fail to download for filePath=$filePath")
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