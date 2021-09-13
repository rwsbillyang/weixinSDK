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

import com.github.rwsbillyang.wxSDK.IBase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * 企业应用API
 * https://work.weixin.qq.com/api/doc/90000/90135/90226
 *
 * */
class AgentApi (corpId: String?, agentId: Int?, suiteId: String?)
    : WorkBaseApi(corpId, agentId,suiteId)
{
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
    fun detail(id: Int):ResponseAgentDetail = doGet("get", mapOf("agentid" to id.toString()))

    /**
     * 设置应用
     * https://work.weixin.qq.com/api/doc/90000/90135/90228
     * */
    fun setAgent(body: Map<String, Any?>) = doPost3("set",body)
}

/**
errcode	出错返回码，为0表示成功，非0表示调用失败
errmsg	返回码提示语
agentid	企业应用id
name	企业应用名称
square_logo_url	企业应用方形头像
description	企业应用详情
allow_userinfos	企业应用可见范围（人员），其中包括userid
allow_partys	企业应用可见范围（部门）
allow_tags	企业应用可见范围（标签）
close	企业应用是否被停用
redirect_domain	企业应用可信域名
report_location_flag	企业应用是否打开地理位置上报 0：不上报；1：进入会话上报；
isreportenter	是否上报用户进入应用事件。0：不接收；1：接收
home_url	应用主页url
 * */
@Serializable
class ResponseAgentDetail(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("agentid")
        val agentId: Int? = null,
        val name: String? = null,
        @SerialName("square_logo_url")
        val logo: String? = null,
        val description: String? = null,
        @SerialName("allow_userinfos")
        val allowUsers: AllowUsers? = null,
        @SerialName("allow_partys")
        val allowDepartment: AllowDepartment? = null,
        @SerialName("allow_tags")
        val allowTags: AllowTags? = null,
        val close: Int? = null,
        @SerialName("redirect_domain")
        val redirectDomain: String? = null,
        @SerialName("report_location_flag")
        val reportLocationFlag: Int? = null,
        @SerialName("isreportenter")
        val isReportEnter: Int? = null,
        @SerialName("home_url")
        val homeUrl: String? = null
): IBase

@Serializable
class AllowUsers(@SerialName("user") val list: List<UserId>? = null)

@Serializable
class UserId(@SerialName("userid") val id: String)

@Serializable
class AllowDepartment(@SerialName("partyid") val list: List<Int>? = null)

@Serializable
class AllowTags(@SerialName("tagid") val list: List<Int>? = null)
