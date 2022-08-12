/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-21 22:21
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

package com.github.rwsbillyang.wxOA.fakeRpc



import com.github.rwsbillyang.wxOA.account.AccountServiceOA
import com.github.rwsbillyang.wxUser.account.VID


import com.github.rwsbillyang.wxUser.fakeRpc.FanInfo
import com.github.rwsbillyang.wxUser.fakeRpc.IFan

import com.github.rwsbillyang.wxOA.fan.FanService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class FanRpcOA: IFan, KoinComponent {
   // private val userClient: IUser by inject()
    private val accountService: AccountServiceOA by inject()
    private val service: FanService by inject()


    override fun getFanInfo(openId: String, unionId: String?): FanInfo {
        val f = service.findFan(openId)
        if(f != null) return FanInfo(f._id, f.uId, f.img, f.name, f.sex?:0,
            "${f.cty} ${f.pro} ${f.city}".trim())

        val g = service.findGuest(openId)
        if(g != null) return FanInfo(g._id, g.uId,  g.img, g.name, g.sex?:0,
            "${g.cty} ${g.pro} ${g.city}".trim())

        return FanInfo(openId, unionId)
    }

    override fun getFanInfo(vId: VID): FanInfo {
        return FanInfo(vId.openId?:"noOpenId")
    }

    override fun getSubscribedList(page: Int, pageSize: Int): List<String> {
       return service.getSubscribedList(page, pageSize)
    }

    fun getFanInfoByUId(uId: String): FanInfo? {
        return accountService.findById(uId)?.let {
            if(it.openId1 != null){
                getFanInfo(it.openId1!!, it.unionId)
            }else null
        }
    }
}