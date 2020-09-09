package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.common.IBase
import com.github.rwsbillyang.wxSDK.common.Response
import com.github.rwsbillyang.wxSDK.officialAccount.outMsg.*

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * 群发消息及模板消息接口，实现更灵活的群发能力
 *
 * 群发图文消息的过程如下：
 *
 * 首先，预先将图文消息中需要用到的图片，使用上传图文消息内图片接口，上传成功并获得图片 URL；
 * 上传图文消息素材，需要用到图片时，请使用上一步获取的图片 URL；
 * 使用对用户标签的群发，或对 OpenID 列表的群发，将图文消息群发出去，群发时微信会进行原创校验，并返回群发操作结果；
 * 在上述过程中，如果需要，还可以预览图文消息、查询群发状态，或删除已群发的消息等。
 * 群发图片、文本等其他消息类型的过程如下：
 *
 * 如果是群发文本消息，则直接根据下面的接口说明进行群发即可；
 * 如果是群发图片、视频等消息，则需要预先通过素材管理接口准备好 mediaID。
 * 关于群发时使用is_to_all为true使其进入公众号在微信客户端的历史消息列表：
 *
 * 使用is_to_all为true且成功群发，会使得此次群发进入历史消息列表。
 * 为防止异常，认证订阅号在一天内，只能使用is_to_all为true进行群发一次，或者在公众平台官网群发（不管本次群发是对全体还是对某个分组）一次。以避免一天内有2条群发进入历史消息列表。
 * 类似地，服务号在一个月内，使用is_to_all为true群发的次数，加上公众平台官网群发（不管本次群发是对全体还是对某个分组）的次数，最多只能是4次。
 * 设置is_to_all为false时是可以多次群发的，但每个用户只会收到最多4条，且这些群发不会进入历史消息列表。
 * 另外，请开发者注意，本接口中所有使用到media_id的地方，现在都可以使用素材管理中的永久素材media_id了。请但注意，使用同一个素材群发出去的链接是一样的，这意味着，删除某一次群发，会导致整个链接失效。
 * */
class MsgApi : OABaseApi() {
    override val group: String = "message"

    /**
     * 发送模板消息
     * */
    fun sendTemplateMsg(msg: TemplateMsg): ResponseSendTemplateMsg = doPost2("template/send", msg)


    /**
     * 推送订阅模板消息给到授权微信用户
     * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/One-time_subscription_info.html
     * TODO: 跳转重定向entry point的处理
     * */
    fun sendSubscribeMsg(msg: OneTimeTemplateMsg): Response = doPost2("template/subscribe", msg)
    
    
    /**
     * ReceiverType使用Tag类型
     * */
    fun massSendByTag(msg: IMassMsg): ResponseMassSend = doPost2("mass/sendall", msg)

    /**
     * ReceiverType使用OpenIds类型
     * */
    fun massSendByOpenIds(msg: IMassMsg): ResponseMassSend = doPost2("mass/send", msg)

    /**
     * 预览接口【订阅号与服务号认证后均可用】 ReceiverType使用OpenId类型
     *
     * 在保留对openID预览能力的同时，增加了对指定微信号发送预览的能力，但该能力每日调用次数有限制（100次）
     * */
    fun preview(msg: IMassMsg): ResponseMassSend = doPost2("mass/preview", msg)

    /**
     * @param msgId    是	msg_id 发送出去的消息ID，来自massSendByTag 或 massSendByOpenIds
     * @param articleId article_idx	否	要删除的文章在图文消息中的位置，第一篇编号为1，该字段不填或填0会删除全部文章
     * */
    fun deleteMas(msgId: Long, articleId: Int? = null): Response = doPost2("mass/delete", mapOf("msg_id" to msgId, "article_idx" to articleId))

    /**
     * @param msgId msg_id	群发消息后返回的消息id
     * @return    消息发送后的状态，SEND_SUCCESS表示发送成功，SENDING表示发送中，SEND_FAIL表示发送失败，DELETE表示已删除
     * */
    fun getStatus(msgId: Long): String = doPost("mass/get", mapOf("msg_id" to msgId))["msg_status"].toString()


    /**
     * 群发速度的级别，是一个0到4的整数，数字越大表示群发速度越慢。
     * 0	80w/分钟
     * 1	60w/分钟
     * 2	45w/分钟
     * 3	30w/分钟
     * 4	10w/分钟
     * */
    fun setSpeed(speed: Int): String = doPost2("speed/set", mapOf("speed" to speed))

    fun getSpeed(): ResponseSpeed = doPost2("speed/get", null)



    /**
     * 客服接口-发消息 ReceiverType使用OpenId类型
     *
     * 目前允许的动作列表如下（公众平台会根据运营情况更新该列表，不同动作触发后，允许的客服接口下发消息条数不同，下发条数达到上限后，会遇到错误返回码，具体请见返回码说明页）：
     * 用户发送信息
     * 点击自定义菜单（仅有点击推事件、扫码推事件、扫码推事件且弹出“消息接收中”提示框这3种菜单类型是会触发客服接口的）
     * 关注公众号
     * 扫描二维码
     * 支付成功
     * */
    fun sendCustomerServiceMsg(msg: ICustomerMsg):Response = doPost2("custom/send",msg)

}


/**
 * 调用群发消息API后返回的结果
 * @param msgId    消息发送任务的ID
 * @param msgDataId 消息的数据ID，该字段只有在群发图文消息时，才会出现。
 * 可以用于在图文分析数据接口中，获取到对应的图文消息的数据，是图文分析数据接口中的msgid字段中的前半部分，
 * 详见图文分析数据接口中的msgid字段的介绍。
 * */
@Serializable
class ResponseMassSend(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("msgid")
        val msgId: Long,
        @SerialName("msg_data_id")
        val msgDataId: Long? = null
) : IBase

/**
 *
 * @param speed    是	群发速度的级别
 * @param realSpeed realspeed	是	群发速度的真实值 单位：万/分钟
 * */
@Serializable
class ResponseSpeed(
        val speed: Int,
        @SerialName("realspeed")
        val realSpeed: Int? = null
)

