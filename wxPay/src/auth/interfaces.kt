/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 13:32
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



import io.ktor.client.statement.*
import java.security.cert.X509Certificate

/**
 * 微信平台证书验证器
 * */
interface Verifier {
    fun verify(serialNumber: String, message: ByteArray, signature: String): Boolean
    val validCertificate: X509Certificate?
}

/**
 * 用微信平台证书（动态更新）验证回复和回调是否真正来自微信支付
 * */
interface Validator {
    /**
     * 返回bodyText文本，出现任何错误都抛出异常
     * */
    fun validate(response: HttpResponse): String
}

/**
 * 向微信支付发出请求时，需要带上商户信息、签名、平台证书序列号等信息，作为请求头
 * */
interface Credentials {
    val schema: String

    /**
     * https://pay.weixin.qq.com/wiki/doc/apiv3/wechatpay/wechatpay4_0.shtml
     *
     * @param method GET, POST
     * @param canonicalUrl 包括？后面的查询参数
     * @param body 获取请求中的请求报文主体（request body）。
     * 请求方法为GET时，报文主体为空。
     * 当请求方法为POST或PUT时，请使用真实发送的JSON报文。
     * 图片上传API，请使用meta对应的JSON报文。
     * 对于下载证书的接口来说，请求报文主体是一个空串。
     * */
    fun getToken(method: String, canonicalUrl: String, body: String): String
}

/**
 * 用私钥签名，返回签名和正在使用的平台证书序列号
 * */
interface Signer {
    fun sign(message: ByteArray): SignatureResult
    class SignatureResult(var sign: String, var certificateSerialNumber: String)
}