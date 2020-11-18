/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-18 22:51
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

package com.github.rwsbillyang.wxSDK.security

import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

/**
 * 为了保证通信过程中敏感信息字段（如用户的住址、银行卡号、手机号码等）的机密性，微信支付API v3要求商户对上送的敏感
 * 信息字段进行加密。与之相对应，微信支付会对下行的敏感信息字段进行加密，商户需解密后方能得到原文。
 *
 * 敏感信息加密使用的RSA公钥加密算法。加密算法使用的填充方案，使用了相对更安全的RSAES-OAEP(Optimal Asymmetric Encryption Padding)。
 * 微信支付使用商户证书中的公钥对下行的敏感信息进行加密。开发者应使用商户私钥对下行的敏感信息的密文进行解密。
 *
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/qian-ming-zhi-nan-1/min-gan-xin-xi-jia-mi
 * https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient
 * */
object RsaCryptoUtil {

    /**
     * 用于微信支付
     * */
    @Throws(IllegalBlockSizeException::class)
    fun encryptOAEP(message: String, certificate: X509Certificate): String {
        return try {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, certificate.publicKey)
            val data = message.toByteArray(StandardCharsets.UTF_8)
            val ciphertext = cipher.doFinal(data)
            Base64.getEncoder().encodeToString(ciphertext)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("当前Java环境不支持RSA v1.5/OAEP", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("当前Java环境不支持RSA v1.5/OAEP", e)
        } catch (e: InvalidKeyException) {
            throw IllegalArgumentException("无效的证书", e)
        } catch (e: IllegalBlockSizeException) {
            throw IllegalBlockSizeException("加密原串的长度不能超过214字节")
        } catch (e: BadPaddingException) {
            throw IllegalBlockSizeException("加密原串的长度不能超过214字节")
        }
    }
    /**
     * 用于微信支付
     * */
    @Throws(BadPaddingException::class)
    fun decryptOAEP(ciphertext: String, privateKey: PrivateKey): String {
        return try {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val data = Base64.getDecoder().decode(ciphertext)
            String(cipher.doFinal(data), StandardCharsets.UTF_8)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("当前Java环境不支持RSA v1.5/OAEP", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("当前Java环境不支持RSA v1.5/OAEP", e)
        } catch (e: InvalidKeyException) {
            throw IllegalArgumentException("无效的私钥", e)
        } catch (e: BadPaddingException) {
            throw BadPaddingException("解密失败")
        } catch (e: IllegalBlockSizeException) {
            throw BadPaddingException("解密失败")
        }
    }
    /**
     * 用于企业微信对聊天消息的解密
     * */
    @Throws(BadPaddingException::class)
    fun decryptPKCS1(ciphertext: String, privateKey: PrivateKey): String{
        return try {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val data = Base64.getDecoder().decode(ciphertext)
            String(cipher.doFinal(data), StandardCharsets.UTF_8)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("当前Java环境不支持RSA v1.5/OAEP", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("当前Java环境不支持RSA v1.5/OAEP", e)
        } catch (e: InvalidKeyException) {
            throw IllegalArgumentException("无效的私钥", e)
        } catch (e: BadPaddingException) {
            throw BadPaddingException("解密失败")
        } catch (e: IllegalBlockSizeException) {
            throw BadPaddingException("解密失败")
        }
    }
}
