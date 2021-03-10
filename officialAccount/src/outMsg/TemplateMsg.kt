package com.github.rwsbillyang.wxSDK.officialAccount.outMsg

import com.github.rwsbillyang.wxSDK.IBase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


//============================= 发送出去的 模版消息 =============================//
/**
 * 待发送的模版消息
 * url和miniprogram都是非必填字段，若都不传则模板无跳转；若都传，会优先跳转至小程序。
 * 开发者可根据实际需要选择其中一种跳转方式即可。当用户的微信客户端版本不支持跳小程序时，将会跳转至url。
 *
 * @param toUser    是	接收者openid
 * @param templateId    是	模板ID
 * @param data    是	模板数据
 * @param url    否	模板跳转链接（海外帐号没有跳转能力）
 * @param mini    否	跳小程序所需数据，不需跳小程序可不用传该数据
 *
 */
@Serializable
class TemplateMsg(
        @SerialName("touser")
        val toUser: String,
        @SerialName("template_id")
        val templateId: String,
        val data: Map<String, ColoredValue>,
        val url: String? = null,
        @SerialName("miniprogram")
        val mini: MiniProgram? = null
)

/**
 *
 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/One-time_subscription_info.html
 * @param toUser    是	接收者openid
 * @param templateId    是	订阅消息模板ID
 * @param data    是	模板数据
 * @param url    否	模板跳转链接（海外帐号没有跳转能力）
 * @param mini    否	跳小程序所需数据，不需跳小程序可不用传该数据
 * @param scene    是	订阅场景值
 * @param title    是	消息标题，15字以内
 * @param data    是	消息正文，value为消息内容文本（200字以内），没有固定格式，可用\n换行，
 * color为整段消息内容的字体颜色（目前仅支持整段消息为一种颜色）
 *
 * 注：url和miniprogram都是非必填字段，若都不传则模板无跳转；若都传，会优先跳转至小程序。开发者可根据实际需要选择其中一种跳转方式即可。
 * 当用户的微信客户端版本不支持跳小程序时，将会跳转至url。
 * */
@Serializable
class OnceTemplateMsg(
        @SerialName("touser")
        val toUser: String,
        @SerialName("template_id")
        val templateId: String,
        val scene: Int,
        val title: String,
        val data: Map<String, ColoredValue>,
        val url: String? = null,
        @SerialName("miniprogram")
        val mini: MiniProgram? = null
) {
    constructor(
            toUser: String,
            templateId: String,
            scene: Int,
            title: String,
            content: String,
            url: String? = null,
            mini: MiniProgram? = null,
            color: String = "173177") :
            this(toUser, templateId, scene, title, mapOf("content" to ColoredValue(content, color)), url, mini)
}

/**
 * 调用发送模板消息API后返回的结果
 * */
@Serializable
class ResponseSendTemplateMsg(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("msgid")
        val msgId: Long
) : IBase


/**
 * 模板消息中的小程序
 * @param appId appid    是	所需跳转到的小程序appid（该小程序appid必须与发模板消息的公众号是绑定关联关系，暂不支持小游戏）
 * @param pagePath pagepath    否	所需跳转到小程序的具体页面路径，支持带参数,（示例index?foo=bar），要求该小程序已发布，暂不支持小游戏
 * */
@Serializable
class MiniProgram(
        @SerialName("appid")
        val appId: String,
        @SerialName("pagepath")
        val pagePath: String
)

/**
 * 模板消息中带颜色的值对
 * @param value 传递过去的值
 * @param color    否	模板内容字体颜色，不填默认为黑色
 * */
@Serializable
class ColoredValue(
        val value: String,
        val color: String? = null, //"#173177"
)

