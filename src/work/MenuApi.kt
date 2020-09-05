package com.github.rwsbillyang.wxSDK.work

import com.github.rwsbillyang.wxSDK.common.Response
import com.github.rwsbillyang.wxSDK.common.bean.Menu
import com.github.rwsbillyang.wxSDK.common.bean.Menus




class MenuApi: WorkBaseApi(){
    override val group = "menu"
    companion object{
        const val CREATE = "create"
        const val DETAIL = "get"
        const val DELETE = "delete"
    }

    /**
     * 创建菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90231
     * */
    fun create(agentId: String, menus: List<Menu>): Response = doPost2(CREATE, Menus(menus), mapOf("agentid" to agentId))

    /**
     * 获取菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90232
     * */
    fun detail(agentId: String): List<Menu> = doGet2(DETAIL,  mapOf("agentid" to agentId))

    /**
     * 删除菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90233
     * */
    fun delete(agentId: String) = doGet(DELETE,  mapOf("agentid" to agentId))
}