package com.github.rwsbillyang.wxSDK.work

import com.github.rwsbillyang.wxSDK.common.Response
import com.github.rwsbillyang.wxSDK.common.bean.Menu
import com.github.rwsbillyang.wxSDK.common.bean.Menus




object MenuApi: WorkBaseApi(){
    override val group = "menu"
    /**
     * 创建菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90231
     * */
    fun create(agentId: String, menus: List<Menu>): Response = doPost2("create", Menus(menus), mapOf("agentid" to agentId))

    /**
     * 获取菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90232
     * */
    fun detail(agentId: String): List<Menu> = doGet2("get",  mapOf("agentid" to agentId))

    /**
     * 删除菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90233
     * */
    fun delete(agentId: String) = doGet("delete",  mapOf("agentid" to agentId))
}