/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 16:27
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

package com.github.rwsbillyang.wxWork.msg

import com.github.rwsbillyang.ktorKit.util.DatetimeUtil
import com.github.rwsbillyang.wxSDK.work.outMsg.WxWorkTextCardMsg
import com.github.rwsbillyang.wxUser.account.Account
import com.github.rwsbillyang.wxUser.account.AccountExpire
import com.github.rwsbillyang.wxUser.fakeRpc.IPayWechatNotifier
import com.github.rwsbillyang.wxUser.fakeRpc.PayNotifierType
import com.github.rwsbillyang.wxUser.fakeRpc.level2Name
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


class PayMsgNotifier: MsgNotifierBase(), IPayWechatNotifier {
    private val log = LoggerFactory.getLogger("PayMsgNotifier")
    override fun onBonusSuccess(
        account: Account,
        appId: String?,
        agentId: Int?,
        productName: String,
        newExpire: Long,
        title: String?
    ) {
        if(appId == null){
            log.warn("no corpId, ignore send msg")
            return
        }
        msgApi(account, appId, agentId)?.let {
            GlobalScope.launch {
                val description = setupDescription(
                    listOf("赠送${productName}",
                        "到期日："+ DatetimeUtil.format(newExpire, "yyyy年MM月dd日")),
                    "点击领取")

                val msg = WxWorkTextCardMsg(title ?: "续费成功", description, url(account,  appId, agentId, PayNotifierType.BonusNotify.name) , agentId!!, account.userId)
                it.send(msg)
            }
        }

    }

    override fun onExpire(
        account: Account,
        appId: String?,
        agentId: Int?,
        accountExpire: AccountExpire?,
        title: String?
    ) {
        if(appId == null){
            log.warn("no corpId, ignore send msg")
            return
        }
        msgApi(account, appId, agentId)?.let {
            GlobalScope.launch {
                val normal = level2Name(accountExpire?.level?:account.level) +"会员到期日："+ DatetimeUtil.format((accountExpire?.expire?:account.expire)!!, "yyyy年MM月dd日")
                val description = setupDescription(normal, "到期后将影响使用，请点击续费")
                val msg = WxWorkTextCardMsg(title ?: "会员到期提醒", description, url(account, appId, agentId, PayNotifierType.ExpireAlarm.name), agentId!!, account.userId)
                it.send(msg)
            }
        }
    }

    override fun onPaySuccess(
        account: Account,
        appId: String?,
        agentId: Int?,
        productName: String,
        totalMoney: String,
        newExpire: Long,
        title: String?
    ) {
        if(appId == null){
            log.warn("no corpId, ignore send msg")
            return
        }
        msgApi(account, appId,  agentId)?.let {
            GlobalScope.launch {
//                val description = setupDescription(
//                    listOf("您已成功支付${totalMoney}，感谢支持！",
//                    "${productName}到期日："+ DatetimeUtil.format(newExpire, "yyyy年MM月dd日")),
//                    "您可以试试分享素材哦")

                val description = grayDiv("${productName}到期日："+ DatetimeUtil.format(newExpire, "yyyy年MM月dd日")) +
                        normalDiv("您已成功支付${totalMoney}，感谢支持！") + grayDiv("您可以试试分享素材哦")

                val msg = WxWorkTextCardMsg(title ?: "续费成功", description, url(account, appId,  agentId, PayNotifierType.PaySuccess.name), agentId!!, account.userId)
                it.send(msg)
            }
        }

    }
}