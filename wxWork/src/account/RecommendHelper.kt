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


import com.github.rwsbillyang.ktorKit.apiJson.to64String
import com.github.rwsbillyang.ktorKit.apiJson.toObjectId
import com.github.rwsbillyang.ktorKit.DataSource
import com.github.rwsbillyang.wxUser.account.Account
import com.github.rwsbillyang.wxUser.account.AccountServiceBase
import com.github.rwsbillyang.wxUser.account.Recommend
import com.github.rwsbillyang.wxUser.fakeRpc.*
import com.github.rwsbillyang.wxWork.fakeRpc.FanRpcWork
import com.github.rwsbillyang.wxWork.msg.PayMsgNotifier


import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.UseContextualSerialization
import org.bson.types.ObjectId
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.coroutine.CoroutineCollection




class RecommendHelper: KoinComponent{
    companion object{
        const val isBonus = true //开关控制是否推荐奖励
        const val bonusDays = 30
        const val bonusLevel = EditionLevel.VIP
    }


    private val dbSource: DataSource by inject(qualifier = named(AccountServiceBase.AccountDbName))

    private val recommendCol: CoroutineCollection<Recommend> by lazy {
        dbSource.mongoDb.getCollection("RecommendWork")
    }

    private val accountService: AccountServiceWxWork by inject()

    private val wechatNotifier: PayMsgNotifier by inject()
    private val fanClient: FanRpcWork by inject()

    /**
     * @param account 被推荐人Account
     * @param rcm 推荐人Account._id
     * */
    fun bonus(account: Account, rcm: String?, agentId:Int?){
        if(!isBonus || rcm == null){
            return
        }
        val rcmId = rcm.toObjectId()
        //val now = System.currentTimeMillis()



        GlobalScope.launch{

            //runBlocking {}
            recommendCol.save(Recommend(account._id, rcmId))

            var nick: String? = ""
            val newExpire = accountService.updateAccountExpiration(account._id.to64String(), agentId, bonusLevel, 0,0, bonusDays)

            if( account.corpId != null) nick = fanClient.getFanInfo(account.toVID()).nick?:""
            //wechat notify 企业微信发送方式接收者有所不同
            wechatNotifier.onBonusSuccess(account, account.corpId, agentId,"点击领取：" + level2Name(bonusLevel), newExpire, "我接受好友邀请，赠送30天VIP到账")


            //奖励推荐人
            accountService.findOne(rcmId)?.let {
                val newExpire2 = accountService.updateAccountExpiration(rcm, agentId, bonusLevel, 0,0, bonusDays)
                //wechat notify 企业微信发送方式接收者有所不同
                wechatNotifier.onBonusSuccess(it, it.corpId, agentId,"点击领取：" + level2Name(bonusLevel),newExpire2, "我邀请的好友 $nick 开通使用， 赠送30天VIP到账")
            }
        }
    }
}