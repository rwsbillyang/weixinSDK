/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-10-07 20:06
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

@file:UseContextualSerialization(ObjectId::class)

package com.github.rwsbillyang.wxOA.account


import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxOA.fan.FanService
import com.github.rwsbillyang.wxOA.msg.TemplatePayMsgNotifier
import com.github.rwsbillyang.wxUser.account.*
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class RecommendHelper : KoinComponent {
    companion object {
        const val isBonus = true //开关控制是否推荐奖励
        const val bonusDays = 30
        const val bonusLevel = EditionLevel.VIP
    }


    private val wechatNotifier: TemplatePayMsgNotifier by inject()
    private val accountService: AccountService by inject()
    private val wxOaAccountService: WxOaAccountService by inject()
    private val fanService: FanService by inject()



    /**
     * @param account.Id
     * @param rcm 推荐人Account._id
     * */
    fun bonus(account: WxOaAccount, rcm: String?) {
        if (!isBonus || rcm == null) {
            return
        }
        val rcmId = rcm.toObjectId()
        //val now = System.currentTimeMillis()


        wxOaAccountService.insertRecommend(Recommend(account._id, rcmId))

        val newExpire = accountService.calculateNewExpireInfo(account.expire, bonusLevel, 0, 0, bonusDays)
        wxOaAccountService.updateExpireInfo(account._id, newExpire)

        val recommender = wxOaAccountService.findWxOaAccount(rcmId)
        if (recommender != null) {
            val p1 = fanService.getProfile(recommender.openId, recommender.unionId)
            //wechat notify 企业微信发送方式接收者有所不同
            wechatNotifier.onBonusSuccess(
                account.openId,
                account.appId,
                "点击领取：" + EditionLevel.level2Name(
                    bonusLevel
                ),
                newExpire.expire,
                "我接受好友 ${p1?.nick?:""} 邀请，赠送${RecommendHelper.bonusDays}天VIP到账"
            )


            //奖励推荐人
            val newExpire2 = accountService.calculateNewExpireInfo(recommender.expire, bonusLevel, 0, 0, bonusDays)
            wxOaAccountService.updateExpireInfo(rcmId, newExpire2)
            //wechat notify 企业微信发送方式接收者有所不同
            val p2 = fanService.getProfile(recommender.openId, recommender.unionId)
            wechatNotifier.onBonusSuccess(
                recommender.openId,
                recommender.appId,
                "点击领取：" + EditionLevel.level2Name(
                    bonusLevel
                ),
                newExpire2.expire,
                "我邀请的好友 ${p2?.nick?:""} 开通使用， 赠送${RecommendHelper.bonusDays}天VIP到账"
            )

        }


    }
}