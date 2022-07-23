/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-11 16:51
 *
 */

package com.github.rwsbillyang.wxOA.msg

import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import org.koin.core.KoinComponent
import org.koin.core.inject


class MsgController : KoinComponent {
    private val service: MsgService by inject()

    fun findMyMsgList(listParam: MyMsgListParams): MyMsgListBox {
        val filter = listParam.toFilter()
        val total = service.countMyMsg(filter)
        val list = service.findMyMsgList(filter, listParam.pagination, listParam.lastId)
        return MyMsgListBox(total,list)
    }
    fun saveMyMsg(doc: MyMsg): DataBox<MyMsg> {
        service.saveMyMsg(doc)
        return DataBox.ok(doc)
    }

    fun delMyReMsg(id: String?): DataBox<Int> {
        if(id == null) return DataBox.ko("invalid parameter: no id")
        return DataBox.ok(service.delMyMsg(id).deletedCount.toInt())
    }

    //flag值为MsgBody中的RE(1)，Mass(2)，KF(4)等值,用于位选择
    fun findMyMsgList(appId: String?, flag: Long?): DataBox<List<MyMsg>> {
        if(appId == null || flag == null) return DataBox.ko("invalid parameter: no flag")
        val list = service.findMyMsgListByFlag(appId, flag)
        return DataBox.ok(list)
    }

}
