package com.github.rwsbillyang.wxSDK.security




import java.nio.charset.Charset
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and


/**
 * 针对org.apache.commons.codec.binary.Base64，
 * 需要导入架包commons-codec-1.9（或commons-codec-1.8等其他版本）
 * 官方下载地址：http://commons.apache.org/proper/commons-codec/download_codec.cgi
 *
 * 提供接收和推送给公众平台消息的加解密接口(UTF8编码的字符串).
 *
 *  1. 第三方回复加密消息给公众平台
 *  1. 第三方收到公众平台发送的消息，验证消息的安全性，并对消息进行解密。
 *
 * 说明：异常java.security.InvalidKeyException:illegal Key Size的解决方案
 *
 *  1. 在官方网站下载JCE无限制权限策略文件（JDK7的下载地址：
 * http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
 *  1. 下载后解压，可以看到local_policy.jar和US_export_policy.jar以及readme.txt
 *  1. 如果安装了JRE，将两个jar文件放到%JRE_HOME%\lib\security目录下覆盖原来的文件
 *  1. 如果安装了JDK，将两个jar文件放到%JDK_HOME%\jre\lib\security目录下覆盖原来文件
 *
 * 加解密方案说明: https://work.weixin.qq.com/api/doc/90000/90139/90968
 *
 * @param token 公众平台上，开发者设置的token
 * @param encodingAesKey 公众平台上，开发者设置的EncodingAESKey
 */
class WXBizMsgCrypt(private val token: String, private val encodingAesKey: String) {
    companion object {
        val CHARSET: Charset = Charset.forName("utf-8")

        //private var base64 = Base64()
        private val base64Decoder = Base64.getUrlDecoder()
        private val base64Encoder = Base64.getUrlEncoder()

        // 随机生成num位字符串
        fun getRandomStr(num: Int = 16): String {
            val base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            val random = Random()
            val sb = StringBuffer()
            for (i in 0 until num) {
                val number = random.nextInt(base.length)
                sb.append(base[number])
            }
            return sb.toString()
        }
    }



    private val aesKey: ByteArray

    /**
     * @throws AesException 执行失败，请查看该异常的错误码和具体的错误信息
     */
    init {
        if (encodingAesKey.length != 43) {
            println("invalid encodingAesKey: $encodingAesKey")
            throw AesException(AesException.IllegalAesKey)
        }

        //FIXME: Apache Commons Codec 1.15 and 1.10 work fine, but v1.13 fail
        //V1.15: Base32/Base64/BCodec: Added strict decoding property to control handling of trailing bits.
        // Default lenient mode discards them without error. Strict mode raise an exception.
        // Fixes CODEC-280. see https://commons.apache.org/proper/commons-codec/changes-report.html#a1.15
        aesKey = Base64.getDecoder().decode("$encodingAesKey=") //base64.decodeBase64()
    }


    /**
     * 接入微信时，验证填写的GET请求URL
     * @param msgSignature 签名串，对应URL参数的msg_signature
     * @param timestamp 时间戳，对应URL参数的timestamp
     * @param nonce 随机串，对应URL参数的nonce
     * @param echoStr 随机串，对应URL参数的echostr
     *
     * @return 解密之后的echostr
     * @throws AesException 执行失败，请查看该异常的错误码和具体的错误信息
     */
    @Throws(AesException::class)
    fun verifyUrl(appId: String,
            msgSignature: String,
            timestamp: String,
            nonce: String,
            echoStr: String
    ): String {
        val signature = SHA1.getSHA1(token, timestamp, nonce, echoStr)
        if (signature != msgSignature) {
            println("verifyUrl fail: timestamp=$timestamp,nonce=$nonce,echoStr=$echoStr,  tx signature=$msgSignature, rx signature=$signature")
            throw AesException(AesException.ValidateSignatureError)
        }
        return decrypt(appId, echoStr)
    }

    /**
     * 解密从微信推送过来的消息（其中包含了签名验证），返回解密后的原文
     *
     * 检验消息的真实性，并且获取解密后的明文.
     *
     *  1. 利用收到的密文生成安全签名，进行签名验证
     *  2. 若验证通过，则提取xml中的加密消息
     *  3. 对消息进行解密
     * @param msgSignature 签名串，URL参数的msg_signature
     * @param timestamp 时间戳，URL参数的timestamp
     * @param nonce 随机串，URL参数的nonce
     * @param postData 密文，接收消息的URL中获取的整个post数据
     * @param encryptType aes，当前用不到
     * @return 解密后的原文
     *
     * @throws AesException 执行失败，请查看该异常的错误码和具体的错误信息
     *
     * https://developers.weixin.qq.com/doc/oplatform/Third-party_Platforms/Message_Encryption/Technical_Plan.html
     */
    @Throws(AesException::class)
    fun decryptWxMsg(appId: String,
            msgSignature: String,
            timestamp: String,
            nonce: String,
            postData: String,
            encryptType: String? = "aes",

    ): String {
        //生成自己的安全签名
        val signature = SHA1.getSHA1(token, timestamp, nonce, postData)

        if (signature != msgSignature) {
            println("verify signature fail: token=$token,timestamp=$timestamp,nonce=$nonce, postData=$postData, tx signature=$msgSignature, rx signature=$signature")
            throw AesException(AesException.ValidateSignatureError)
        }

        // 解密
        return decrypt(appId, postData)
    }

    /**
     * 生成xml格式字符串，包括Encrypt、ToUserName, AgentID, MsgSignature, Timestamp, Nonce等信息
     * 提供toUserName或agentId时用于模拟一条来自微信的推送消息
     *
     * @param replyMsg 回复消息，xml格式的字符串,形如：
     *   <xml>
     *   <ToUserName></ToUserName>
     *    <FromUserName></FromUserName>
     *    <CreateTime>1411034505</CreateTime>
     *    <MsgType></MsgType>
     *   <Content></Content>
     *   <FuncFlag>0</FuncFlag>
     *   </xml>
     *
     * @param timeStamp 时间戳，可以自己生成，也可以用URL参数的timestamp
     * @param nonce 随机串，可以自己生成，也可以用URL参数的nonce
     * @param toUserName 用于测试，模拟构造一个推送消息或事件
     * @param agentId 用于测试，模拟构造一个推送企业微信中的消息或事件
     *
     * @return 加密后的可以直接回复的密文，以及签名，签名用于模拟腾讯发送消息的测试
     * @throws AesException 执行失败，请查看该异常的错误码和具体的错误信息
     */
    @Throws(AesException::class)
    fun encryptMsg(appId: String, replyMsg: String,
                   timeStamp: String = System.currentTimeMillis().toString(),
                   nonce: String = getRandomStr(),
                   toUserName: String? = null,
                   agentId: Int? = null
    ): Pair<String, String> {
        val encrypt = encrypt(appId, replyMsg)
        val signature = SHA1.getSHA1(token, timeStamp, nonce, encrypt)

        // 生成回复发送的xml
        val xml = XmlUtil.generateEncryptReMsg(encrypt, signature, timeStamp, nonce, toUserName, agentId)
        return Pair(xml, signature)
    }


    // 生成4个字节的网络字节序
    private fun getNetworkBytesOrder(sourceNumber: Int): ByteArray {
        val orderBytes = ByteArray(4)
        orderBytes[3] = (sourceNumber and 0xFF).toByte()
        orderBytes[2] = (sourceNumber shr 8 and 0xFF).toByte()
        orderBytes[1] = (sourceNumber shr 16 and 0xFF).toByte()
        orderBytes[0] = (sourceNumber shr 24 and 0xFF).toByte()
        return orderBytes
    }

    // 还原4个字节的网络字节序
    private fun recoverNetworkBytesOrder(orderBytes: ByteArray): Int {
        var sourceNumber = 0
        for (i in 0..3) {
            sourceNumber = sourceNumber shl 8
            sourceNumber = sourceNumber or ((orderBytes[i] and 0xff.toByte()).toInt())
        }
        return sourceNumber
    }



    /**
     * 对明文进行加密.
     * @param appId 公众号appId或企业的corpId,suiteId
     * @param text 需要加密的明文
     * @return 加密后base64编码的字符串
     * @throws AesException aes加密失败
     */
    @Throws(AesException::class)
    fun encrypt(appId: String, text: String, randomStr: String = getRandomStr()): String {
        val byteCollector = ByteGroup()
        val randomStrBytes = randomStr.toByteArray(CHARSET)
        val textBytes = text.toByteArray(CHARSET)
        val networkBytesOrder = getNetworkBytesOrder(textBytes.size)
        val appidBytes = appId.toByteArray(CHARSET)

        // randomStr + networkBytesOrder + text + appid
        byteCollector.addBytes(randomStrBytes)
        byteCollector.addBytes(networkBytesOrder)
        byteCollector.addBytes(textBytes)
        byteCollector.addBytes(appidBytes)

        // ... + pad: 使用自定义的填充方式对明文进行补位填充
        val padBytes = PKCS7Encoder.encode(byteCollector.size())
        byteCollector.addBytes(padBytes)

        // 获得最终的字节流, 未加密
        val unencrypted: ByteArray = byteCollector.toBytes()
        return try {
            // 设置加密模式为AES的CBC模式
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            val keySpec = SecretKeySpec(aesKey, "AES")
            val iv = IvParameterSpec(aesKey, 0, 16)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv)

            // 加密
            val encrypted = cipher.doFinal(unencrypted)

            // 使用BASE64对加密后的字符串进行编码
            base64Encoder.encodeToString(encrypted)//base64.encodeToString(encrypted)
        } catch (e: Exception) {
            e.printStackTrace()
            throw AesException(AesException.EncryptAESError)
        }
    }

    /**
     * 对密文进行解密.
     *
     * @param text 需要解密的密文
     * @param appId 公众号appId，用于校验是否来自该公众号。，对于企业微信中的corpId，永远null，不进行校验
     * @return 解密得到的明文
     * @throws AesException aes解密失败
     */
    @Throws(AesException::class)
    private fun decrypt(appId: String, text: String?): String {
        val original: ByteArray
        original = try {
            // 设置解密模式为AES的CBC模式
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            val key_spec = SecretKeySpec(aesKey, "AES")
            val iv = IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16))
            cipher.init(Cipher.DECRYPT_MODE, key_spec, iv)

            // 使用BASE64对密文进行解码
            val encrypted = base64Decoder.decode(text) //Base64.decodeBase64(text)

            // 解密
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
           // e.printStackTrace()
            throw AesException(AesException.DecryptAESError)
        }

        val xmlContent: String
        val from_appid: String
        try {
            // 去除补位字符
            val bytes = PKCS7Encoder.decode(original)

            // 分离16位随机字符串,网络字节序和AppId
            val networkOrder = Arrays.copyOfRange(bytes, 16, 20)
            val xmlLength = recoverNetworkBytesOrder(networkOrder)
            xmlContent = String(Arrays.copyOfRange(bytes, 20, 20 + xmlLength), CHARSET)
            from_appid = String(
                    Arrays.copyOfRange(bytes, 20 + xmlLength, bytes.size),
                    CHARSET
            )
        } catch (e: Exception) {
            //e.printStackTrace()
            throw AesException(AesException.IllegalBuffer)
        }

        //企业微信中消息get请求时不校验，公众号则校验。公众号和企业微信所有的post的消息都校验
        if (from_appid != appId) {
            println("ValidateAppidError")
            throw AesException(AesException.ValidateAppidError)
        }
        return xmlContent
    }
}