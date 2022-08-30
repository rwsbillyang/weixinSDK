/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-11-23 13:41
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
package com.github.rwsbillyang.wxOA.qrcodeChannel

import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
import com.github.rwsbillyang.ktorKit.toObjectId
import io.ktor.resources.*

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.regex

//引导关注公众号的渠道二维码
@Serializable
data class QrCodeChannel(
    val _id: ObjectId,
    val appId: String, //公众号ID
    val name: String, //渠道名称
    val code: String, //渠道编码值
    val type: Int, //类型 0：临时 1：永久
    val remark: String? = null, //备注
    val qrCode:String? = null, //qrcode 解析后的url，可自行生成
    val imgUrl: String? = null, //下载到本地文件系统中的二维码，若被删除只能通过qrCode生成
    val time: Long, //创建时间
    val msgId: ObjectId? = null //通过渠道码关注后的响应的消息，优先级最高
    )

//编辑或新增时提交上来的数据
@Serializable
class ChannelBean(
    val _id: String? = null,
    val appId: String, //公众号ID
    val name: String, //渠道名称
    val code: String, //渠道编码值
    val type: Int, //类型 0：临时 1：永久
    val remark: String?, //备注
    val qrCode:String? = null, //qrcode 解析后的url
)

@Serializable
class QrCodeInfo(
    val qrCode:String? = null, //qrcode 解析后的url，可自行生成
    val imgUrl: String? = null, //下载到本地文件系统中的二维码，若被删除只能通过qrCode生成
)
/**
 * 列表过滤查询
 * @param lastId 上一条记录中最后一条的id，用于分页  Limitation： lastId只有在基于_id排序时才可正确地工作，而且只支持上下一页
 * */
@Serializable
@Resource("/list")
data class ChannelListParams(
    override val umi: String? = null,
    val _id: String? = null,
    val code: String? = null,
    val type: Int? = null,
    val appId: String? = null,
    val keyword: String? = null,
    val lastId: String? = null
): IUmiPaginationParams {
    override fun toFilter(): Bson {
        val idFilter = _id?.let { QrCodeChannel::_id eq it.toObjectId() }
        val codeFilter = code?.let { QrCodeChannel::code eq it }
        val appIdFilter = appId?.let { QrCodeChannel::appId eq it }
        val typeFilter = if(type != null) QrCodeChannel::type eq type else null
        val keywordFilter = keyword?.let { QrCodeChannel::name regex  (".*$it.*")}


        return and(idFilter, codeFilter, appIdFilter,typeFilter, keywordFilter)
    }
}