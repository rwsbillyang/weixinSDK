package com.github.rwsbillyang.wxSDK.common.msg

import com.github.rwsbillyang.wxSDK.common.aes.AesException
import com.github.rwsbillyang.wxSDK.common.aes.WXBizMsgCrypt
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory


abstract class WxMsgHub(private val wxBizMsgCrypt: WXBizMsgCrypt?)
{
    companion object{
        val log: Logger = LoggerFactory.getLogger("WxMsgHub")
    }
    fun handleXmlMsg(
        postXmlMsg: String,
        msgSignature: String?,
        timeStamp: String?,
        nonce: String?,
        encryptType: String?
    ): String?
    {
        log.debug("post data: $postXmlMsg")
        try {
            val xmlText = if(wxBizMsgCrypt == null) {
                postXmlMsg
            }else{
                if (StringUtils.isAllBlank(msgSignature, timeStamp, nonce)) {
                    log.warn("some one of is blank: msgSignature={},timeStamp={},nonce{}", msgSignature, timeStamp, nonce)
                    return null
                }
                wxBizMsgCrypt.decryptWxMsg(msgSignature!!, timeStamp!!, nonce!!, postXmlMsg, encryptType)
            }
            log.debug("after decrypt: $xmlText")

            val reader: XMLEventReader = XMLInputFactory.newInstance().createXMLEventReader(xmlText.byteInputStream())

            val base = BaseInfo.fromXml(xmlText,reader)

            val reMsg = when(base.msgType){
                BaseInfo.EVENT -> dispatchEvent(reader,base)
                else -> dispatchMsg(reader,base)
            }

            reader.close()

            return reMsg?.toXml()?.let { wxBizMsgCrypt?.encryptMsg(it)?.first }

        }catch (e: AesException){
            e.printStackTrace()
            log.warn("${e.message}")
        }catch (e: Exception){
            e.printStackTrace()
            log.warn("${e.message}")
        }

        return null
    }

    abstract fun dispatchMsg(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?

    abstract fun dispatchEvent(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?
}