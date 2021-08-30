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
class ContactsApi private constructor (corpId: String?) : WorkBaseApi(corpId){
    /**
     * ISV模式，suiteId为null表示single单应用模式
     * */
    constructor(suiteId: String?, corpId: String) : this(corpId) {
        this.suiteId = suiteId
    }

    /**
     * 企业内部应用模式，空参表示single单应用模式
     * */
    constructor(corpId: String? = null, agentId: Int? = null) : this(corpId) {
        this.agentId = agentId
    }

    override val group = "user"

    fun create(body: Map<String, Any?>) = doPost3("create", body)

    fun detail(userId: String):ResponseUserDetail = doGet("get", mapOf("userid" to userId))

    fun update(body: Map<String, Any?>) = doPost3("update", body)

    fun delete(userId: String) = doGet3("delete", mapOf("userid" to userId))

    fun batchDelete(userIdList: List<String>) = doPost3("batchdelete", userIdList)

    fun simpleList(departmentId: Int, fetchChild: Int) = doGet3(
            "simplelist",
            mapOf("department_id" to departmentId.toString(), "fetch_child" to fetchChild.toString()))

    fun list(departmentId: Int, fetchChild: Int) = doGet3(
            "list",
            mapOf("department_id" to departmentId.toString(), "fetch_child" to fetchChild.toString()))

    fun convertToOpenId(userId: String) = doPost3("convert_to_openid", mapOf("userid" to userId))

    /**
     * 该接口主要应用于使用企业支付之后的结果查询。
     * 开发者需要知道某个结果事件的openid对应企业微信内成员的信息时，可以通过调用该接口进行转换查询。
     * */
    fun convertToUserId(openId: String) = doPost3("convert_to_userid", mapOf("openid" to openId))

    fun authSucc(userId: String) = doGet3("authsucc", mapOf("userid" to userId))


    /**
     * 获取加入企业二维码
     * 支持企业用户获取实时成员加入二维码。
     *  https://work.weixin.qq.com/api/doc/90000/90135/91714
     * */
    fun getJoinQrCode(sizeType: Int) = doGet()
    { "$base/corp/get_join_qrcode?access_token=${accessToken()}&size_type=$sizeType" }

    /**
     * 获取手机号随机串
     * 支持企业获取手机号随机串，该随机串可直接在企业微信终端搜索手机号对应的微信用户。
     * */
    fun getMobileHashCode(mobile: String, state: String) = doPost3(
            "get_mobile_hashcode",
            mapOf("mobile" to mobile, "state" to state))


    /**
     * 3rd服务商使用
     * 获取成员授权列表
     * 当企业当前授权模式为成员授权时，可调用该接口获取成员授权列表。
     * */
    fun getAuthMemberList(cursor: String? = null, limit: Int? = null): ResponseAuthMemberList
     = doGet("list_member_auth", mapOf("cursor" to cursor, "limit" to limit.toString()))

    /**
     * 3rd服务商使用
     * 查询成员用户是否已授权
     * 当企业当前授权模式为成员授权时，可调用该接口查询成员用户是否已授权
     * */
    fun checkMemberAuth(openUserId: String): ResponseCheckMemberAuth
            = doGet("check_member_auth", mapOf("open_userid" to openUserId))


    /**
     * 3rd服务商使用
     * 获取SelectedTicket用户
     * 当第三方应用支持成员授权时，可调用该接口获取SelectedTicket对应的用户open_userid（只会返回应用可见范围内的用户open_userid）。
     * @param selectedTicket 选人sdk或者选人jsapi返回的ticket
     * */
    fun getSelectedTicket(selectedTicket: String):ResponseSelectedTicket = doGet("list_selected_ticket_user", mapOf("selected_ticket" to selectedTicket))
}

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
): IBase

@Serializable
class ResponseCheckMemberAuth(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    @SerialName("is_member_auth")
    val isMemberAuth: Boolean? = null
): IBase


@Serializable
class ResponseSelectedTicket(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,

    val operator_open_userid: String? = null, //选人用户的open_userid
    val open_userid_list: List<String>? = null, //应用可见范围内的用户open_userid
    val total: Int? = null //用户选择的总人数
): IBase



class DepartmentApi private constructor (corpId: String?) : WorkBaseApi(corpId){
    /**
     * ISV模式，suiteId为null表示single单应用模式
     * */
    constructor(suiteId: String?, corpId: String) : this(corpId) {
        this.suiteId = suiteId
    }

    /**
     * 企业内部应用模式，空参表示single单应用模式
     * */
    constructor(corpId: String? = null, agentId: Int? = null) : this(corpId) {
        this.agentId = agentId
    }

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
    fun create(body: Map<String, Any?>) = doPost3(CREATE, body)

    /**
     * https://work.weixin.qq.com/api/doc/90000/90135/90206
     * */
    fun update(body: Map<String, Any?>) = doPost3(UPDATE, body)

    /**
     *  @param id 部门id。（注：不能删除根部门；不能删除含有子部门、成员的部门）
     * https://work.weixin.qq.com/api/doc/90000/90135/90207
     * */
    fun delete(id: Int) = doGet3(DELETE, mapOf("id" to id.toString()))

    /**
     *
     * @param id 部门id。获取指定部门及其下的子部门。 如果不填，默认获取全量组织架构
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90208
     * */
    fun list(id: Int?) = doGet3(LIST, mapOf("id" to id.toString()))

}

class TagApi private constructor (corpId: String?) : WorkBaseApi(corpId){
    /**
     * ISV模式，suiteId为null表示single单应用模式
     * */
    constructor(suiteId: String?, corpId: String) : this(corpId) {
        this.suiteId = suiteId
    }

    /**
     * 企业内部应用模式，空参表示single单应用模式
     * */
    constructor(corpId: String? = null, agentId: Int? = null) : this(corpId) {
        this.agentId = agentId
    }

    override val group = "tag"

    companion object {
        const val CREATE = "create"
        const val UPDATE = "update"
        const val DELETE = "delete"
        const val LIST = "list"
        const val DETAIL = "get"
        const val ADD_TAG_USERS = "addtagusers"
        const val DEL_TAG_USERS = "deltagusers"
    }

    /**
     * 创建
     * https://work.weixin.qq.com/api/doc/90000/90135/90210
     * */
    fun create(tagname: String, id: Int?) = doPost3(CREATE, mapOf("tagname" to tagname, "tagid" to id))

    /**
     * https://work.weixin.qq.com/api/doc/90000/90135/90211
     * */
    fun update(tagname: String, id: Int) = doPost3(UPDATE, mapOf("tagname" to tagname, "tagid" to id))

    /**
     * https://work.weixin.qq.com/api/doc/90000/90135/90212
     * */
    fun delete(id: Int) = doGet3(DELETE, mapOf("tagid" to id.toString()))

    /**
     * 获取标签成员
     * https://work.weixin.qq.com/api/doc/90000/90135/90213
     * */
    fun detail(id: Int) = doGet3(DETAIL, mapOf("tagid" to id.toString()))

    /**
     * 增加标签成员
     * 注意：userlist、partylist不能同时为空，单次请求长度不超过1000
     * https://work.weixin.qq.com/api/doc/90000/90135/90214
     * */
    fun addTagUsers(id: Int, userlist: List<String>?, partylist: List<Int>?) = doPost3(
            ADD_TAG_USERS,
            mapOf("tagid" to id, "userlist" to userlist, "partylist" to partylist))


    /**
     * 删除标签成员
     * 注意：userlist、partylist不能同时为空，单次请求长度不超过1000
     * https://work.weixin.qq.com/api/doc/90000/90135/90214
     * */
    fun delTagUsers(id: Int, userlist: List<String>?, partylist: List<Int>?) = doPost3(
            DEL_TAG_USERS,
            mapOf("tagid" to id, "userlist" to userlist, "partylist" to partylist))

    fun list() = doGet3(LIST, null)
}

class BatchUserCallback(val url: String?,
                        val token: String?,
                        val encodingaeskey: String?)

class BatchUserBody(val mediaId: String, toInvite: Boolean?, callback: BatchUserCallback?)

class UserBatchApi private constructor (corpId: String?) : WorkBaseApi(corpId){
    /**
     * ISV模式，suiteId为null表示single单应用模式
     * */
    constructor(suiteId: String?, corpId: String) : this(corpId) {
        this.suiteId = suiteId
    }

    /**
     * 企业内部应用模式，空参表示single单应用模式
     * */
    constructor(corpId: String? = null, agentId: Int? = null) : this(corpId) {
        this.agentId = agentId
    }

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
    fun invite(user: List<Int>?, party: List<Int>?, tag: List<Int>?) = doPost3(
            INVITE,
            mapOf("user" to user, "party" to party, "tag" to tag))

    fun syncUser(mediaId: String, toInvite: Boolean = true, callback: BatchUserCallback? = null) = doPost3(
            SYNC_USER,
            BatchUserBody(mediaId, toInvite, callback)
    )

    fun replaceUser(mediaId: String, toInvite: Boolean = true, callback: BatchUserCallback? = null) = doPost3(
            REPLACE_USER,
            BatchUserBody(mediaId, toInvite, callback)
    )

    fun replaceParty(mediaId: String, callback: BatchUserCallback? = null) = doPost3(
            REPLACE_PARTY,
            BatchUserBody(mediaId, null, callback)
    )

    fun getResult(jobId: String) = doPost3(GET_RESULT, mapOf("jobid" to jobId))

}


/**
{
"errcode": 0,
"errmsg": "ok",
"userid": "zhangsan",
"name": "李四",
"department": [1, 2],
"order": [1, 2],
"position": "后台工程师",
"mobile": "13800000000",
"gender": "1",
"email": "zhangsan@gzdev.com",
"is_leader_in_dept": [1, 0],
"avatar": "http://wx.qlogo.cn/mmopen/ajNVdqHZLLA3WJ6DSZUfiakYe37PKnQhBIeOQBO4czqrnZDS79FH5Wm5m4X69TBicnHFlhiafvDwklOpZeXYQQ2icg/0",
"thumb_avatar": "http://wx.qlogo.cn/mmopen/ajNVdqHZLLA3WJ6DSZUfiakYe37PKnQhBIeOQBO4czqrnZDS79FH5Wm5m4X69TBicnHFlhiafvDwklOpZeXYQQ2icg/100",
"telephone": "020-123456",
"alias": "jackzhang",
"address": "广州市海珠区新港中路",
"open_userid": "xxxxxx",
"main_department": 1,
"extattr": {
"attrs": [
{
"type": 0,
"name": "文本名称",
"text": {
"value": "文本"
}
},
{
"type": 1,
"name": "网页名称",
"web": {
"url": "http://www.test.com",
"title": "标题"
}
}
]
},
"status": 1,
"qr_code": "https://open.work.weixin.qq.com/wwopen/userQRCode?vcode=xxx",
"external_position": "产品经理",
"external_profile": {
"external_corp_name": "企业简称",
"external_attr": [{
"type": 0,
"name": "文本名称",
"text": {
"value": "文本"
}
},
{
"type": 1,
"name": "网页名称",
"web": {
"url": "http://www.test.com",
"title": "标题"
}
},
{
"type": 2,
"name": "测试app",
"miniprogram": {
"appid": "wx8bd80126147dFAKE",
"pagepath": "/index",
"title": "my miniprogram"
}
}
]
}
}

{"errcode":0,"errmsg":"ok",
"userid":"YangShaoWen",
"name":"杨绍文",
"department":[1],
"position":"",
"mobile":"17503715758",
"gender":"1",
"email":"",
"avatar":"https://wework.qpic.cn/wwhead/duc2TvpEgSTPk74IwG7Bs3icnQHhUYvPg1WJyp97g81ibrPcj7kTjA00Nv8h9bVCeRZLGDYeHaNk4/0",
"status":1,
"isleader":0,
"extattr":{"attrs":[]},
"telephone":"",
"enable":1,
"hide_mobile":0,
"order":[0],
"main_department":1,
"qr_code":"https://open.work.weixin.qq.com/wwopen/userQRCode?vcode=vc18885293950e8b25",
"alias":"文文",
"is_leader_in_dept":[0],
"thumb_avatar":"https://wework.qpic.cn/wwhead/duc2TvpEgSTPk74IwG7Bs3icnQHhUYvPg1WJyp97g81ibrPcj7kTjA00Nv8h9bVCeRZLGDYeHaNk4/100"
}
参数说明：

参数	说明
errcode	返回码
errmsg	对返回码的文本描述内容
userid	成员UserID。对应管理端的帐号，企业内必须唯一。不区分大小写，长度为1~64个字节
name	成员名称，此字段从2019年12月30日起，对新创建第三方应用不再返回真实name，使用userid代替name，
2020年6月30日起，对所有历史第三方应用不再返回真实name，使用userid代替name，后续第三方仅通讯录应用可获取，
第三方页面需要通过通讯录展示组件来展示名字
mobile	手机号码，第三方仅通讯录应用可获取
department	成员所属部门id列表，仅返回该应用有查看权限的部门id
order	部门内的排序值，默认为0。数量必须和department一致，数值越大排序越前面。值范围是[0, 2^32)
position	职务信息；第三方仅通讯录应用可获取
gender	性别。0表示未定义，1表示男性，2表示女性
email	邮箱，第三方仅通讯录应用可获取
is_leader_in_dept	表示在所在的部门内是否为上级。；第三方仅通讯录应用可获取
avatar	头像url。 第三方仅通讯录应用可获取
thumb_avatar	头像缩略图url。第三方仅通讯录应用可获取
telephone	座机。第三方仅通讯录应用可获取
alias	别名；第三方仅通讯录应用可获取
extattr	扩展属性，第三方仅通讯录应用可获取
status	激活状态: 1=已激活，2=已禁用，4=未激活，5=退出企业。
已激活代表已激活企业微信或已关注微工作台（原企业号）。未激活代表既未激活企业微信又未关注微工作台（原企业号）。
qr_code	员工个人二维码，扫描可添加为外部联系人(注意返回的是一个url，可在浏览器上打开该url以展示二维码)；第三方仅通讯录应用可获取
external_profile	成员对外属性，字段详情见对外属性；第三方仅通讯录应用可获取
external_position	对外职务，如果设置了该值，则以此作为对外展示的职务，否则以position来展示。第三方仅通讯录应用可获取
address	地址。第三方仅通讯录应用可获取
open_userid	全局唯一。对于同一个服务商，不同应用获取到企业内同一个成员的open_userid是相同的，最多64个字节。仅第三方应用可获取
main_department	主部门
 * */
@Serializable
class ResponseUserDetail(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,

        val userid: String,
        val name: String? = null,
        val mobile: String? = null,
        val avatar: String? = null,
        val thumb_avatar: String? = null,
        val telephone: String? = null,    //座机。第三方仅通讯录应用可获取
        val alias: String? = null,
        val status: Int? = null,//激活状态: 1=已激活，2=已禁用，4=未激活，5=退出企业。
        val gender: String, //0表示未定义，1表示男性，2表示女性
        val email: String? = null,
        val address: String? = null,
        val extattr: ExtAttr? = null,

        val enable:Int? = null, //根据返回结果添加，文档中没有此字段说明
        val hide_mobile: Int? =null, //根据返回结果添加，文档中没有此字段说明

        val department: List<Int>? = null,
        val main_department: Int? = null,
        /**
         * TODO:官方文档里没有此字段，但返回结果有此字段
         * */
        val isleader: Int? = null,
        val is_leader_in_dept: List<Int>? = null,
        val order: List<Int>? = null,
        val position: String? = null,

        val qr_code: String? = null,
        val external_position: String? = null,
        val external_profile: ExternalProfile? = null,

        val open_userid: String? = null
): IBase

@Serializable
class ExternalProfile(
        val external_corp_name: String? = null, //内部联系人拥有该属性,外部联系人无此属性
        val external_attr: List<Attr>? = null
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