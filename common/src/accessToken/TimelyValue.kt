package com.github.rwsbillyang.wxSDK.accessToken

import kotlinx.serialization.Serializable

/**
 * 有时间期限的值
 * @param value 值
 * @param time 生效起始utc时间，在time+expireTime之后过期
 * */
@Serializable
open class TimelyValue(
    var value: String? = null,
    var time: Long = 0L
)

/**
 * 值类型枚举
 * @property ACCESS_TOKEN 微信token
 * @property TICKET  微信ticket
 */
enum class UpdateType {
    ACCESS_TOKEN,
    TICKET,
    WX_WORK_SUITE_TOKEN,
    WX_WORK_PROVIDER_TOKEN //https://work.weixin.qq.com/api/doc/90001/90142/90593
}

/**
 * accessToken, JsToken变化时的通知内容
 *
 * @param appId
 * @param value 新值
 * @param time 时间
 * @param type 是哪种值变化
 * */
@Serializable
data class UpdateMsg(var appId: String, var type: UpdateType, var value: String?, var time: Long, val extra: String?)
