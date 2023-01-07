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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*


/**
 * 通讯录同步相关接口，可以对部门、成员、标签等通讯录信息进行查询、添加、修改、删除等操作。
 * https://open.work.weixin.qq.com/api/doc/90000/90135/90193
 *
 * 使用通讯录管理接口，原则上需要使用 通讯录管理secret，也可以使用 应用secret。
 * 但是使用应用secret只能进行“查询”、“邀请”等非写操作，而且只能操作应用可见范围内的通讯录。
 *
 * 获取通讯录管理secret的方法如下：
1、进入企业微信管理后台，在“管理工具” — “通讯录同步助手”开启“API接口同步”
 * */
class ContactsApi(corpId: String?, agentId: String?, suiteId: String?) : WorkBaseApi(corpId, agentId, suiteId) {

    override val group = "user"
    override var sysAccessTokenKey: String? = SysAgentKey.Contact.name


    fun create(body: Map<String, Any?>) = doPostRaw("create", body)

    fun detail(userId: String): ResponseUserDetail = doGet("get", mapOf("userid" to userId))

    fun update(body: Map<String, Any?>) = doPostRaw("update", body)

    fun delete(userId: String) = doPostRaw("delete", mapOf("userid" to userId))

    fun batchDelete(userIdList: List<String>) = doPostRaw("batchdelete", userIdList)

    fun simpleList(departmentId: Int, fetchChild: Int): ResponseSimpleList = doGet(
        "simplelist",
        mapOf("department_id" to departmentId.toString(), "fetch_child" to fetchChild.toString())
    )

    fun list(departmentId: Int, fetchChild: Int) = doGetRaw(
        "list",
        mapOf("department_id" to departmentId.toString(), "fetch_child" to fetchChild.toString())
    )

    fun convertToOpenId(userId: String): ResponseToOpenId = doPost("convert_to_openid", mapOf("userid" to userId))

    /**
     * 该接口主要应用于使用企业支付之后的结果查询。
     * 开发者需要知道某个结果事件的openid对应企业微信内成员的信息时，可以通过调用该接口进行转换查询。
     * */
    fun convertToUserId(openId: String) = doPostRaw("convert_to_userid", mapOf("openid" to openId))

    fun authSucc(userId: String) = doGetRaw("authsucc", mapOf("userid" to userId))


    /**
     * 获取加入企业二维码
     * 支持企业用户获取实时成员加入二维码。
     *  https://work.weixin.qq.com/api/doc/90000/90135/91714
     * */
    fun getJoinQrCode(sizeType: Int) = doGetRawByUrl("$base/corp/get_join_qrcode?access_token=${accessToken()}&size_type=$sizeType")


    /**
     * 获取手机号随机串
     * 支持企业获取手机号随机串，该随机串可直接在企业微信终端搜索手机号对应的微信用户。
     * */
    fun getMobileHashCode(mobile: String, state: String) = doPostRaw(
        "get_mobile_hashcode",
        mapOf("mobile" to mobile, "state" to state)
    )


    /**
     * 3rd服务商使用
     * 获取成员授权列表
     * 当企业当前授权模式为成员授权时，可调用该接口获取成员授权列表。
     * */
    fun getAuthMemberList(cursor: String? = null, limit: Int? = null): ResponseAuthMemberList =
        doGet("list_member_auth", mapOf("cursor" to cursor, "limit" to limit.toString()))

    /**
     * 3rd服务商使用
     * 查询成员用户是否已授权
     * 当企业当前授权模式为成员授权时，可调用该接口查询成员用户是否已授权
     * */
    fun checkMemberAuth(openUserId: String): ResponseCheckMemberAuth =
        doGet("check_member_auth", mapOf("open_userid" to openUserId))


    /**
     * 3rd服务商使用
     * 获取SelectedTicket用户
     * 当第三方应用支持成员授权时，可调用该接口获取SelectedTicket对应的用户open_userid（只会返回应用可见范围内的用户open_userid）。
     * @param selectedTicket 选人sdk或者选人jsapi返回的ticket
     * */
    fun getSelectedTicket(selectedTicket: String): ResponseSelectedTicket =
        doGet("list_selected_ticket_user", mapOf("selected_ticket" to selectedTicket))
}

@Serializable
class ResponseSimpleList(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    @SerialName("userlist")
    val userList: List<SimpleUserListItem>? = null
) : IBase

@Serializable
class SimpleUserListItem(
    @SerialName("userid")
    val userId: String,
    val name: String,
    val department: List<Int>?,
    @SerialName("open_userid")
    val openUserId: String? = null
)


@Serializable
class ResponseToOpenId(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    @SerialName("openid")
    val openId: String? = null
) : IBase

@Serializable
class OpenUserId(val open_userid: String)

@Serializable
class ResponseAuthMemberList(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("member_auth_list")
    val list: List<OpenUserId>? = null
) : IBase

@Serializable
class ResponseCheckMemberAuth(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    @SerialName("is_member_auth")
    val isMemberAuth: Boolean? = null
) : IBase


@Serializable
class ResponseSelectedTicket(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    val operator_open_userid: String? = null, //选人用户的open_userid
    val open_userid_list: List<String>? = null, //应用可见范围内的用户open_userid
    val total: Int? = null //用户选择的总人数
) : IBase


class DepartmentApi(corpId: String?, agentId: String?, suiteId: String?) : WorkBaseApi(corpId, agentId, suiteId) {

    override val group = "department"

    companion object {
        const val CREATE = "create"
        const val UPDATE = "update"
        const val DELETE = "delete"
        const val LIST = "list"
    }

    /**
     * 创建部门
     * https://work.weixin.qq.com/api/doc/90000/90135/90204
     * */
    fun create(body: Map<String, Any?>) = doPostRaw(CREATE, body)

    /**
     * https://work.weixin.qq.com/api/doc/90000/90135/90206
     * */
    fun update(body: Map<String, Any?>) = doPostRaw(UPDATE, body)

    /**
     *  @param id 部门id。（注：不能删除根部门；不能删除含有子部门、成员的部门）
     * https://work.weixin.qq.com/api/doc/90000/90135/90207
     * */
    fun delete(id: Int) = doPostRaw(DELETE, mapOf("id" to id.toString()))

    /**
     *
     * @param id 部门id。获取指定部门及其下的子部门。 如果不填，默认获取全量组织架构
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90208
     * */
    fun list(id: Int? = null):ResponseDepartment = if(id != null) doGet(LIST, mapOf("id" to id.toString())) else doGet(LIST)

}

class TagApi(corpId: String?, agentId: String?, suiteId: String?) : WorkBaseApi(corpId, agentId, suiteId) {
    override val group = "tag"

    /**
     * 创建
     * https://work.weixin.qq.com/api/doc/90000/90135/90210
     * */
    fun create(tagname: String, id: Int?) = doPostRaw("create", mapOf("tagname" to tagname, "tagid" to id))

    /**
     * https://work.weixin.qq.com/api/doc/90000/90135/90211
     * */
    fun update(tagname: String, id: Int) = doPostRaw("update", mapOf("tagname" to tagname, "tagid" to id))

    /**
     * https://work.weixin.qq.com/api/doc/90000/90135/90212
     * */
    fun delete(id: Int) = doPostRaw("delete", mapOf("tagid" to id.toString()))

    /**
     * 获取标签成员
     * https://work.weixin.qq.com/api/doc/90000/90135/90213
     * */
    fun detail(id: Int):ResponseTagDetail = doGet("get", mapOf("tagid" to id.toString()))

    /**
     * 增加标签成员
     * 注意：userlist、partylist不能同时为空，单次请求长度不超过1000
     * https://work.weixin.qq.com/api/doc/90000/90135/90214
     * */
    fun addTagUsers(id: Int, userlist: List<String>?, partylist: List<Int>?) = doPostRaw(
        "addtagusers",
        mapOf("tagid" to id, "userlist" to userlist, "partylist" to partylist)
    )


    /**
     * 删除标签成员
     * 注意：userlist、partylist不能同时为空，单次请求长度不超过1000
     * https://work.weixin.qq.com/api/doc/90000/90135/90214
     * */
    fun delTagUsers(id: Int, userlist: List<String>?, partylist: List<Int>?) = doPostRaw(
        "deltagusers",
        mapOf("tagid" to id, "userlist" to userlist, "partylist" to partylist)
    )

    /**
     * 获取标签列表
     * */
    fun list() = doGetRaw("list")
}

@Serializable
class ResponseTagDetail(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    @SerialName("tagname")
    val tagName: String? = null,
    @SerialName("userlist")
    val userList: List<SimpleUserListItem>? = null,
    @SerialName("partylist")
    val partyList: List<Int>? = null
) : IBase

class BatchUserCallback(
    val url: String?,
    val token: String?,
    val encodingaeskey: String?
)

class BatchUserBody(val mediaId: String, toInvite: Boolean?, callback: BatchUserCallback?)

class UserBatchApi(corpId: String?, agentId: String?, suiteId: String?) : WorkBaseApi(corpId, agentId, suiteId) {

    override val group = "batch"

    companion object {
        const val INVITE = "invite"
        const val SYNC_USER = "syncuser"
        const val REPLACE_USER = "replaceuser"
        const val REPLACE_PARTY = "replaceparty"
        const val GET_RESULT = "getresult"
    }

    /**
     * 邀请成员
     * 企业可通过接口批量邀请成员使用企业微信，邀请后将通过短信或邮件下发通知。
     *  https://work.weixin.qq.com/api/doc/90000/90135/90975
     * user, party, tag三者不能同时为空；
     * 如果部分接收人无权限或不存在，邀请仍然执行，但会返回无效的部分（即invaliduser或invalidparty或invalidtag）;
     * 同一用户只须邀请一次，被邀请的用户如果未安装企业微信，在3天内每天会收到一次通知，最多持续3天。
     * 因为邀请频率是异步检查的，所以调用接口返回成功，并不代表接收者一定能收到邀请消息（可能受上述频率限制无法接收）。
     * */
    fun invite(user: List<Int>?, party: List<Int>?, tag: List<Int>?) = doPostRaw(
        INVITE,
        mapOf("user" to user, "party" to party, "tag" to tag)
    )

    fun syncUser(mediaId: String, toInvite: Boolean = true, callback: BatchUserCallback? = null) = doPostRaw(
        SYNC_USER,
        BatchUserBody(mediaId, toInvite, callback)
    )

    fun replaceUser(mediaId: String, toInvite: Boolean = true, callback: BatchUserCallback? = null) = doPostRaw(
        REPLACE_USER,
        BatchUserBody(mediaId, toInvite, callback)
    )

    fun replaceParty(mediaId: String, callback: BatchUserCallback? = null) = doPostRaw(
        REPLACE_PARTY,
        BatchUserBody(mediaId, null, callback)
    )

    fun getResult(jobId: String) = doPostRaw(GET_RESULT, mapOf("jobid" to jobId))

}


@Serializable
class ResponseUserDetail(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    val userid: String? = null,
    val name: String? = null,
    val mobile: String? = null,
    val avatar: String? = null,
    val thumb_avatar: String? = null,
    val telephone: String? = null,    //座机。第三方仅通讯录应用可获取
    val alias: String? = null,
    val status: Int? = null,//激活状态: 1=已激活，2=已禁用，4=未激活，5=退出企业。
    val gender: String? = null, //0表示未定义，1表示男性，2表示女性
    val email: String? = null,
    val biz_mail: String? = null,
    val address: String? = null,
    val extattr: ExtAttr? = null,

    val enable: Int? = null, //根据返回结果添加，文档中没有此字段说明
    val hide_mobile: Int? = null, //根据返回结果添加，文档中没有此字段说明

    val department: List<Int>? = null,
    val main_department: Int? = null,
    /**
     * TODO:官方文档里没有此字段，但返回结果有此字段
     * */
    val isleader: Int? = null,
    val is_leader_in_dept: List<Int>? = null,
    val direct_leader: List<String>? = null,
    val order: List<Int>? = null,
    val position: String? = null,

    val qr_code: String? = null,
    val external_position: String? = null,
    val external_profile: ExternalProfile? = null,

    val open_userid: String? = null
) : IBase

@Serializable
class ExternalProfile(
    val external_corp_name: String? = null, //内部联系人拥有该属性,外部联系人无此属性
    val external_attr: List<Attr>? = null,
    val wechat_channels: WechatChannel? = null
)

@Serializable
class WechatChannel(
    val nickname: String,//对外展示视频号名称（即微信视频号名称）。第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val status: Int//对外展示视频号状态。0表示企业视频号已被确认，可正常使用，1表示企业视频号待确认。第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
)

@Serializable
class Text(val value: String)

@Serializable
class Web(val url: String, val title: String)

@Serializable
class MiniProgram(val appid: String, val pagepath: String, val title: String)

@Serializable(with = AttrSerializer::class)
sealed class Attr {
    abstract val type: Int
    abstract val name: String
}

@Serializable
class TextAttr(override val type: Int, override val name: String, val text: Text) : Attr()

@Serializable
class WebAttr(override val type: Int, override val name: String, val web: Web) : Attr()

@Serializable
class MiniProgramAttr(override val type: Int, override val name: String, val miniprogram: MiniProgram) : Attr()

@Serializable
class ExtAttr(val attrs: List<Attr>? = null)

class AttrSerializer : KSerializer<Attr> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Attr")
    override fun deserialize(decoder: Decoder): Attr {
        // Decoder -> JsonDecoder
        require(decoder is JsonDecoder) // this class can be decoded only by Json
        // JsonDecoder -> JsonElement
        val element = decoder.decodeJsonElement()
        // JsonElement -> value
        require(element is JsonObject)
        return when (val type = element.getValue("type").jsonPrimitive.int) {
            0 -> decoder.json.decodeFromJsonElement(TextAttr.serializer(), element)
            1 -> decoder.json.decodeFromJsonElement(WebAttr.serializer(), element)
            2 -> decoder.json.decodeFromJsonElement(MiniProgramAttr.serializer(), element)
            else -> error("not support type=$type")
        }
    }

    override fun serialize(encoder: Encoder, value: Attr) {
        // Encoder -> JsonEncoder
        require(encoder is JsonEncoder) // This class can be encoded only by Json
        // value -> JsonElement
        val element = when (value) {
            is TextAttr -> encoder.json.encodeToJsonElement(TextAttr.serializer(), value)
            is WebAttr -> encoder.json.encodeToJsonElement(WebAttr.serializer(), value)
            is MiniProgramAttr -> encoder.json.encodeToJsonElement(MiniProgramAttr.serializer(), value)
        }
        // JsonElement -> JsonEncoder
        encoder.encodeJsonElement(element)
    }
}

@Serializable
class DepartmentItem(
    val id: Int, //部门Id
    val name: String? = null, //部门名称
    @SerialName("name_en")
    val nameEn: String? = null,
    @SerialName("department_leader")
    val leader: List<String>? = null,
    @SerialName("parentid")
    val parentId: Int? = null,  //父部门id
    val order: Int? = null//部门排序
)

@Serializable
class ResponseDepartment(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    val department: List<DepartmentItem>? = null
    ) : IBase