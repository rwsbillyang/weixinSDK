/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 12:21
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

package com.github.rwsbillyang.wxSDK.wxPay.auth




import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.security.SecureRandom


/**
 * 用私钥及商户个体信息，生成一系列信息及签名，作为token，放置于请求头中：
 *      Authorization: 认证类型[WECHATPAY2-SHA256-RSA2048] 签名信息
 *
 * 微信支付API v3要求商户对请求进行签名。微信支付会在收到请求后进行签名的验证。如果签名验证不通过，
 * 微信支付API v3将会拒绝处理请求，并返回401 Unauthorized。
 *
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/qian-ming-zhi-nan-1/qian-ming-sheng-cheng#ji-suan-qian-ming-zhi
 * https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient
 * */
class WechatPayCredentials(var merchantId: String,  var signer: Signer): Credentials {
    companion object {
        private val log = LoggerFactory.getLogger("WechatPay2Credentials")
        private const val SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val RANDOM = SecureRandom()
    }
    override val schema = "WECHATPAY2-SHA256-RSA2048"

    /**
     * @param method 如 GET, POST
     * @param canonicalUrl 如：/v3/certificates
     * @param body GET时为空
     * */
    override fun getToken(method: String, canonicalUrl: String, body: String): String {
        val nonceStr = generateNonceStr()
        val timestamp = System.currentTimeMillis() / 1000
        val message = buildMessage(method, canonicalUrl, timestamp, nonceStr, body)

        log.info("authorization message=[$message]")
        val signature = signer.sign(message.toByteArray(StandardCharsets.UTF_8))
        val token = ("mchid=\"" + merchantId + "\","
                + "nonce_str=\"" + nonceStr + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + signature.certificateSerialNumber + "\","
                + "signature=\"" + signature.sign + "\"")
        log.info("message.length=${message.length}")
        return token
    }


    private fun generateNonceStr(): String {
        val nonceChars = CharArray(32)
        for (index in nonceChars.indices) {
            nonceChars[index] = SYMBOLS[RANDOM.nextInt(SYMBOLS.length)]
        }
        return String(nonceChars)
    }


    /**
     * https://pay.weixin.qq.com/wiki/doc/apiv3/wechatpay/wechatpay4_0.shtml
     *
     * @param method GET, POST
     * @param canonicalUrl 包括？后面的查询参数
     * @param timestamp 获取发起请求时的系统当前时间戳，即格林威治时间1970年01月01日00时00分00秒(北京时间1970年01月01日08时00分00秒)起至现在的总秒数，作为请求时间戳。微信支付会拒绝处理很久之前发起的请求，请商户保持自身系统的时间准确。
     * @param nonceStr 生成一个请求随机串（我们推荐生成随机数算法如下：调用随机数函数生成，将得到的值转换为字符串）
     * @param body 获取请求中的请求报文主体（request body）。
     * 请求方法为GET时，报文主体为空。
     * 当请求方法为POST或PUT时，请使用真实发送的JSON报文。
     * 图片上传API，请使用meta对应的JSON报文。
     * 对于下载证书的接口来说，请求报文主体是一个空串。
    HTTP请求方法\n
    URL\n
    请求时间戳\n
    请求随机串\n
    请求报文主体\n
     * */
    private fun buildMessage(method: String, canonicalUrl: String, timestamp: Long, nonceStr: String,
                             body: String) = "$method\n$canonicalUrl\n$timestamp\n$nonceStr\n$body\n"

}
