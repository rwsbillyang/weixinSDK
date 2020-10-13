package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.common.aes.SignUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

data class JsApiSignature(
    val noncestr: String? = null,
    val timestamp: Long = 0,
    val url: String? = null,
    val signature: String? = null
)

object JsAPI {
    private val log: Logger = LoggerFactory.getLogger("JsAPI")

    /**
     * 获取js-sdk所需的签名JsApiSignature
     * @param url 当前网页的URL，不包含#及其后面部分
     * @param nonceStr 随机字符串
     * @param timestamp 时间戳
     *
     * @return 签名以及相关参数
     */
    fun getSignature(url: String, nonceStr: String? = null, timestamp: Long? = null): JsApiSignature? {
        if(url.contains("#"))
        {
            log.error("cannot include # in url")
            return null
        }
        val jsApiTicket = OfficialAccount._OA.ticket.get()
        if(jsApiTicket == null){
            log.error("jsApiTicket is null, does you config correctly?")
            return null
        }

        val nonce = nonceStr?:UUID.randomUUID().toString().replace("-".toRegex(), "")
        val time = timestamp?:System.currentTimeMillis() / 1000
        val signature = SignUtil.jsApiSignature(jsApiTicket, nonce, time, url)

        return JsApiSignature(nonceStr, time,url, signature)
    }
}