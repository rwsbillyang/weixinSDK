/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 14:54
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

package com.github.rwsbillyang.wxUser.fakeRpc

import com.github.rwsbillyang.wxUser.account.Account
import com.github.rwsbillyang.wxUser.account.AccountExpire

/**
 * 配置url时的键值
 * */
enum class PayNotifierType{
    ExpireAlarm, PaySuccess, BonusNotify
}


interface IPayWechatNotifier {
    /**
     * 续费通知
     * @param account 通知目标对象
     * @param agentId 目标对象在哪个agent应用，对于公众号则为null
     * @param title 通知内容
     * */
    fun onExpire(account: Account, appId:String?, agentId: Int?, accountExpire: AccountExpire?, title: String?)


    /**
     * 续费成功通知
     * @param account 通知目标对象
     * @param productName 产品名称，通常是版本level转换加上年限
     * @param totalMoney 金额信息，或 赠送说明
     * @param newExpire 新到期时间
     * @param title 标题，不提供使用默认。在赠送时可以单独提供：如邀请有礼30天赠送
     * */
    fun onPaySuccess(account: Account, appId:String?, agentId: Int?, productName: String, totalMoney: String, newExpire: Long, title: String?)

    fun onBonusSuccess(account: Account, appId:String?, agentId: Int?, productName: String, newExpire: Long, title: String?)

}