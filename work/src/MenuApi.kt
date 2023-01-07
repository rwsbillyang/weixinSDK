/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:38
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

package com.github.rwsbillyang.wxSDK.work

import com.github.rwsbillyang.wxSDK.Response
import com.github.rwsbillyang.wxSDK.bean.Menu
import com.github.rwsbillyang.wxSDK.bean.Menus




class MenuApi (corpId: String?, agentId: String?, suiteId: String?)
    : WorkBaseApi(corpId, agentId,suiteId)
{

    override val group = "menu"
    /**
     * 创建菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90231
     * */
    fun create(agentId: String, menus: List<Menu>): Response = doPost("create", Menus(menus), mapOf("agentid" to agentId))

    /**
     * 获取菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90232
     * */
    fun detail(agentId: String): List<Menu> = doGet("get",  mapOf("agentid" to agentId))

    /**
     * 删除菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90233
     * */
    fun delete(agentId: String) = doGetRaw("delete",  mapOf("agentid" to agentId))
}