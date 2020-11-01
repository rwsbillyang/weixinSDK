/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 12:18
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

import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.SignatureException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.HashMap

/**
 * 用微信支付平台证书,  来验证收到的消息及签名是否合法，即是否来自微信官方
 * */
class CertificatesVerifier(list: List<X509Certificate>) : Verifier {
    private val certificates = HashMap<BigInteger, X509Certificate>()
    private fun verify(certificate: X509Certificate?, message: ByteArray?, signature: String?): Boolean {
        return try {
            val sign = Signature.getInstance("SHA256withRSA")
            sign.initVerify(certificate)
            sign.update(message)
            sign.verify(Base64.getDecoder().decode(signature))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("当前Java环境不支持SHA256withRSA", e)
        } catch (e: SignatureException) {
            throw RuntimeException("签名验证过程发生了错误", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("无效的证书", e)
        }
    }

    /**
     * @param serialNumber 微信支付签名使用微信支付平台私钥，证书序列号包含在应答HTTP头部的Wechatpay-Serial
     * 商户上送敏感信息时使用微信支付平台公钥加密，证书序列号包含在请求HTTP头部的Wechatpay-Serial
     * */
    override fun verify(serialNumber: String, message: ByteArray, signature: String): Boolean {
        val value = BigInteger(serialNumber, 16)
        return certificates.containsKey(value) && verify(certificates[value], message, signature)
    }

    override val validCertificate: X509Certificate
        get() {
            for (x509Cert in certificates.values) {
                return try {
                    x509Cert.checkValidity()
                    x509Cert
                } catch (e: CertificateExpiredException) {
                    continue
                } catch (e: CertificateNotYetValidException) {
                    continue
                }
            }
            throw NoSuchElementException("没有有效的微信支付平台证书")
        }

    init {
        for (item in list) {
            certificates[item.serialNumber] = item
        }
    }
}