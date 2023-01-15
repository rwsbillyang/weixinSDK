/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-12-10 17:04
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

package com.github.rwsbillyang.wxOA

import com.github.rwsbillyang.wxOA.fan.FanService
import com.github.rwsbillyang.wxOA.msg.MsgService
import com.github.rwsbillyang.wxOA.qrcodeChannel.QrCodeChannelService
import com.github.rwsbillyang.wxOA.stats.StatsEvent
import com.github.rwsbillyang.wxOA.stats.StatsService
import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

//每个公众号都拥有自己的context，对应着MsgbHub、msgHandler、EventHandler，通常情况下它们都共用一个handler
// 这些EventHandler和MsgHandler只处理属于自己的消息或事件
class EventHandler: DefaultOAEventHandler(), KoinComponent  {
    private val log = LoggerFactory.getLogger("OAEventHandler")

    private val statsService: StatsService by inject()
    private val reMsgChooser: ReMsgChooser by inject()
    private val fanService: FanService by inject()
    //用于关注后，查询关注渠道二维码设置的响应消息ID
    private val qrCodeChannelService: QrCodeChannelService by inject()
    private val msgService: MsgService by inject()//用于查询配置的消息

    private fun insertStats(appId:String, e: WxXmlEvent) = runBlocking{
        launch {
            val stats = StatsEvent(appId, e.toUserName, e.fromUserName, e.createTime, e.event, e.xml)
            statsService.insertEvent(stats)
        }
    }
    override fun onDefault(appId:String, e: WxXmlEvent): ReBaseMSg? {
        insertStats(appId, e)

        return reMsgChooser.tryReMsg(appId, e)
    }


    /**
     * 用户已关注时，扫码关注后的事件推送
     * TODO: 测试号关注时无此事件？
     * @property eventKey EventKey	事件KEY值，事件KEY值，是一个32位无符号整数，即创建二维码时的二维码scene_id
     * @property ticket Ticket	二维码的ticket，可用来换取二维码图片
     * */
    override fun onOAScanEvent(appId:String, e: OAScanEvent): ReBaseMSg? {
        log.info("onOAScanEvent from ${e.fromUserName}")

        insertStats(appId, e)

        val scene = e.eventKey
        return tryUseQrcodeMsgConfig(appId, e, scene)?:reMsgChooser.tryReMsg(appId, e,false)
    }

    /**
     * 用户未关注时，扫码关注后的事件推送
     * TODO: 测试号关注时无此事件？
     * @property eventKey EventKey	事件KEY值，qrscene_为前缀，后面为二维码的参数值
     * @property ticket Ticket	二维码的ticket，可用来换取二维码图片
     * */
    override fun onOAScanSubscribeEvent(appId:String, e: OAScanSubscribeEvent): ReBaseMSg? {
        log.info("onOAScanSubscribeEvent from ${e.fromUserName}")

        insertStats(appId, e)

        val scene = e.eventKey?.removePrefix("qrscene_")
        return tryUseQrcodeMsgConfig(appId, e, scene)?:reMsgChooser.tryReMsg(appId, e,false)
    }

    /**
     * 关注事件
     * TODO: 测试号关注时无此事件？
     * 用户在关注与取消关注公众号时，微信会把这个事件推送到开发者填写的URL。
     * 方便开发者给用户下发欢迎消息或者做帐号的解绑。为保护用户数据隐私，开发者收到用户取消关注事件时需要删除该用户的所有信息。微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。
     * 关于重试的消息排重，推荐使用FromUserName + CreateTime 排重。
     * 假如服务器无法保证在五秒内处理并回复，可以直接回复空串，微信服务器不会对此作任何处理，并且不会发起重试。
     * */
    override fun onOASubscribeEvent(appId:String, e: OASubscribeEvent): ReBaseMSg? {
        log.info("onOASubscribeEvent from ${e.fromUserName}")

        insertStats(appId, e)

        return reMsgChooser.tryReMsg(appId, e,true) //腾讯收回权限，插入一条记录
    }


    /**
     * 取消关注事件
     *
     * 用户在关注与取消关注公众号时，微信会把这个事件推送到开发者填写的URL。
     * 方便开发者给用户下发欢迎消息或者做帐号的解绑。为保护用户数据隐私，开发者收到用户取消关注事件时需要删除该用户的所有信息。微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。
     * 关于重试的消息排重，推荐使用FromUserName + CreateTime 排重。
     * 假如服务器无法保证在五秒内处理并回复，可以直接回复空串，微信服务器不会对此作任何处理，并且不会发起重试。
     * */
    override fun onOAUnsubscribeEvent(appId:String, e: OAUnsubscribeEvent): ReBaseMSg? {
        insertStats(appId, e)
        e.fromUserName?.let { fanService.subscribeOrUnsubscribe(it, 0) }
        return reMsgChooser.tryReMsg(appId, e, false)
    }

    //插入一条FAN记录, 并检查该qrcode是否配置有回复消息
    private fun tryUseQrcodeMsgConfig(appId: String, e: WxXmlEvent, scene: String?): ReBaseMSg?{
        //腾讯收回权限，但用于获取其它信息，并插入一条FAN记录
        if(e.fromUserName != null)
            reMsgChooser.upsertFan(appId, e.fromUserName!!)
        else {
            log.warn("from(openId) should not null")
        }

        if(scene != null){
            val msgId = qrCodeChannelService.findMsgId(appId, scene)
            if(msgId != null){
                val msg = msgService.findMyMsg(msgId)
                if(msg != null){
                    return reMsgChooser.myMsgToReMsg(msg, e.fromUserName, e.toUserName)
                }else{
                    log.warn("not found msg for msgId=${msgId.toHexString()}, to check event type config")
                }
            }else{
                log.info("no config re-msg for qrcode: ${scene}, to check event type config")
            }
        }else{
            log.warn("onOAScanSubscribeEvent: no scene? eventKey=${scene}, to check event type config")
        }
        return null
    }
}
