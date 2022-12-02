/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 15:09
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

package com.github.rwsbillyang.wxOA.msg


import com.github.rwsbillyang.ktorKit.util.DatetimeUtil
import com.github.rwsbillyang.wxSDK.officialAccount.outMsg.ColoredValue
import com.github.rwsbillyang.wxUser.account.EditionLevel.level2Name
import com.github.rwsbillyang.wxUser.account.ExpireInfo
import com.github.rwsbillyang.wxUser.account.PayNotifierType
import org.slf4j.LoggerFactory


class TemplatePayMsgNotifier : TemplateMsgBase() {
    private val log = LoggerFactory.getLogger("WechatNotifier")


    /**
     * 续费通知
     * */
    fun onExpire(
        openId: String?,
        appId: String?,
        expireInfo: ExpireInfo,
        title: String?
    ) {

        if (openId == null || expireInfo.expire == null) {
            log.warn("onExpire: openId or expire is null, ignore ")
            return
        }

        val data = mutableMapOf<String, ColoredValue>()
        data["first"] = ColoredValue(title ?: "亲，您的会员即将到期")
        data["keynote1"] = ColoredValue(DatetimeUtil.format(expireInfo.expire!!, "yyyy年MM月dd日"), color)
        data["keynote2"] = ColoredValue(level2Name(expireInfo.level) + "会员到期后，影响使用", color)
        data["remark"] = ColoredValue("点击续费!", color)

        sendTemplateMsg(openId, appId, PayNotifierType.ExpireAlarm.name, data)
    }

    /**
     * 续费成功通知
     * @param account 系统用户
     * @param productName 产品名称，通常是版本level转换加上年限
     * @param totalMoney 金额信息，或 赠送说明
     * @param newExpire 新到期时间
     * @param title 标题，不提供使用默认。在赠送时可以单独提供：如邀请有礼30天赠送
     * */
    fun onPaySuccess(
        openId: String?,
        appId: String?,
        productName: String,
        totalMoney: String,
        newExpire: Long,
        title: String?
    ) {
        if (openId == null) {
            log.warn("onPaySuccess: onPaySuccess  is null, ignore ")
            return
        }
        val data = mutableMapOf<String, ColoredValue>()
        data["first"] = ColoredValue(title ?: "亲，您已成功续费，感谢支持！")
        data["keyword1"] = ColoredValue(productName, color)
        data["keyword2"] = ColoredValue(totalMoney, color)
        data["keyword3"] = ColoredValue(DatetimeUtil.format(newExpire), color)
        data["remark"] = ColoredValue("点击试试分享素材哦", color)

        sendTemplateMsg(openId, appId, PayNotifierType.PaySuccess.name, data)
    }


    fun onBonusSuccess(
        openId: String?,
        appId: String?,
        productName: String,
        newExpire: Long,
        title: String?
    ) {

        if (openId == null) {
            log.warn("onBonusSuccess: onPaySuccess  is null, ignore ")
            return
        }

        val data = mutableMapOf<String, ColoredValue>()
        data["first"] = ColoredValue(title ?: "赠送到账")
        data["keyword1"] = ColoredValue(productName, color)
        data["keyword3"] = ColoredValue(DatetimeUtil.format(newExpire), color)
        data["remark"] = ColoredValue("点击领取", color)


        sendTemplateMsg(openId, appId, PayNotifierType.BonusNotify.name, data)
    }
}
