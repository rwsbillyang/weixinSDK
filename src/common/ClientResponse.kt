package com.github.rwsbillyang.wxSDK.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



/**
 * 开发者需根据errcode存在且不为0判断为失败，否则为成功（errcode意义请见全局错误码）。
 * 而errmsg仅作参考，后续可能会有变动，因此不可作为是否调用成功的判据。
 * */
interface IBase{
    val errCode: Int?
    val errMsg: String?
    fun isOK() = errCode == 0
}

@Serializable
open class Response(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null
): IBase

@Serializable
class ResponseAccessToken(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = 0
): IBase

@Serializable
data class ResponseTicket(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    val ticket: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = 0
): IBase

@Serializable
data class ResponseCallbackIps(
    @SerialName("errcode")
    val errCode: Int?,
    @SerialName("errmsg")
    val errMsg: String?,
    @SerialName("ip_list")
    val ipList: List<String>?
)



/**
 * @param accessToken    是	调用接口凭证。获取方法查看“获取access_token”
 * @param userId userid	是	成员UserID。对应管理端的帐号，企业内必须唯一。不区分大小写，长度为1~64个字节。只能由数字、字母和“_-@.”四种字符组成，且第一个字符必须是数字或字母。
 * @param name    是	成员名称。长度为1~64个utf8字符
 * @param alias    否	成员别名。长度1~32个utf8字符
 * @param mobile    否	手机号码。企业内必须唯一，mobile/email二者不能同时为空
 * @param department    是	成员所属部门id列表,不超过20个
 * @param order    否	部门内的排序值，默认为0，成员次序以创建时间从小到大排列。数量必须和department一致，数值越大排序越前面。有效的值范围是[0, 2^32)
 * @param position    否	职务信息。长度为0~128个字符
 * @param gender    否	性别。1表示男性，2表示女性
 * @param email    否	邮箱。长度6~64个字节，且为有效的email格式。企业内必须唯一，mobile/email二者不能同时为空
 * @param telephone    否	座机。32字节以内，由纯数字或’-‘号组成。
 * @param isLeaderInDept is_leader_in_dept	否	个数必须和department一致，表示在所在的部门内是否为上级。1表示为上级，0表示非上级。在审批等应用里可以用来标识上级审批人
 * @param avatarMediaId avatar_mediaid	否	成员头像的mediaid，通过素材管理接口上传图片获得的mediaid
 * @param enable    否	启用/禁用成员。1表示启用成员，0表示禁用成员
 * @param extattr    否	自定义字段。自定义字段需要先在WEB管理端添加，见扩展属性添加方法，否则忽略未知属性的赋值。与对外属性一致，不过只支持type=0的文本和type=1的网页类型，详细描述查看对外属性
 * @param to_invite    否	是否邀请该成员使用企业微信（将通过微信服务通知或短信或邮件下发邀请，每天自动下发一次，最多持续3个工作日），默认值为true。
 * @param external_profile    否	成员对外属性，字段详情见对外属性
 * @param external_position    否	对外职务，如果设置了该值，则以此作为对外展示的职务，否则以position来展示。长度12个汉字内
 * @param address    否	地址。长度最大128个字符
 * */
@Serializable
data class Member(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("userid")
    val userId: String,
    val name: String,
    val alias: String? = null,
    val gender: Int? = null,

    val mobile: String? = null,
    val email: String? = null,
    val telephone: String? = null,

    val department: List<Int>? = null,
    val order: List<Int>? = null,
    @SerialName("is_leader_in_dept")
    val isLeaderInDept: List<Int>? = null,
    val position: String? = null,

    @SerialName("avatar_mediaid")
    val avatarMediaId: String? = null,
    val enable: Int? = null,
    val extattr: String? = null,
    val to_invite: Boolean = true,
    val external_profile: String? = null,
    val external_position: String? = null,
    val address: String? = null
)
