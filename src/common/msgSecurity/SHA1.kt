package com.github.rwsbillyang.wxSDK.common.msgSecurity

import java.security.MessageDigest
import java.util.*
import kotlin.experimental.and

/**
 *  @copyright Copyright (c) 1998-2014 Tencent Inc.
 *
 * 计算公众平台的消息签名接口.
 * https://developers.weixin.qq.com/doc/oplatform/Third-party_Platforms/Message_Encryption/Technical_Plan.html
 */

internal object SHA1 {
    /**
     * 用SHA1算法生成安全签名
     * @param token 票据
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param encrypt 加密的消息密文，此处是微信服务器推送过来的加密字符串，即标签<Encrypt>中的内容
     * @return 安全签名
     * @throws AesException
     */
    fun getSHA1(
        token: String,
        timestamp: String,
        nonce: String,
        encrypt: String
    ): String {
        return try {
            val array = arrayOf(token, timestamp, nonce, encrypt)
            Arrays.sort(array) // 字符串排序

            val sb = StringBuffer()
            for (i in 0..3) {
                sb.append(array[i])
            }

            val sha1 = MessageDigest.getInstance("SHA-1")
            sha1.update(sb.toString().toByteArray())
            val digest = sha1.digest()

            val hexstr = StringBuffer()
            var shaHex = ""
            for (i in digest.indices) {
                shaHex = Integer.toHexString((digest[i] and 0xFF.toByte()).toInt())
                if (shaHex.length < 2) {
                    hexstr.append(0)
                }
                hexstr.append(shaHex)
            }
            hexstr.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw AesException(AesException.ComputeSignatureError)
        }
    }
}
