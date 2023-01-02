/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-31 21:57
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

@file:UseContextualSerialization(ObjectId::class)

package com.github.rwsbillyang.wxWork.channel


import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
import com.github.rwsbillyang.ktorKit.toObjectId
import io.ktor.resources.*


import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.*


/**
 * 销售渠道，如推荐人，推荐购买，推荐添加企业微信等
 * @param _id 用于配置客户联系「联系我」方式中的state，不超过30个字符
 *
 * 不依赖于Account， account可清除重建
 * */
@Serializable
data class Channel(
    var _id: ObjectId? = null,//用户编码，如作为state编码于客服的二维码之中
    val name: String, //渠道名称，人工维护，自己填写或管理员填写 联系方式的备注信息，用于助记，不超过30个字符
    val tel: String? = null, //渠道电话，人工维护，自己填写或管理员填写
    val remark: String? = null, //渠道备注说明
    val corpId: String, //所属企业
    val userId: String, //客服或销售人员
    val time: Long = System.currentTimeMillis(), //create time

    val openId: String? = null,  //客服或销售人员信息  用于对于微信公众号或isv账户的openId
    val unionId: String? = null, //客服或销售人员信息  用于对于微信公众号或isv账户的unionId
    val externalId: String? = null, //客服或销售人员信息 企业微信外部联系人对应的账号

    val nick: String? = null,//客服或销售人员信息 冗余信息，来自微信用户信息
    val avatar: String? = null,//客服或销售人员信息  冗余信息，来自微信用户信息
    val city: String? = null,//客服或销售人员信息 冗余信息，来自微信用户信息
    val province: String? = null, //客服或销售人员信息 冗余信息，来自微信用户信息

    val qrCode: String? = null, //二维码url地址
    val configId: String? = null
)

///**
// * 前端新增、修改channel时提交给后端的信息，code为唯一字符串，出现重复将导致出错
// * */
//@Serializable
//class ChannelInfo(
//    val name: String,
//    val tel: String? = null,
//    val remark: String? = null,
//    val _id: String? = null)




/**
 * 列表过滤查询
 * @param lastId 上一条记录中最后一条的id，用于分页  Limitation： lastId只有在基于_id排序时才可正确地工作，而且只支持上下一页
 * */
@Serializable
@Resource("/list")
data class ChannelListParams(
    override val umi: String? = null,
    val _id: String? = null,
    val openId: String? = null,
    val userId: String? = null,
    val corpId: String? = null,
    val keyword: String? = null
): IUmiPaginationParams {
    override fun toFilter(): Bson {
        val idFilter = _id?.let { Channel::_id eq it.toObjectId() }
        val corpIdFilter = corpId?.let { Channel::corpId eq it }
        val userFilter = if(openId != null) Channel::openId eq openId
        else if(userId != null)Channel::userId eq userId else null

        val keywordFilter = keyword?.let { Channel::name regex  (".*$it.*")}


        return and(idFilter, userFilter, corpIdFilter, keywordFilter)
    }
}


