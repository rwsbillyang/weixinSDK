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

import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpRequestWrapper
import java.io.IOException
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
    @Throws(IOException::class)
    fun validate(response: CloseableHttpResponse): Boolean
}

/**
 * 向微信支付发出请求时，需要带上商户信息、签名、平台证书序列号等信息，作为请求头
 * */
interface Credentials {
    val schema: String

    @Throws(IOException::class)
    fun getToken(request: HttpRequestWrapper): String
}

/**
 * 用私钥签名，返回签名和正在使用的平台证书序列号
 * */
interface Signer {
    fun sign(message: ByteArray): SignatureResult
    class SignatureResult(var sign: String, var certificateSerialNumber: String)
}