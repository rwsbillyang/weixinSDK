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
package com.github.rwsbillyang.wxWork.account


import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxUser.account.AccountService
import com.github.rwsbillyang.wxUser.account.EditionLevel
import com.github.rwsbillyang.wxUser.account.EditionLevel.level2Name
import com.github.rwsbillyang.wxUser.account.Recommend
import com.github.rwsbillyang.wxWork.msg.PayMsgNotifier
import com.github.rwsbillyang.wxWork.wxWorkModule
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection


class RecommendHelper: KoinComponent {
    companion object{
        const val isBonus = true //开关控制是否推荐奖励
        const val bonusDays = 30
        const val bonusLevel = EditionLevel.VIP
    }


    private val dbSource: MongoDataSource by inject(qualifier = named(wxWorkModule.dbName!!))

    private val recommendCol: CoroutineCollection<Recommend> by lazy {
        dbSource.mongoDb.getCollection("wxWorkRecommend")
    }

    private val accountService: AccountService by inject()
    private val wxWorkAccountService: WxWorkAccountService by inject()

    private val wechatNotifier: PayMsgNotifier by inject()

    /**
     * @param account.Id
     * @param rcm 推荐人Account._id
     * */
    fun bonus(account: WxWorkAccount, rcm: String?, agentId:Int?){
        if(!isBonus || rcm == null){
            return
        }
        val rcmId = rcm.toObjectId()

        wxWorkAccountService.insertRecommend(Recommend(account._id, rcmId))

        val newExpire = accountService.calculateNewExpireInfo(account?.expire, bonusLevel, 0,0, bonusDays)
        wxWorkAccountService.updateExpireInfo(account._id, newExpire)

        val recommender = wxWorkAccountService.findWxWorkAccount(rcmId)
        if(recommender != null){
            //wechat notify 企业微信发送方式接收者有所不同
            wechatNotifier.onBonusSuccess(account, account.corpId, agentId,"点击领取：" + level2Name(bonusLevel), newExpire.expire, "我接受好友 ${recommender.userId} 邀请，赠送${RecommendHelper.bonusDays}天VIP到账")


            //奖励推荐人
            val newExpire2 = accountService.calculateNewExpireInfo(recommender.expire, bonusLevel, 0,0, bonusDays)
            wxWorkAccountService.updateExpireInfo(rcmId, newExpire2)
            //wechat notify 企业微信发送方式接收者有所不同
            wechatNotifier.onBonusSuccess(recommender, recommender.corpId, agentId,"点击领取：" + level2Name(bonusLevel),newExpire2.expire, "我邀请的好友 ${account.userId} 开通使用， 赠送${RecommendHelper.bonusDays}天VIP到账")
        }
    }
}