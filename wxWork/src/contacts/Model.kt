/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-20 19:35
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

package com.github.rwsbillyang.wxWork.contacts

import com.github.rwsbillyang.ktorKit.apiJson.IUmiListParams
import com.github.rwsbillyang.ktorKit.apiJson.to64String
import com.github.rwsbillyang.ktorKit.apiJson.toObjectId
import com.github.rwsbillyang.wxSDK.work.*
import io.ktor.locations.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.and
import org.litote.kmongo.elemMatch
import org.litote.kmongo.eq
import org.litote.kmongo.regex

/**
 * 数据库表：内部成员
 * */
@Serializable
data class Contact(
    var _id: ObjectId?, // 因为userId可能会改变，故不能以corpId/userId作为键值
    val corpId: String,
    var userId: String, //userid
    val name: String? = null,//成员名称；第三方不可获取，调用时返回userid以代替name；代开发自建应用需要管理员授权才返回；对于非第三方创建的成员，第三方通讯录应用也不可获取；未返回name的情况需要通过通讯录展示组件来展示名字
    val mobile: String? = null,//手机号码，代开发自建应用需要管理员授权才返回；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val department: List<Int>? = null,//成员所属部门id列表，仅返回该应用有查看权限的部门id；成员授权模式下，固定返回根部门id，即固定为1
    val order: List<Int>? = null,//部门内的排序值，默认为0。数量必须和department一致，数值越大排序越前面。值范围是[0, 2^32)。成员授权模式下不返回该字段
    val position: String? = null,//position	职务信息；代开发自建应用需要管理员授权才返回；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val gender: String? = null, //0表示未定义，1表示男性，2表示女性
    val email: String? = null, //邮箱，代开发自建应用需要管理员授权才返回；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val isLeaderInDept: List<Int>? = null, //is_leader_in_dept 表示在所在的部门内是否为上级。第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val avatar: String? = null,//头像url。 第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val thumb: String? = null, //thumb_avatar 头像缩略图url。第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val tel: String? = null,    //座机。代开发自建应用需要管理员授权才返回；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val alias: String? = null, //别名；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val attr: List<Attr>? = null, //extattr	扩展属性，代开发自建应用需要管理员授权才返回；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val status: Int? = null,//激活状态: 1=已激活，2=已禁用，4=未激活，5=退出企业。 已激活代表已激活企业微信或已关注微工作台（原企业号）。未激活代表既未激活企业微信又未关注微工作台（原企业号）。
    val qr: String? = null, //qr_code 员工个人二维码，扫描可添加为外部联系人(注意返回的是一个url，可在浏览器上打开该url以展示二维码)；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val profile: ExternalProfile? = null, //external_profile	成员对外属性，字段详情见对外属性；代开发自建应用需要管理员授权才返回；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val externalPosition: String? = null, //external_position	对外职务，如果设置了该值，则以此作为对外展示的职务，否则以position来展示。代开发自建应用需要管理员授权才返回；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val address: String? = null, // 地址。代开发自建应用需要管理员授权才返回；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取
    val mainDep: Int? = null, //main_department	主部门
    val openUserId: String? = null,//全局唯一。对于同一个服务商，不同应用获取到企业内同一个成员的open_userid是相同的，最多64个字节。仅第三方应用可获取
    val hideMobile: Int? = null,//文档中没有，但返回字段中有
    val enable: Int? = null, // 文档中没有，但返回字段中有
    val isLeader: Int? = null,// 文档中没有，但返回字段中有
    val openId: String? = null //通过userId得到
   // var archive: Int = 0, //自定义字段：支持会话存档为1，否则为0
   // val ids: List<String>? = null //外部联系人external_id列表
){
    fun toBean(children: List<ContactBean>?, size: Long = -1) = ContactBean(_id!!.to64String(), userId, name, mobile, thumb, gender?.toInt(),
        childrenSize = if(size < 0) children?.size?.toLong()?:0L else size, status, children = children)
}
fun ResponseUserDetail.toDoc(corpId: String) = Contact(
    null, corpId,
    userid, name, mobile, department, order, position, gender, email, is_leader_in_dept,
    avatar,thumb_avatar,telephone, alias, extattr?.attrs,  status, qr_code,external_profile,
    external_position, address, main_department, open_userid, hide_mobile, enable, isleader)

/**
 * Contact作为微信官方数据存储，不添加任何扩展。
 * 扩展部分放到ContactExtra中，如是否支持会话存档、外部联系人列表，以及未来其它数据均可添加到此处
 * */
@Serializable
data class ContactExtra(
    val _id: ObjectId, // Contact._id  因为userId可能会改变，故不能以corpId/userId作为键值
    val corpId: String,
    val userId: String, //userid
    val archive: Int? = null, //支持会话存档为1，否则为0
    val ids: List<String>? = null //外部联系人external_id列表
)

/**
 * 与客户端交互的实体，随业务需要扩展
 * */
@Serializable
class ContactBean(
    val _id: String,// Contact._id or ExternalContact._id  因为userId可能会改变，故不能以corpId/userId作为键值
    val userId: String, //Contact.userId or ExternalContact.external_userId
    val name: String? = null,
    val mobile: String? = null,
    val thumb: String? = null,
    val gender: Int?, //0表示未定义，1表示男性，2表示女性
    val childrenSize: Long = 0L, //当查询的是客服列表时，表示客户数量；当查询的是客户列表时表示聊天消息数量

    //以下字段为内部联系人专有
    val status: Int? = null,//激活状态: 1=已激活，2=已禁用，4=未激活，5=退出企业。
    val archive: Int? = null, // 0 or 1
    val children: List<ContactBean>? = null, // ExternalContact

    //以下字段为外部联系人专有
    val addWay: Int? = null,// val add_way: Int, https://work.wxwork.qq.com/api/doc/90000/90135/92114
    val relationChanges: List<RelationChange>? = null, //关系变动列表
    val remark: String? = null
)


@Location("/list")
data class ContactListParams(
    val corpId: String,
    override val umi: String? = null,
    val archive: Int? = 1,
    val id: String? = null,
    val name: String? = null,
    val mobile: String? = null,
    val status: Int? = null,//激活状态: 1=已激活，2=已禁用，4=未激活，5=退出企业。
    val gender: String? = null, //0表示未定义，1表示男性，2表示女性
    val children: Int = 0, //是否获取其客户列表,默认不获取
    val lastId: String? = null
) : IUmiListParams {
    fun toFilter(): Bson {

        val idFilter = id?.let { Contact::_id eq it.toObjectId() }
        val nameFilter = name?.let { Contact::name regex ".*$it.*" }
        val mobileFilter = mobile?.let { Contact::mobile eq it }
        val statusFilter = status?.let { Contact::status eq it }
        val genderFilter = gender?.let { Contact::gender eq it }

        return and(idFilter, Contact::corpId eq corpId, mobileFilter,  statusFilter, genderFilter,nameFilter)
    }

    fun toExtraFilter(): Bson{
        val archiveFilter = archive?.let { ContactExtra::archive eq it }
        val idFilter = id?.let { Contact::_id eq it.toObjectId() }
        return and(idFilter, ContactExtra::corpId eq corpId, archiveFilter)
    }
}





/**
 * 外部联系人，通常指客户
 * */
@Serializable
data class ExternalContact(
    var _id: ObjectId?,
    val corpId: String,
    val externalId: String, //external_userid	外部联系人的userid
    val name: String? = null, ////如果是微信用户，则返回其微信昵称。如果是企业微信联系人，则返回其设置对外展示的别名或实名
    val avatar: String? = null,//外部联系人头像，代开发自建应用需要管理员授权才可以获取，第三方不可获取
    val type: Int, //外部联系人的类型，1表示该外部联系人是微信用户，2表示该外部联系人是企业微信用户
    val gender: Int, //外部联系人性别 0-未知 1-男性 2-女性
    val unionId: String? = null,//外部联系人在微信开放平台的唯一身份标识（微信unionid），通过此字段企业可将外部联系人与公众号/小程序用户关联起来。仅当联系人类型是微信用户，且企业或第三方服务商绑定了微信开发者ID有此字段：https://work.weixin.qq.com/api/doc/90000/90135/92114
    val position: String? = null,//外部联系人的职位，如果外部企业或用户选择隐藏职位，则不返回，仅当联系人类型是企业微信用户时有此字段
    val corpName: String? = null,//外部联系人所在企业的简称，仅当联系人类型是企业微信用户时有此字段
    val corpFullName: String? = null,//外部联系人所在企业的主体名称，仅当联系人类型是企业微信用户时有此字段
    val externalProfile: ExternalProfile? = null, //外部联系人的自定义展示信息，可以有多个字段和多种类型，包括文本，网页和小程序，仅当联系人类型是企业微信用户时有此字段
    val followers: List<FollowUser>? = null,
    val openId: String? = null //通过external_userId得到
){
    /**
     * 若不是特定成员的客户，则contactUserId为空
     * */
    fun toBean(contactUserId: String?, msgCount: Long, relationChanges: List<RelationChange>?):ContactBean{
        return if(contactUserId == null){
            ContactBean(_id!!.to64String(), externalId,  name, null, avatar,  gender)
        }else{
            val remarkInfo = followers?.firstOrNull { it.userid == contactUserId }
            ContactBean(_id!!.to64String(), externalId, name,remarkInfo?.remark_mobiles?.firstOrNull(), avatar,  gender,
                childrenSize = msgCount,
                addWay = remarkInfo?.add_way,
                relationChanges = relationChanges,
                remark = remarkInfo?.remark
            )
        }

    }
}

@Location("/external/list")
data class ExternalListParams(
    val corpId: String,

    var userId: String? = null, //指定成员的客户 用于follow中的查询
    val contactId: String? = null, //指定成员的客户 contactId为可选参数，用于查询relationChanges

    var channelId: String? = null, //特定渠道客户，由业务逻辑指定，也就是可以业务逻辑修改强行指向某个channel
    override val umi: String? = null,

    val id: String? = null,
    val name: String? = null,
    val type: Int? = null,
    val gender: Int? = null, //0表示未定义，1表示男性，2表示女性
    val lastId: String? = null
) : IUmiListParams {
    fun toFilter(): Bson {
        val corpIdFilter = ExternalContact::corpId eq corpId

        val userChannelFilter = if(channelId != null)
        {
            ExternalContact::followers.elemMatch( and(FollowUser::state eq channelId, FollowUser::userid eq userId))
        }else {
            ExternalContact::followers.elemMatch( FollowUser::userid eq userId)
        }


        val idFilter = id?.let { ExternalContact::_id eq it.toObjectId() }
        val nameFilter = name?.let { ExternalContact::name regex ".*$it.*" }
        val typeFilter = type?.let { ExternalContact::type eq it }
        val genderFilter = gender?.let { ExternalContact::gender eq it }

        return and(idFilter,corpIdFilter, userChannelFilter,  typeFilter, genderFilter,nameFilter)
    }

}


/**
 * 与客户端交互的实体，随业务需要扩展
add_way
0	未知来源
1	扫描二维码
2	搜索手机号
3	名片分享
4	群聊
5	手机通讯录
6	微信联系人
7	来自微信的添加好友申请
8	安装第三方应用时自动添加的客服人员
9	搜索邮箱
201	内部成员共享
202	管理员/负责人分配
 * */
fun ResponseExternalContactDetail.toDoc(corpId: String) = external_contact?.run {
    ExternalContact(
        null, corpId, external_userid, name, avatar, type, gender,
        unionid, position, corp_name, corp_full_name, external_profile, follow_user
    )
}

/**
 * 成员与外部联系人关系变动历史表
 * 来源于消息事件通知
 * */
@Serializable
data class RelationChange(
    val _id: ObjectId,
    val corpId: String,
    val userId: String, //userId有可能更改，不能作为查询参数，只能作为参考
    val contactId: ObjectId?, //不存在时才使用userId
    val externalUserId: String,
    val type: Int,
    val time: String,
    val ctime: Long?,
    val state: String?
){
    companion object{
        const val TypeUserAddExternal = 1//成员添加客户
        const val TypeExternalAddUser = 2//客户添加成员
        const val TypeUserDelExternal = -1//成员删除客户
        const val TypeExternalDelUser = -2 //客户删除了成员

        const val TypeTransferFail = -10 // trasfer fail, 此时state表示failReason
    }
}


@Serializable
data class Department(
    val _id: ObjectId?,
    val corpId: String,
    val id: Int, //部门Id
    val name: String?, //部门名称
    val parentId: Int? = null,  //父部门id
    val order: Int? = null,//部门排序
    val nameEn: String? = null,
    val leader: List<String>? = null
)

/**
 * 获取部门所含成员列表，当agent可见范围只有部门时，需要获取其里面的所有成员
 * */
//@Serializable
//data class ContactDepartment(
//    val _id: ObjectId?,
//    val corpId: String,
//    val userId: String,
//    val name: String? = null, //成员名称，代开发自建应用需要管理员授权才返回；此字段从2019年12月30日起，对新创建第三方应用不再返回真实name，使用userid代替name，2020年6月30日起，对所有历史第三方应用不再返回真实name，使用userid代替name，后续第三方仅通讯录应用可获取，未返回名称的情况需要通过通讯录展示组件来展示名字
//    val department: List<Int>, //成员所属部门列表。列表项为部门ID，32位整型
//    val openUserId: String? = null //全局唯一。对于同一个服务商，不同应用获取到企业内同一个成员的open_userid是相同的，最多64个字节。仅第三方应用可获取
//)