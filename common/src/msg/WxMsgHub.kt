package com.github.rwsbillyang.wxSDK.msg

import com.github.rwsbillyang.wxSDK.security.AesException
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.util.XmlUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Element


/**
 * xml消息事件通知的解包、解析、分发处理,xml消息为腾讯post过来的body，重点处理Encrypt中的内容
 * */
abstract class WxMsgHub(val wxBizMsgCrypt: WXBizMsgCrypt?)
{
    private val log: Logger = LoggerFactory.getLogger("WxMsgHub")
    /**
     * @param appId 公众号appId或企业微信中的corpId
     * */
    fun handleXmlMsg(
        appId: String, //公众号通过路径传入appId，企业微信为corpId or suiteId
        agentId: String?, //企业微信内建应用
        body: String,
        msgSignature: String,
        timestamp: String,
        nonce: String,
        encryptType: String?
    ): String?
    {
        try {
            var agentIdInXml: String? = null
            val decryptedXmlText = if(wxBizMsgCrypt == null) {
                log.info("wxBizMsgCrypt is null, appId=$appId, agentId=$agentId")
                body
            }else{
                //  内建应用的body
                //<xml>
                // <ToUserName></ToUserName>
                //  <Encrypt><![CDATA[7I...bKjU=]]></Encrypt>
                //  <MsgSignature>a198da2128097708a2c6bd04f97e4eb71a11d10e</MsgSignature>
                //  <TimeStamp>1599291766096</TimeStamp>
                //  <Nonce>YU3g9W</Nonce>
                // </xml>
                //<xml>
                // <ToUserName><![CDATA[wwfc2fead39b1e60dd]]></ToUserName>
                // <Encrypt><![CDATA[ksULeHpUG0imG/CKibJVeTn3P35Yf0JcShljm2SkXX6KffMLZHSpwnKwoQJ7ixjh9DM0kkq8atMJf42sG8PDiIubteW6idDu9cVryEmF0s2EkZ/VYtKTp/Ht1NmdoVU8j8AWUyhZJYbP/Vpv/ycOXylWAhr4PRKpxYjKp3pVzf9tl4e/jweQH2plmuWUX1AD+DBw+NF5aYV3M8HJR2CE7dMTnahSutXfoYn3sLbvxh3dtcIv2pWa4j9fBKrL9EuwuRtV6RGgAMEe98Z6Xr2/ICfIqH+3bmxtYKEOmmr0Sd3GQJMP6prihJE5RBaATcWL3bC5snJUYnySe07uXA/zutsjYpAulDWSPbbIACj3is0uLLzR+LZ18gnqUMGnHRKEJfDQ+MVSUd35o7ctMvgZQFgvoZLyyMtDyU2hsE6oMIysDBBRuozpUokxTZVulWdXchh0wuYtnJnkmg5Fp1ASHhRTeFGGQyrxOT/CUAjBcsagjIDRWM7tZ3jo5+cvz54qvWgSVQMF0q7S74z565LFzt80RRevTRgB61SMzjFO/psBuVehmo+HbN1nDSLS6W63di+eTUxjQOY8Rpz8de8UtQ==]]></Encrypt>
                // <AgentID><![CDATA[3010151]]></AgentID></xml>
                val map = XmlUtil.parseXmlByDom(body, setOf("ToUserName","Encrypt","AgentID"))//body转换成的map数据结构
              //  val appId2 = map["ToUserName"] //公众号则为其微信号，g_开头
                val encryptText = map["Encrypt"]?:""//提取body中的Encrypt字段
                agentIdInXml = map["AgentID"] //企业自建应用非空，第三方应用和公众号为空
                wxBizMsgCrypt.decryptWxMsg(appId, msgSignature, timestamp, nonce, encryptText, encryptType)
            }

            //log.info("after decrypt: $decryptedXmlText")

            //当为sysAgent时，如wxkf，路径中为WxKeFu，但xml中为类似于3010151这样的值
            // 而ctx配置则使用的是WxKeFu，应优先使用path中的路径参数
           val reMsg = handleMsgOrEvent(appId, agentId?:agentIdInXml, decryptedXmlText)

            return reMsg?.toXml()?.let { wxBizMsgCrypt?.encryptMsg(appId, it)?.first }
        }catch (e: AesException){
            log.warn("AesException: ${e.localizedMessage}, agentId=$agentId,  appId=$appId, msgSignature=$msgSignature,  timestamp=$timestamp, nonce=$nonce, body=$body")
        }catch (e: Exception){
            log.warn("something wrong, exception: ${e.localizedMessage}")
            e.printStackTrace()
        }

        return null
    }

    /**
     * @param decryptedXmlText 由外层xml中的Encrypt字段解析出来的新xml字段
     * @param outerMap 外层xml得到的map数据,即body直接转换成的map数据
     * */
    protected open fun handleMsgOrEvent(
        appId: String, agentId: String?,
        decryptedXmlText: String
    ): ReBaseMSg?{
        try {
            val root = XmlUtil.getXmlRootDom(decryptedXmlText)
            val reMsg = when(val msgType = root.get("MsgType")){
                MsgType.EVENT -> {
                    val event = root.get("Event")
                    dispatchEvent(appId, agentId, decryptedXmlText, root, event)
                }
                else -> dispatchMsg(appId, agentId, decryptedXmlText, root, msgType)
            }

            return reMsg
        }catch (e: Exception){
            e.printStackTrace()
            log.warn("handleMsgOrEvent fail: ${e.message}")
        }
        return null
    }



    /**
     * @param appId 优先使用xml中的ToUserName，其次才使用回调路径中的path参数，
     * 若ToUserName总是为appId，则路径中可去掉该参数
     * @param agentId 企业微信适用，优先使用xml中的
     * @param xml 解密后的xml文本，handler中可查看原始文本
     * @param rootDom 将解密后的xml文本转换成xml dom，handler可通过该dom进一步解析
     * @param msgType 已解析除的msgType
     * */
    abstract fun dispatchMsg(appId: String, agentId: String?, xml: String, rootDom: Element, msgType: String?): ReBaseMSg?

    /**
     * 当 msgType == "event"时进行分发
     * @param appId 优先使用xml中的ToUserName，其次才使用回调路径中的path参数，
     * 若ToUserName总是为appId，则路径中可去掉该参数
     * @param agentId 企业微信适用，优先使用xml中的
     * @param xml @param xml 解密后的xml文本，handler中可查看原始文本
     * @param rootDom 将解密后的xml文本转换成xml dom，handler可通过该dom进一步解析
     *
     * */
    abstract fun dispatchEvent(appId: String, agentId: String?, xml: String, rootDom: Element, eventType: String?): ReBaseMSg?
}