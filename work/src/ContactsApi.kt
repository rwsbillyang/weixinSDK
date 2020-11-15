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


object UserApi: WorkBaseApi(){
    override val group = "user"
    /**
     * 获取访问用户身份
     *
     * 根据code获取成员信息，用于网页授权后的身份信息获取
     * 跳转的域名须完全匹配access_token对应应用的可信域名，否则会返回50001错误。
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/91023
     * */
    fun getUserInfo(code: String) = doGet3("getuserinfo", mapOf("code" to code))

    fun create(body: Map<String, Any?>)= doPost3("create", body)

    fun detail(userId: String) = doGet3("get", mapOf("userid" to userId))

    fun update(body: Map<String, Any?>) = doPost3("update", body)

    fun delete(userId: String) = doGet3("delete", mapOf("userid" to userId))

    fun batchDelete(userIdList: List<String>) = doPost3("batchdelete",userIdList)

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
    { "$base/corp/get_join_qrcode?access_token=${accessToken()}&size_type=$sizeType"}
    /**
     * 获取手机号随机串
     * 支持企业获取手机号随机串，该随机串可直接在企业微信终端搜索手机号对应的微信用户。
     * */
    fun getMobileHashCode(mobile: String, state: String) = doPost3(
            "get_mobile_hashcode",
        mapOf("mobile" to mobile, "state" to state))

}


class DepartmentApi: WorkBaseApi(){
    override val group = "department"
    companion object{
        const val CREATE = "create"
        const val UPDATE = "update"
        const val DELETE = "delete"
        const val LIST = "list"
    }

    /**
     * 创建部门
     * https://work.weixin.qq.com/api/doc/90000/90135/90204
     * */
    fun create(body: Map<String, Any?>) = doPost3(CREATE,body)
    /**
     * https://work.weixin.qq.com/api/doc/90000/90135/90206
     * */
    fun update(body: Map<String, Any?>) = doPost3(UPDATE,body)

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

class TagApi: WorkBaseApi(){
    override val group = "tag"
    companion object{
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

class UserBatchApi: WorkBaseApi(){
    override val group = "batch"
    companion object{
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

    fun syncUser(mediaId: String, toInvite: Boolean = true, callback: BatchUserCallback?=null)
            = doPost3(
        SYNC_USER,
        BatchUserBody(mediaId, toInvite, callback)
    )

    fun replaceUser(mediaId: String, toInvite: Boolean = true, callback: BatchUserCallback?=null)
            = doPost3(
        REPLACE_USER,
        BatchUserBody(mediaId, toInvite, callback)
    )

    fun replaceParty(mediaId: String, callback: BatchUserCallback?=null)
            = doPost3(
        REPLACE_PARTY,
        BatchUserBody(mediaId, null, callback)
    )

    fun getResult(jobId: String)
            = doPost3(GET_RESULT, mapOf("jobid" to jobId))

}