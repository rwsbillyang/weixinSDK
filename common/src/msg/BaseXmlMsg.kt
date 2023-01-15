package com.github.rwsbillyang.wxSDK.msg


import nl.adaptivity.xmlutil.dom.get
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.stream.XMLStreamException

//方便使用
fun Element.get(tag: String) = getElementsByTagName(tag).item(0)?.textContent
/**
 * 接收的消息及被动回复消息的总基类
 * @param toUserName 开发者微信号
 * 用户发送给公众号/小程序的消息（由第三方平台代收）为公众号/小程序原始 ID（可通过获取授权方的帐号基本信息接口来获得）。
 * 微信服务器发送给第三方平台自身的通知或事件推送（如取消授权通知，component_verify_ticket 推送等）。此时，消息 XML体中没有 ToUserName 字段，而是 AppId 字段，即第三方平台的 AppId。
 *
 * @param fromUserName 发送者的openId
 * @param createTime 是微信公众平台记录粉丝发送该消息的具体时间
 * @param msgType 消息类型
 * */
open class WxXmlMsgBase(val xml: String, rootDom: Element){
    fun get(rootDom: Element, tag: String) = rootDom.getElementsByTagName(tag).item(0)?.textContent
    fun getChild(rootDom: Element, tag: String): Node = rootDom.getElementsByTagName(tag).item(0)
    fun getChildren(rootDom: Element, tag: String): NodeList = rootDom.getElementsByTagName(tag)

    val toUserName = get(rootDom,"ToUserName")
    val fromUserName = get(rootDom,"FromUserName")
    val createTime = get(rootDom,"CreateTime")?.toLong()
    val msgType = get(rootDom,"MsgType")
}

/**
 * 微信推送过来的消息 基类
 * @property msgId msgId
 * */
open class WxXmlMsg(xml: String, rootDom: Element): WxXmlMsgBase(xml, rootDom){
    val msgId = get(rootDom, "MsgId")?.toLong()
}
open class WxXmlEvent(xml: String, rootDom: Element): WxXmlMsgBase(xml, rootDom){
    val event = get(rootDom, "Event")
}




/**
 * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Custom_Menu_Push_Events.html#3
 * https://work.weixin.qq.com/api/doc/90000/90135/90240#%E5%BC%B9%E5%87%BA%E7%B3%BB%E7%BB%9F%E6%8B%8D%E7%85%A7%E5%8F%91%E5%9B%BE%E7%9A%84%E4%BA%8B%E4%BB%B6%E6%8E%A8%E9%80%81
 * */
class SendPicsInfo(val count: Int?, val picList: List<String>?){

    companion object{
        /*
         * Event为pic_sysphoto, pic_photo_or_album, pic_weixin时触发

        <SendPicsInfo>
        <Count>1</Count>
        <PicList><item><PicMd5Sum><![CDATA[5a75aaca956d97be686719218f275c6b]]></PicMd5Sum></item></PicList>
        </SendPicsInfo>
         */
        @Throws(XMLStreamException::class)
        fun fromXml(node: Node): SendPicsInfo? {
            if (node.nodeType == Node.ELEMENT_NODE) {
                val e = node as Element
                val count = e.getElementsByTagName("Count").item(0)?.textContent?.toInt()
                val nList = e.getElementsByTagName("PicMd5Sum")
                val picList = mutableListOf<String>()
                (0 until nList.getLength()).forEach {
                    nList.get(it)?.textContent?.let { picList.add(it) }
                }
                return SendPicsInfo(count, picList)
            }
            return null
        }
    }

}

//open class BaseInfo(
//    val toUserName: String?,
//    val fromUserName: String?,
//    val createTime: Long?,
//    val msgType: String?
//) {
//    companion object {
//        private val log: Logger = LoggerFactory.getLogger("BaseInfo")
//        /**
//         * 严格按照顺序来进行解析xml，当遇到"MsgType"时结束解析，从而进行对应的子消息的继续解析
//         * Limit： 当MsgType不是ToUserName、FromUserName、CreateTime、CreateTime在之后时会导致这些字段未解析
//         *
//         * @return 返回XMLEventReader用于进一步解析xml
//         * */
//        fun fromXml(xml: String,reader: XMLEventReader): BaseInfo
//        {
//            //val reader: XMLEventReader = XMLInputFactory.newInstance().createXMLEventReader(xml.byteInputStream())
//            var toUserName: String? = null
//            var fromUserName: String? = null
//            var createTime: Long? = null
//            var msgType: String? = null
//
//            var count = 0 //解析出的节点计数，用于报警
//            while (reader.hasNext() && count < 5) {
//                val event = reader.nextEvent()
//                if (event.isStartElement) {
//                    when(event.asStartElement().name.toString()){
//                        "xml" -> count++
//                        "ToUserName" -> {
//                            count++
//                            toUserName = reader.elementText
//                        }
//                        "FromUserName" -> {
//                            count++
//                            fromUserName = reader.elementText
//                        }
//                        "CreateTime"-> {
//                            count++
//                            createTime = reader.elementText?.toLong()
//                        }
//                        "MsgType" -> {
//                            count++
//                            msgType = reader.elementText
//                            if(count < 5){
//                               log.warn("WARN: Maybe lack of value ToUserName,FromUserName,CreateTime before MsgType!!!  xml=$xml")
//                            }
//                        }
//                    }
//                }
//            }
//
//            return  BaseInfo(toUserName,fromUserName,createTime, msgType)
//        }
//    }
//}



