/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 12:02
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

package com.github.rwsbillyang.wxSDK.wxPay.util

import java.io.IOException
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 用于 微信回调通知和平台证书下载时，对部分关键信息进行解密
 *
 * 为了保证安全性，微信支付在回调通知和平台证书下载接口中，对关键信息进行了AES-256-GCM加密。
 *
 * 证书和回调报文使用的加密密钥为APIv3密钥。
 *
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/qian-ming-zhi-nan-1/zheng-shu-he-hui-tiao-bao-wen-jie-mi
 * https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient
 * */
class AesUtil(key: ByteArray) {
    private val aesKey: ByteArray

    /**
     * @param associatedData 附加数据包（可能为空）
     * @param nonce 加密使用的随机串初始化向量
     * @param ciphertext Base64编码后的密文
     * */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun decryptToString(associatedData: ByteArray?, nonce: ByteArray, ciphertext: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = SecretKeySpec(aesKey, "AES")
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, nonce)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.updateAAD(associatedData)
            String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), Charset.forName("UTF-8"))
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException(e)
        } catch (e: NoSuchPaddingException) {
            throw IllegalStateException(e)
        } catch (e: InvalidKeyException) {
            throw IllegalArgumentException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw IllegalArgumentException(e)
        }
    }

    companion object {
        const val KEY_LENGTH_BYTE = 32
        const val TAG_LENGTH_BIT = 128
    }

    init {
        require(key.size == KEY_LENGTH_BYTE) { "无效的ApiV3Key，长度必须为32个字节" }
        aesKey = key
    }
}