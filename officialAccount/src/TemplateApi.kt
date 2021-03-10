/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:37
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

package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.IBase
import com.github.rwsbillyang.wxSDK.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 管理模板之用
 * */
class TemplateApi(appId: String) : OABaseApi(appId) {
    override val group: String = "template"

    /**
     * 获取帐号下所有模板信息,即自己公众号下添加的模板
     * */
    fun getAllTemplates(): ResponseTemplateList = doGet("get_all_private_template")

    /**
     * 根据模板库中模板编号，获取自己的模板消息id
     * @param code 模板库中模板的编号，有“TM**”和“OPENTMTM**”等形式
     * */
    fun getTemplateMsgId(code: String): ResponseTemplateMsgId =
        doPost("api_add_template", mapOf("template_id_short" to code))

    /**
     * @param templateId    公众帐号下模板消息ID, 参见Template
     * */
    fun delTemplate(templateId: String): Response = doPost("del_private_template", mapOf("template_id" to templateId))

    /**
     * 获取帐号所设置的行业信息
     * */
    fun getIndustry(): ResponseIndustry = doGet("get_industry")

    /**
     * 修改账号所属行业
     * @param industryId1    是	公众号模板消息所属行业编号
     * @param industryId2 是	公众号模板消息所属行业编号
     *
    IT科技	互联网/电子商务	1
    IT科技	IT软件与服务	2
    IT科技	IT硬件与设备	3
    IT科技	电子技术	4
    IT科技	通信与运营商	5
    IT科技	网络游戏	6
    金融业	银行	7
    金融业	基金理财信托	8
    金融业	保险	9
    餐饮	餐饮	10
    酒店旅游	酒店	11
    酒店旅游	旅游	12
    运输与仓储	快递	13
    运输与仓储	物流	14
    运输与仓储	仓储	15
    教育	培训	16
    教育	院校	17
    政府与公共事业	学术科研	18
    政府与公共事业	交警	19
    政府与公共事业	博物馆	20
    政府与公共事业	公共事业非盈利机构	21
    医药护理	医药医疗	22
    医药护理	护理美容	23
    医药护理	保健与卫生	24
    交通工具	汽车相关	25
    交通工具	摩托车相关	26
    交通工具	火车相关	27
    交通工具	飞机相关	28
    房地产	建筑	29
    房地产	物业	30
    消费品	消费品	31
    商业服务	法律	32
    商业服务	会展	33
    商业服务	中介服务	34
    商业服务	认证	35
    商业服务	审计	36
    文体娱乐	传媒	37
    文体娱乐	体育	38
    文体娱乐	娱乐休闲	39
    印刷	印刷	40
    其它	其它	41
     * */
    fun setIndustry(industryId1: String, industryId2: String): Response =
        doPost("api_set_industry", mapOf("industry_id1" to industryId1, "industry_id2" to industryId2))
}


/**
 * @param primary    是	帐号设置的主营行业
 * @param secondary    是	帐号设置的副营行业
}
 * */
@Serializable
class ResponseIndustry(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    @SerialName("primary_industry")
    val primary: Industry,
    @SerialName("secondary_industry")
    val secondary: Industry
) : IBase

@Serializable
class Industry(
    @SerialName("first_class")
    val first: String,
    @SerialName("second_class")
    val second: String? = null
)

@Serializable
class ResponseTemplateMsgId(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    @SerialName("template_id")
    val templateId: String
) : IBase

@Serializable
class ResponseTemplateList(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    @SerialName("template_list")
    val list: List<Template>
) : IBase

/**
 * @param templateId    是	模板ID
 * @param title    是	模板标题
 * @param primary    是	模板所属行业的一级行业
 * @param deputy    是	模板所属行业的二级行业
 * @param content    是	模板内容
 * @param example    是	模板示例
 * */
@Serializable
class Template(
    @SerialName("template_id")
    val templateId: String,
    val title: String,
    @SerialName("primary_industry")
    val primary: String,
    @SerialName("deputy_industry")
    val deputy: String,
    val content: String,
    val example: String
)