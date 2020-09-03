package com.github.rwsbillyang.wxSDK.common.msgSecurity

import com.github.rwsbillyang.wxSDK.common.msg.MsgBuilder
import com.github.rwsbillyang.wxSDK.common.msgSecurity.AesException
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory


/**
 *
 * 提取消息格式中的密文及生成回复消息格式的接口.
 */
internal object XMLUtil {
    /**
     * 提取出xml数据包中的加密消息
     * @param xmltext 待提取的xml字符串,形如：
     * <xml>
     *     <ToUserName></ToUserName>
     *     <Encrypt></Encrypt>
     * </xml>
     *
     * @return 提取出的加密消息字符串
     * @throws AesException
     */
    @Throws(AesException::class)
    fun extract(xmltext: String): Array<Any?> {
        val result = arrayOfNulls<Any?>(3)
        return try {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false)
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            dbf.isXIncludeAware = false
            dbf.isExpandEntityReferences = false
            val db = dbf.newDocumentBuilder()
            val sr = StringReader(xmltext)
            val `is` = InputSource(sr)
            val document = db.parse(`is`)
            val root = document.documentElement
            val nodelist1 = root.getElementsByTagName("Encrypt")
            val nodelist2 = root.getElementsByTagName("ToUserName")
            result[0] = 0
            result[1] = nodelist1.item(0).textContent
            result[2] = nodelist2.item(0).textContent
            result
        } catch (e: Exception) {
            e.printStackTrace()
            throw AesException(AesException.ParseXmlError)
        }
    }

    /**
     * 2.4 回包加密 生成xml消息
     * https://developers.weixin.qq.com/doc/oplatform/Third-party_Platforms/Message_Encryption/Message_encryption_and_decryption.html
     * <xml>
        <Encrypt>
        <![CDATA[LDF...8frNvA==]]> </Encrypt>
        <MsgSignature></MsgSignature>
        <TimeStamp>1411034505</TimeStamp>
        <Nonce></Nonce>
    </xml>
     * @param encrypt 加密后的消息密文
     * @param signature 安全签名
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @return 生成的xml字符串
     *
     */
    fun generateEncryptReMsg(
        encrypt: String?,
        signature: String?,
        timestamp: String?,
        nonce: String?
    ): String {
        val builder = MsgBuilder("<xml>\n")
        builder.addData("Encrypt", encrypt)
        builder.addTag("MsgSignature", signature)
        builder.addTag("TimeStamp", timestamp)
        builder.addTag("Nonce", nonce)

        builder.append("</xml>")

        return builder.toString()
    }
}
