package com.github.rwsbillyang.wxSDK.security

import com.github.rwsbillyang.wxSDK.msg.XmlMsgBuilder
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory


/**
 *
 * 提取消息格式中的密文及生成回复消息格式的接口.
 */
object XmlUtil {
    /**
     * 提取出xml数据包中的加密消息
     * @param xmltext 待提取的xml字符串,形如：
     * <xml>
     *     <ToUserName></ToUserName>
     *     <Encrypt><![CDATA[7I...bKjU=]]></Encrypt>
     *      <MsgSignature>a198da2128097708a2c6bd04f97e4eb71a11d10e</MsgSignature>
     *      <TimeStamp>1599291766096</TimeStamp>
     *      <Nonce>YU3g9W</Nonce>
     * </xml>
     *
     * @return 提取出的加密消息字符串map
     * @throws AesException
     */
    @Throws(AesException::class)
    fun extract(xmltext: String): Map<String, String?> {
       val map = mutableMapOf<String, String?>()
        return try {
            val dbf = DocumentBuilderFactory.newInstance()

            // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all XML entity attacks are prevented
            // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)

            // If you can't completely disable DTDs, then at least do the following:
            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
            // JDK7+ - http://xml.org/sax/features/external-general-entities
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false)

            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
            // JDK7+ - http://xml.org/sax/features/external-parameter-entities
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)

            // Disable external DTDs as well
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
            dbf.isXIncludeAware = false
            dbf.isExpandEntityReferences = false
            // And, per Timothy Morgan: "If for some reason support for inline DOCTYPEs are a requirement, then
            // ensure the entity settings are disabled (as shown above) and beware that SSRF attacks
            // (http://cwe.mitre.org/data/definitions/918.html) and denial
            // of service attacks (such as billion laughs or decompression bombs via "jar:") are a risk."

            val db = dbf.newDocumentBuilder()
            val sr = StringReader(xmltext)
            val `is` = InputSource(sr)
            val document = db.parse(`is`)
            val root = document.documentElement

            for(tag in listOf("ToUserName","Encrypt","TimeStamp","Nonce"))
            {
                map[tag] = root.getElementsByTagName(tag)?.item(0)?.textContent
            }
            return map
        } catch (e: Exception) {
            println("Fail parse xml: $xmltext")
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
        nonce: String?,
        toUserName: String? = null,
        agentId: Int? = null
    ): String {
        val builder = XmlMsgBuilder("<xml>\n")
        if(!toUserName.isNullOrBlank()) builder.addData("ToUserName", toUserName)
        builder.addData("Encrypt", encrypt)
        builder.addTag("MsgSignature", signature)
        builder.addTag("TimeStamp", timestamp)
        builder.addTag("Nonce", nonce)
        if(agentId != null) builder.addTag("AgentID", agentId.toString())
        builder.append("</xml>")

        return builder.toString()
    }
}
