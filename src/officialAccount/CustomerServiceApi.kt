package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.common.IBase
import com.github.rwsbillyang.wxSDK.common.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 请注意，必须先在公众平台官网为公众号设置微信号后才能使用该能力
 *
 * 如果公众号处于开发模式，普通微信用户向公众号发消息时，微信服务器会先将消息POST到开发者填写的url上，如果希望将消息转发到客服系统，
 * 则需要开发者在响应包中返回MsgType为transfer_customer_service的消息，微信服务器收到响应后会把当次发送的消息转发至客服系统。
 * 您也可以在返回transfer_customer_service消息时，在XML中附上TransInfo信息指定分配给某个客服帐号。
 * 用户被客服接入以后，客服关闭会话以前，处于会话过程中时，用户发送的消息均会被直接转发至客服系统。当会话超过30分钟客服没有关闭时，
 * 微信服务器会自动停止转发至客服，而将消息恢复发送至开发者填写的url上。用户在等待队列中时，用户发送的消息仍然会被推送至开发者填写的url上。
 * */
object CustomerServiceApi : OABaseApi(){
    override val group: String = "customservice"

    /**
     * 获取客服列表
     * */
    fun getAccountList():ResponseAccountList = doGet2("getkflist")

    /**
     * 添加客服帐号
     *
     * 新添加的客服帐号是不能直接使用的，只有客服人员用微信号绑定了客服账号后，方可登录Web客服进行操作。
     *
     * @param account kf_account	帐号前缀 完整客服帐号的前缀，帐号前缀最多10个字符，必须是英文、数字字符或者下划线，此处为前缀。最终账号完整格式为：帐号前缀@公众号微信号，故配置中wechatId不能为空。
     * @param nickname	客服昵称，最长16个字
     * */
    fun accountAdd(account: String, nickname: String): Response = doPost2("kfaccount/add", mapOf("kf_account" to "$account@${_OA.wechatId}", "nickname" to nickname))

    /**
     * 邀请绑定客服帐号
     * 新添加的客服帐号是不能直接使用的，只有客服人员用微信号绑定了客服账号后，方可登录Web客服进行操作。
     * 此接口发起一个绑定邀请到客服人员微信号，客服人员需要在微信客户端上用该微信号确认后帐号才可用。
     * 尚未绑定微信号的帐号可以进行绑定邀请操作，邀请未失效时不能对该帐号进行再次绑定微信号邀请。
     * @param  account kf_account	帐号前缀
     * @param inviteWx invite_wx	接收绑定邀请的客服微信号
     * */
    fun accountInvite(account: String, inviteWx: String): Response = doPost2("kfaccount/inviteworker", mapOf("kf_account" to "$account@${_OA.wechatId}", "invite_wx" to inviteWx))


    /**
     * 更新客服信息
     * @param  account kf_account	帐号前缀
     * */
    fun accountUpdate(account: String, nickname: String): Response  = doPost2("kfaccount/update", mapOf("kf_account" to "$account@${_OA.wechatId}", "nickname" to nickname))

    /**
     * 上传客服头像
     * @param  account kf_account	帐号前缀
     * */
    fun accountUploadHeadImg(account: String, file: String): Response  = doUpload2("kfaccount/uploadheadimg", file, mapOf("kf_account" to "$account@${_OA.wechatId}"))


    /**
     * 删除客服帐号
     * @param  account kf_account	帐号前缀
     * */
    fun accountDel(account: String): Response  = doGet2("kfaccount/del", mapOf("kf_account" to "$account@${_OA.wechatId}"))



    /**
     * 创建会话
     * 此接口在客服和用户之间创建一个会话，如果该客服和用户会话已存在，则直接返回0。指定的客服帐号必须已经绑定微信号且在线。
     * @param  account kf_account	帐号前缀
     * @param customer 客户的openId
     * */
    fun sessionCreate(account: String, customer: String): Response  = doPost2("kfsession/create", mapOf("kf_account" to "$account@${_OA.wechatId}", "openid" to customer))

    /**
     * 关闭会话
     * @param  account kf_account	帐号前缀
     * @param customer 客户的openId
     * */
    fun sessionClose(account: String, customer: String): Response = doPost2("kfsession/close", mapOf("kf_account" to "$account@${_OA.wechatId}", "openid" to customer))


    /**
     * 根据某个客户信息，获取客户会话状态
     * */
    fun sessionState(customer: String): ResponseSessionState = doGet2("kfsession/getsession", mapOf("openid" to customer))

    /**
     * 获取某个客服的会话列表
     *
     * 可以遍历所有客服，然后获取各个客户的会话列表，并进而根据列表项中的粉丝openId查询会话状态
     * */
    fun sessionList(account: String): ResponseSessionList = doGet2("kfsession/getsessionlist", mapOf("kf_account" to "$account@${_OA.wechatId}"))

    fun sessionWaitList(): ResponseWaitList = doGet2("kfsession/getwaitcase")


    /**
     * 获取聊天记录
     *
     * 此接口返回的聊天记录中，对于图片、语音、视频，分别展示成文本格式的[image]、[voice]、[video]。对于较可能包含重要信息的图片消息，后续将提供图片拉取URL，近期将上线。
     * @param startTime starttime	起始时间，unix时间戳
     * @param endTime endtime	结束时间，unix时间戳，每次查询时段不能超过24小时
     * @param msgId msgid	起始消息id，顺序从小到大，第一次从1开始
     * @param size number	每次获取条数，最多10000条
     * */
    fun chatHistory(startTime: Long, endTime: Long, msgId: Long, size: Int):ResponseChatList = doPost2("msgrecord/getmsglist",
            mapOf("starttime" to startTime, "endtime" to endTime, "msgid" to msgId, "number" to size))
}

@Serializable
class ResponseAccountList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("kf_list")
        val list: List<KfAccount> ? = null
): IBase
/**
 * @param account kf_account	完整客服帐号，格式为：帐号前缀@公众号微信号
 * @param nickname kf_nick	客服昵称
 * @param id kf_id	客服编号
 * @param headImg kf_headimgurl	客服头像
 * @param wechatId kf_wx	如果客服帐号已绑定了客服人员微信号， 则此处显示微信号
 * @param inviteWx invite_wx	如果客服帐号尚未绑定微信号，但是已经发起了一个绑定邀请， 则此处显示绑定邀请的微信号
 * @param inviteExpire invite_expire_time	如果客服帐号尚未绑定微信号，但是已经发起过一个绑定邀请， 邀请的过期时间，为unix 时间戳
 * @param inviteStatus invite_status	邀请的状态，有等待确认“waiting”，被拒绝“rejected”， 过期“expired”
 * */
@Serializable
class KfAccount(
        @SerialName("kf_account")
        val account: String,
        @SerialName("kf_nick")
        val nickname: String,
        @SerialName("kf_id")
        val id: String? = null,
        @SerialName("kf_headimgurl")
        val headImg: String? = null,
        @SerialName("kf_wx")
        val wechatId: String? = null,
        @SerialName("invite_wx")
        val inviteWx: String? = null,
        @SerialName("invite_expire_time")
        val inviteExpire: Long? = null,
        @SerialName("invite_status")
        val inviteStatus: String? = null
)
/**
 * 获取未接入会话列表
 *
 * @param account	正在接待的客服，为空表示没有人在接待
 * @param createTime	会话接入的时
 * */
@Serializable
class ResponseSessionState(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("kf_account")
        val account: String? = null,
        @SerialName("createtime")
        val createTime: Long? = null
): IBase


@Serializable
class ResponseSessionList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("sessionlist")
        val list: List<SessionListItem> ? = null
): IBase

/**
 * @param customer	客户open id
 * @param createTime	会话接入的时
 * */
@Serializable
class SessionListItem(
        @SerialName("openid")
        val customer:  String? = null,
        @SerialName("createtime")
        val createTime: Long? = null
)

/**
 * @param count	未接入会话数量
 * @param list waitcaselist	未接入会话列表，最多返回100条数据，按照来访顺序
 * */
@Serializable
class ResponseWaitList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("waitcaselist")
        val list: List<WaitItem> ? = null,
        val count: Int = 0
): IBase

/**
 * @param openId 粉丝的openid
 * @param latestTime latest_time	粉丝的最后一条消息的时间
 * */
@Serializable
class WaitItem(
        @SerialName("openid")
        val openId: String,
        @SerialName("latest_time")
        val latestTime: Long
)

/**
 * @param number 未接入会话数量
 * @param list waitcaselist	未接入会话列表，最多返回100条数据，按照来访顺序
 * */
@Serializable
class ResponseChatList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("recordlist")
        val list: List<ChatItem> ? = null,
        val number: Int? = null,
        @SerialName("msgid")
        val msgId: Long? = null
): IBase

/**
 * @param worker	完整客服帐号，格式为：帐号前缀@公众号微信号
 * @param openId openid	用户标识
 * @param code opercode	操作码，2002（客服发送信息），2003（客服接收消息）
 * @param text	聊天记录
 * @param time	操作时间，unix时间戳
 * */
@Serializable
class ChatItem(
        val worker: String,
        @SerialName("openid")
        val openId: String,
        @SerialName("opercode")
        val code: Int,
        val text: String,
        val time: Long
)

