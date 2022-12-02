/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-10-07 20:13
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

package com.github.rwsbillyang.wxWork.account


import com.github.rwsbillyang.ktorKit.server.LifeCycle
import com.github.rwsbillyang.wxWork.msg.PayMsgNotifier
import io.ktor.server.application.*
import it.justwrote.kjob.InMem
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.kjob
import it.justwrote.kjob.kron.KronModule
import org.koin.ktor.ext.inject


class ExpireNotifierWork(application: Application): LifeCycle(application)  {
    object ExpireAlarmJob : KronJob("ExpireAlarmJob", "0 0 1 * * ?") //utc时间1:00, bj时间9:00

    private val accountService: WxWorkAccountService by application.inject()
    private val wechatNotifier: PayMsgNotifier by application.inject()

    private val job = kjob(InMem) {
        extension(KronModule) // enable the Kron extension
    }.start()

//    init {
//        job(Kron).kron(ExpireAlarmJob) {
//            execute {
//                //明天要过期
//                accountService.findExpireInDays(1).forEach {
//                    val arry = it._id.split("/")
//                    val agentId = if(arry.size == 2) arry[1].toInt() else null
//                    val account = arry.firstOrNull()?.let { accountService.findById(it) }
//                    if(account != null && account.state == Account.STATE_ENABLED){
//                        wechatNotifier.onExpire(account, account.corpId, agentId, it, "亲，您的${level2Name(it.level)}会员明天过期")
//                    }
//
//                }
//                //今天过期
//                accountService.findExpireInDays(0).forEach {
//                    val arry = it._id.split("/")
//                    val agentId = if(arry.size == 2) arry[1].toInt() else null
//                    val account = arry.firstOrNull()?.let { accountService.findById(it) }
//                    if(account != null && account.state == Account.STATE_ENABLED){
//                    wechatNotifier.onExpire(account, account.corpId, agentId, it,"亲，您的${level2Name(it.level)}会员今天过期")
//                    }
//                }
//            }
//        }
//
//        onStopping {
//            job.shutdown()
//        }
//
//    }
}