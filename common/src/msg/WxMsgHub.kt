package com.github.rwsbillyang.wxSDK.msg

import com.github.rwsbillyang.wxSDK.security.AesException
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.security.XmlUtil
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

/**
 * xml消息事件通知的解包、解析、分发处理,xml消息为腾讯post过来的body，重点处理Encrypt中的内容
 * */
abstract class WxMsgHub(private val wxBizMsgCrypt: WXBizMsgCrypt?)
{
    companion object{
        val log: Logger = LoggerFactory.getLogger("WxMsgHub")
    }
    /**
     * @param appId 公众号appId或企业微信中的corpId
     * */
    fun handleXmlMsg(
        appId: String?, //公众号通过路径传入appId，企业微信为corpId，通过xml解析得到
        body: String,
        msgSignature: String?,
        timeStamp: String?,
        nonce: String?,
        encryptType: String?
    ): String?
    {
        log.debug("Receive post data: $body")
        try {
            var map: Map<String, String?> = mapOf()
            var corpId: String? = null
            val decryptedXmlText = if(wxBizMsgCrypt == null) {
                body
            }else{
                if (StringUtils.isAllBlank(msgSignature, timeStamp, nonce)) {
                    log.warn("some one of is blank: msgSignature={},timeStamp={},nonce{}", msgSignature, timeStamp, nonce)
                    return null
                }
                map = XmlUtil.extract(body)//body转换成的map数据结构
                // 提取密文
                val encryptText = map["Encrypt"]?:""//提取body中的Encrypt字段
                corpId = appId?:map["ToUserName"]
                if(corpId == null){
                   log.error("no corpId")
                   return null
                }else{
                    wxBizMsgCrypt.decryptWxMsg(corpId, msgSignature!!, timeStamp!!, nonce!!, encryptText, encryptType)
                }
            }
            log.debug("after decrypt: $decryptedXmlText")


           val reMsg = parseXml(decryptedXmlText, map)

            //val re = reMsg?.toXml()?.let { wxBizMsgCrypt?.encryptMsg(it)?.first }
            //log.debug("Reply: $re")
            return reMsg?.toXml()?.let { wxBizMsgCrypt?.encryptMsg(corpId!!, it)?.first }

        }catch (e: AesException){
            e.printStackTrace()
            log.warn("${e.message}")
        }catch (e: Exception){
            e.printStackTrace()
            log.warn("${e.message}")
        }

        return null
    }

    /**
     * @param decryptedXmlText 由外层xml中的Encrypt字段解析出来的新xml字段
     * @param outerMap 外层xml得到的map数据,即body直接转换成的map数据
     * */
    protected open fun parseXml(decryptedXmlText: String, outerMap: Map<String, String?>): ReBaseMSg?{
        val reader: XMLEventReader = XMLInputFactory.newInstance().createXMLEventReader(decryptedXmlText.byteInputStream())

        val base = BaseInfo.fromXml(decryptedXmlText, reader)

        val reMsg = when(base.msgType){
            MsgType.EVENT -> dispatchEvent(reader,base)
            else -> dispatchMsg(reader,base)
        }
        reader.close()
        return reMsg
    }



    abstract fun dispatchMsg(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?

    abstract fun dispatchEvent(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?
}