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


/**
 * 企业应用API
 * https://work.weixin.qq.com/api/doc/90000/90135/90226
 * */
object AgentApi : WorkBaseApi(){
    override val group = "agent"
    
    /**
     * 获取access_token对应的应用列表
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90227
     * */
    fun list() = doGet3("list", null)

    /**
     * 获取指定的应用详情
     * */
    fun detail(id: String) = doGet3("get", mapOf("agentid" to id))

    /**
     * 设置应用
     * https://work.weixin.qq.com/api/doc/90000/90135/90228
     * */
    fun setAgent(body: Map<String, Any?>) = doPost3("set",body)
}

