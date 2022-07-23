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

package com.github.rwsbillyang.wxOA.account


import com.github.rwsbillyang.ktorKit.LifeCycle
import com.github.rwsbillyang.wxOA.msg.TemplatePayMsgNotifier

import com.github.rwsbillyang.wxUser.fakeRpc.level2Name
import io.ktor.application.*
import it.justwrote.kjob.InMem
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.kjob
import it.justwrote.kjob.kron.Kron
import it.justwrote.kjob.kron.KronModule

import org.koin.ktor.ext.inject


class ExpireNotifierOA(application: Application): LifeCycle(application)  {
    object ExpireAlarmJob : KronJob("ExpireAlarmJob", "0 0 2 * * ?") //utc时间2:00, bj时间10:00

    private val accountService: AccountServiceOA by application.inject()
    private val wechatNotifier: TemplatePayMsgNotifier by application.inject()

    private val job = kjob(InMem) {
        extension(KronModule) // enable the Kron extension
    }.start()

    init {
        job(Kron).kron(ExpireAlarmJob) {
            execute {
                //明天要过期
                accountService.findExpireInDays(1).forEach {
                    wechatNotifier.onExpire(it, it.appId, null, null,"亲，您的${level2Name(it.level)}会员明天过期")
                }
                //今天过期
                accountService.findExpireInDays(0).forEach {
                    wechatNotifier.onExpire(it, it.appId, null, null,"亲，您的${level2Name(it.level)}会员今天过期")
                }
            }
        }

        onStopping {
            job.shutdown()
        }

    }
}