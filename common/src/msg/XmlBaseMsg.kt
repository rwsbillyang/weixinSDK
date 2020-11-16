package com.github.rwsbillyang.wxSDK.msg


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLStreamException



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
open class BaseInfo(
    val toUserName: String?,
    val fromUserName: String?,
    val createTime: Long?,
    val msgType: String?
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger("BaseInfo")
        /**
         * 严格按照顺序来进行解析xml，当遇到"MsgType"时结束解析，从而进行对应的子消息的继续解析
         * Limit： 当MsgType不是ToUserName、FromUserName、CreateTime、CreateTime在之后时会导致这些字段未解析
         *
         * @return 返回XMLEventReader用于进一步解析xml
         * */
        fun fromXml(xml: String,reader: XMLEventReader): BaseInfo
        {
            //val reader: XMLEventReader = XMLInputFactory.newInstance().createXMLEventReader(xml.byteInputStream())
            var toUserName: String? = null
            var fromUserName: String? = null
            var createTime: Long? = null
            var msgType: String? = null

            var count = 0 //解析出的节点计数，用于报警
            while (reader.hasNext() && count < 5) {
                val event = reader.nextEvent()
                if (event.isStartElement) {
                    when(event.asStartElement().name.toString()){
                        "xml" -> count++
                        "ToUserName" -> {
                            count++
                            toUserName = reader.elementText
                        }
                        "FromUserName" -> {
                            count++
                            fromUserName = reader.elementText
                        }
                        "CreateTime"-> {
                            count++
                            createTime = reader.elementText?.toLong()
                        }
                        "MsgType" -> {
                            count++
                            msgType = reader.elementText
                            if(count < 5){
                               log.warn("WARN: Maybe lack of value ToUserName,FromUserName,CreateTime before MsgType!!!  xml=$xml")
                            }
                        }
                    }
                }
            }

            return  BaseInfo(toUserName,fromUserName,createTime, msgType)
        }
    }
}




/**
 * 微信推送过来的消息 基类
 *
 * 用组合代替继承实现
 *
 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Receiving_standard_messages.html
 * https://work.weixin.qq.com/api/doc/90000/90135/90239
 *
 * @param base 通过fromXml解析出来BaseInfo，用组合代替继承实现
 * @property msgId msgId
 * */
open class WxBaseMsg(val base: BaseInfo) {
    var msgId: Long? = null

    open fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "MsgId" -> {
                        msgId = reader.elementText?.toLong()
                        break
                    }
                }
            }
        }
    }
}


/**
 * 事件基类
 * */
open class WxBaseEvent(val base: BaseInfo)  {
    var event: String? = null

    open fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "Event" -> {
                        event = reader.elementText
                        break
                    }
                }
            }
        }
    }
}



/**
 * 被动回复消息基类
 * */
abstract class ReBaseMSg(
    toUserName: String?,
    fromUserName: String?,
    createTime: Long,
    msgType: String
) : BaseInfo(toUserName, fromUserName, createTime, msgType) {
    open fun toXml(): String {
        val builder = XmlMsgBuilder("<xml>\n")
        builder.addData("ToUserName", toUserName)
        builder.addData("FromUserName", fromUserName)
        builder.addTag("CreateTime", createTime.toString().substring(0, 10))
        builder.addData("MsgType", msgType)
        addMsgContent(builder)
        builder.append("</xml>")
        return builder.toString()
    }

    override fun toString() = toXml()

    protected abstract fun addMsgContent(builderXml: XmlMsgBuilder)
}



/**
 * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Custom_Menu_Push_Events.html#3
 * https://work.weixin.qq.com/api/doc/90000/90135/90240#%E5%BC%B9%E5%87%BA%E7%B3%BB%E7%BB%9F%E6%8B%8D%E7%85%A7%E5%8F%91%E5%9B%BE%E7%9A%84%E4%BA%8B%E4%BB%B6%E6%8E%A8%E9%80%81
 * <item><PicMd5Sum><![CDATA[1b5f7c23b5bf75682a53e7b6d163e185]]></PicMd5Sum></item>
 * */
class Pic(val md5: String?)

/**
 * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Custom_Menu_Push_Events.html#3
 * https://work.weixin.qq.com/api/doc/90000/90135/90240#%E5%BC%B9%E5%87%BA%E7%B3%BB%E7%BB%9F%E6%8B%8D%E7%85%A7%E5%8F%91%E5%9B%BE%E7%9A%84%E4%BA%8B%E4%BB%B6%E6%8E%A8%E9%80%81
 *
<SendPicsInfo>
<Count>1</Count>
<PicList><item><PicMd5Sum><![CDATA[1b5f7c23b5bf75682a53e7b6d163e185]]></PicMd5Sum></item></PicList>
</SendPicsInfo>

 * */
class SendPicsInfo(val count: Int?, val picList: List<Pic>?){

    companion object{
        /**
         * Event为pic_sysphoto, pic_photo_or_album, pic_weixin时触发
         *
         *
        <SendPicsInfo>
        <Count>1</Count>
        <PicList><item><PicMd5Sum><![CDATA[5a75aaca956d97be686719218f275c6b]]></PicMd5Sum></item></PicList>
        </SendPicsInfo>
         *
         * @param reader reader
         * @return 读取结果
         * @throws XMLStreamException XML解析异常
         */
        @Throws(XMLStreamException::class)
        fun fromXml(reader: XMLEventReader): SendPicsInfo {
            var count:Int? = null
            val picList = mutableListOf<Pic>()

            loop@ while (reader.hasNext()) {
                val e = reader.nextEvent()
                if (e.isStartElement) {
                    val tagName = e.asStartElement().name.toString()
                    if ("Count" == tagName) {
                        count = reader.elementText?.toInt()
                    } else if ("PicList" == tagName) {
                        while (reader.hasNext()) {
                            val event1 = reader.nextEvent()
                            if (event1.isStartElement && "PicMd5Sum" == event1.asStartElement().name.toString())
                            {
                                picList.add(Pic(reader.elementText))
                            } else if (event1.isEndElement && "PicList" == event1.asEndElement().name.toString()) {
                                break@loop
                            }
                        }
                    }
                }
            }
            return SendPicsInfo(count, picList)
        }
    }

}