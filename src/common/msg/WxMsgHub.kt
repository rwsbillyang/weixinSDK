package com.github.rwsbillyang.wxSDK.common.msg

import com.github.rwsbillyang.wxSDK.common.msgSecurity.AesException
import com.github.rwsbillyang.wxSDK.common.msgSecurity.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.officialAccount.OA
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory


abstract class WxMsgHub(aesKey: String?)
{
    companion object{
        val log: Logger = LoggerFactory.getLogger("WxMsgHub")
    }

    private var wxMsgCrypt: WXBizMsgCrypt? = null

    init {
        if(!aesKey.isNullOrBlank()){
            wxMsgCrypt = WXBizMsgCrypt(OA.token, aesKey, OA.appId)
        }
    }


    fun handleXmlMsg(
        postXmlMsg: String,
        msgSignature: String?,
        timeStamp: String?,
        nonce: String?,
        encryptType: String?
    ): String
    {
        if(wxMsgCrypt != null) {
            if (StringUtils.isAllBlank(msgSignature, timeStamp, nonce)) {
                log.warn("some one of is blank: msgSignature={},timeStamp={},nonce{}", msgSignature, timeStamp, nonce)
                return ""
            }
        }

        try {
            val xmlText = wxMsgCrypt?.decryptWxMsg(msgSignature!!, timeStamp!!, nonce!!, postXmlMsg, encryptType)?: postXmlMsg

            val reader: XMLEventReader = XMLInputFactory.newInstance().createXMLEventReader(xmlText.byteInputStream())

            val base = BaseMsg.fromXml(xmlText,reader)

            val reMsg = when(base.msgType){
                BaseMsg.EVENT -> dispatchEvent(reader,base)
                else -> dispatchMsg(reader,base)
            }

            reader.close()

            return if(reMsg == null ) "success" else{
                wxMsgCrypt?.encryptReMsg(reMsg.toXml()) ?: reMsg.toXml()
            }
        }catch (e: AesException){
            log.warn("${e.message}")
        }catch (e: Exception){
            log.warn("${e.message}")
        }

        return ""
    }

    abstract fun dispatchMsg(reader: XMLEventReader, base: BaseMsg): ReBaseMSg?

    abstract fun dispatchEvent(reader: XMLEventReader, base: BaseMsg): ReBaseMSg?
}