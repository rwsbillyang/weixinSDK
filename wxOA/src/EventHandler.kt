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

import com.github.rwsbillyang.wxOA.stats.StatsEvent
import com.github.rwsbillyang.wxOA.stats.StatsService
import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.*
import org.koin.core.inject
import javax.xml.stream.XMLEventReader

class EventHandler: IOAEventHandler, MsgEventCommonHandler() {

    private val statsService: StatsService by inject()

    override fun onDefault(appId:String, e: WxBaseEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event)
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onDispatch(appId:String, agentId:Int?, reader: XMLEventReader, base: BaseInfo): ReBaseMSg? {
        return null// 返回null将由onDefault继续处理
    }

    override fun onOALocationEvent(appId:String, e: OALocationEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = "${e.latitude}, ${e.longitude}, ${e.precision}" )
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMassSendFinishEvent(appId:String, e: OAMassSendFinishEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event)
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuClickEvent(appId:String, e: OAMenuClickEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, eKey = e.eventKey )
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuLocationEvent(appId:String, e: OAMenuLocationEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, eKey = e.eventKey,  extra = "${e.locationX}, ${e.locationY}, ${e.scale}, ${e.label}" )
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuMiniEvent(appId:String, e: OAMenuMiniEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = e.menuId )
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuOAAlbumEvent(appId:String, e: OAMenuOAAlbumEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = e.sendPicsInfo.toString() )
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuPhotoEvent(appId:String, e: OAMenuPhotoEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = e.sendPicsInfo.toString() )
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuPhotoOrAlbumEvent(appId:String, e: OAMenuPhotoOrAlbumEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = e.sendPicsInfo.toString() )
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuScanCodePushEvent(appId:String, e: OAMenuScanCodePushEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = "${e.scanResult},${e.scanType}")
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuScanCodeWaitEvent(appId:String, e: OAMenuScanCodeWaitEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = "${e.scanResult},${e.scanType}")
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    override fun onOAMenuViewEvent(appId:String, e: OAMenuViewEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = e.menuId)
        statsService.insertEvent(event)

        //进入菜单时更新，后期可以去掉, 避免频繁更新
        return tryReMsg(appId, e, event.from != null && fanService.findFan(event.from) == null)
    }

    override fun onOATemplateSendJobFinish(appId:String, e: OATemplateSendJobFinish): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, extra = "${e.status}")
        statsService.insertEvent(event)

        return tryReMsg(appId, e)
    }

    /**
     * 用户已关注时，扫码关注后的事件推送
     * TODO: 测试号关注时无此事件？
     * @property eventKey EventKey	事件KEY值，事件KEY值，是一个32位无符号整数，即创建二维码时的二维码scene_id
     * @property ticket Ticket	二维码的ticket，可用来换取二维码图片
     * */
    override fun onOAScanEvent(appId:String, e: OAScanEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, eKey = e.eventKey, extra = e.ticket)
        statsService.insertEvent(event)

        log.info("onOAScanEvent from ${event.from}")
        return tryReMsg(appId, e,false) //腾讯收回权限，干脆不获取
    }

    /**
     * 用户未关注时，扫码关注后的事件推送
     * TODO: 测试号关注时无此事件？
     * @property eventKey EventKey	事件KEY值，qrscene_为前缀，后面为二维码的参数值
     * @property ticket Ticket	二维码的ticket，可用来换取二维码图片
     * */
    override fun onOAScanSubscribeEvent(appId:String, e: OAScanSubscribeEvent): ReBaseMSg? {
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event, eKey = e.eventKey, extra = e.ticket)
        statsService.insertEvent(event)

        log.info("onOAScanSubscribeEvent from ${event.from}")
        return tryReMsg(appId, e,false) //腾讯收回权限，干脆不获取
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
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event)
        statsService.insertEvent(event)
        log.info("onOASubscribeEvent from ${event.from}")
        return tryReMsg(appId, e,false) //腾讯收回权限，干脆不获取
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
        val event = StatsEvent(appId, e.base.toUserName, e.base.fromUserName, e.base.createTime, e.event)
        statsService.insertEvent(event)
        e.base.fromUserName?.let { fanService.subscribeOrUnsubscribe(it, 0) }
        return tryReMsg(appId, e, false)
    }
}
