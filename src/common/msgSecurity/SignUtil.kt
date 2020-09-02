package com.github.rwsbillyang.wxSDK.common.msgSecurity

import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.experimental.and


object SignUtil {
    private val log: Logger = LoggerFactory.getLogger("AbstractApiConfig")
    private val digit = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )


    /**
     * 认证微信，可以参见微信开发者文档
     * 加密/校验流程如下：
     * 1）将token、timestamp、nonce三个参数进行字典序排序
     * 2）将三个参数字符串拼接成一个字符串进行sha1加密
     * 3）开发者获得加密后的字符串可与signature对比，相同表示该请求来源于微信
     *
     * @param token     我们自己设定的token值
     * @param signature 微信传来的变量
     * @param timestamp 微信传来的变量
     * @param nonce     微信传来的变量
     * @return 是否合法
     */
    fun checkSignature(
        token: String, signature: String,
        timestamp: String, nonce: String
    ): Boolean {
        val arr = arrayOf(token, timestamp, nonce)
        Arrays.sort(arr)
        val content = StringBuffer()
        for (anArr in arr) {
            content.append(anArr)
        }

        try {
            val sha1 = MessageDigest.getInstance("SHA-1")
            val digest = sha1.digest(content.toString().toByteArray(charset("UTF-8")))
            val signature2 = byteToStr(digest)

            val signature3 = DigestUtils.sha1Hex(content.toString())

            log.info("signature3=$signature3, signature2=$signature2, signature=$signature")

            return if(signature3 == signature) true
            else{
                log.warn("fail to check weixin signature:token=$token,timestamp=$timestamp,nonce=$nonce, signature=$signature,our signature2=$signature2,original content=$content")
                false
            }

        } catch (e: NoSuchAlgorithmException) {
            log.error("NoSuchAlgorithmException:{}", e.message)
        } catch (e: UnsupportedEncodingException) {
            log.error("UnsupportedEncodingException:{}", e.message)
        }


        return false
    }


    /**
     * 计算 wx.config() 中需要使用的签名。每个页面都需要进行计算
     *
     * @param jsApiTicket 微信 js-sdk提供的ticket
     * @param nonceStr    随机字符串
     * @param timestamp   时间戳
     * @param url         当前网页的URL，不包含#及其后面部分
     * @return 合法的签名
     * @throws Exception 获取签名异常
     */
    @Throws(Exception::class)
    fun jsApiSignature(
        jsApiTicket: String,
        nonceStr: String,
        timestamp: Long,
        url: String
    ): String {
        val paramMap: MutableMap<String, String> =
            TreeMap()
        paramMap["jsapi_ticket"] = jsApiTicket
        paramMap["noncestr"] = nonceStr
        paramMap["timestamp"] = timestamp.toString()
        paramMap["url"] = url
        val sb = StringBuffer()
        for (entry in paramMap.entries) {
            sb.append("&").append(entry.toString())
        }

        return DigestUtils.sha1Hex(sb.substring(1))
    }

    /**
     * 将byte数组变为16进制对应的字符串
     *
     * @param byteArray byte数组
     * @return 转换结果
     */
    private fun byteToStr(byteArray: ByteArray): String? {
        val len = byteArray.size
        val strDigest = StringBuffer(len * 2)
        for (aByteArray in byteArray) {
            strDigest.append(
                byteToHexStr(
                    aByteArray
                )
            )
        }
        return strDigest.toString()
    }

    private fun byteToHexStr(mByte: Byte): String? {
        val tempArr = CharArray(2)
        tempArr[0] = digit[(mByte.toInt().ushr(4)) and 0X0F]
        tempArr[1] = digit[(mByte and 0X0F).toInt()]
        return String(tempArr)
    }
}