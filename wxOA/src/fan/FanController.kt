/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-10 15:46
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.wxOA.fan


import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxOA.account.WxOaAccountService
import com.github.rwsbillyang.wxUser.account.Profile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class FanController : KoinComponent {
    private val service: FanService by inject()
    private val wxOaAccountService: WxOaAccountService by inject()

    fun findFanList(fanListParams: FanListParams):  DataBox<List<Fan>>  {
        val list = service.findFanList(fanListParams)
        list.forEach{
            val f = it
            service.findGuest(it._id)?.let {
               f.name = it.name
               f.img = it.img
            }
        }
        return DataBox.ok(list)
    }

    fun findFan(openId: String?): DataBox<Fan> {
        return if(openId.isNullOrBlank()) DataBox.ko("no openId")
        else DataBox.ok(service.findFan(openId)?.also {
            val f = it
            service.findGuest(it._id)?.let {
                f.name = it.name
                f.img = it.img
            }
        })
    }

    /**
     * 是否关注公众号，1：关注，0：未关注
     * */
    fun isSubscribed(openId: String?): DataBox<Int> {
        return if(openId.isNullOrBlank()) DataBox.ko("no openId")
        else DataBox.ok(service.findFan(openId)?.sub?: 0)
    }


    fun getProfileByUId(uId: String): DataBox<Profile>{
       val a = wxOaAccountService.findWxOaAccount(uId.toObjectId())
        return if(a != null){
            DataBox.ok(service.getProfile(a.openId, a.unionId))
        }else
        {
            DataBox.ko("no account for uId=$uId")
        }
    }
    fun getProfileByOpenId(openId: String)=DataBox.ok(service.getProfile(openId, null))
}
