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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class FanController : KoinComponent {
    private val service: FanService by inject()

    fun findFanList(fanListParams: FanListParams):  DataBox<List<Fan>>  {
        //val filter = fanListParams.toFilter()
        //val total = service.countFan(filter)
        val list = service.findFanList(fanListParams.toFilter(), fanListParams.pagination, fanListParams.lastId)
        return DataBox.ok(list)
    }

    fun findFan(openId: String?): DataBox<Fan> {
        return if(openId.isNullOrBlank()) DataBox.ko("no openId")
        else DataBox.ok(service.findFan(openId))
    }

    /**
     * 是否关注公众号，1：关注，0：未关注
     * */
    fun isSubscribed(openId: String?): DataBox<Int> {
        return if(openId.isNullOrBlank()) DataBox.ko("no openId")
        else DataBox.ok(service.findFan(openId)?.sub?: 0)
    }


}
