package com.github.rwsbillyang.wxSDK.work.msg

import com.github.rwsbillyang.wxSDK.common.msg.BaseInfo
import javax.xml.stream.XMLEventReader

/**
 * 企业微信中的接收到的消息基类
 *
 * 另起一个基类，不继承自公众中的消息基类，主要是让公众号和企业微信中的消息分离开来，不形成依赖关系
 *
 * @property msgId MsgId 消息id，64位整型
 * @property agentId 企业应用的id，整型。可在应用的设置页面查看
 * */
open class WorkBaseMsg(val base: BaseInfo) {
    var msgId: Long? = null
    var agentId: Int? = null
    open fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "MsgId" -> {
                        msgId = reader.elementText?.toLong()
                        count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText?.toInt()
                        count++
                    }
                }
            }
        }
    }
}

/**
 * 文本消息 微信服务器推送过来的
 * @property content 消息内容
 * */
class WorkTextMsg(base: BaseInfo): WorkBaseMsg(base)
{
    var content: String? = null
    override fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "Content" -> {
                        content = reader.elementText
                        break
                    }
                }
            }
        }
        super.read(reader)
    }
}


/**
 * 图片消息 微信服务器推送过来的
 * @property picUrl 图片链接（由系统生成）
 * @property mediaId 图片消息媒体id，可以调用获取临时素材接口拉取数据。
 * */
class WorkImgMSg(base: BaseInfo): WorkBaseMsg(base)
{
    var picUrl: String? = null
    var mediaId: String? = null
    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "PicUrl" -> {picUrl = reader.elementText; count++}
                    "MediaId" -> {mediaId = reader.elementText; count++}
                }
            }
        }
        super.read(reader)
    }
}


/**
 * 语音消息
 *
 * @property mediaId 语音媒体文件id，可以调用获取媒体文件接口拉取数据，仅三天内有效
 * @property format 语音格式，如amr，speex等
 *
 * */
class WorkVoiceMsg(base: BaseInfo): WorkBaseMsg(base)
{
    var format: String? = null
    var mediaId: String?= null
    //var recognition: String?= null

    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "MediaId" -> {mediaId = reader.elementText; count++}
                    "Format" -> {format = reader.elementText; count++}
                    //"Recognition" -> {recognition = reader.elementText; count++}
                    //"MsgId" -> { msgId = reader.elementText?.toLong(); count++}
                }
            }
        }
        super.read(reader)
    }
}

/**
 * 视频消息
 * @property mediaId 视频媒体文件id，可以调用获取媒体文件接口拉取数据，仅三天内有效
 * @property thumbMediaId ThumbMediaId	视频消息缩略图的媒体id，可以调用获取媒体文件接口拉取数据，仅三天内有效
 * */
class WorkVideoMsg(base: BaseInfo): WorkBaseMsg(base)
{
    var mediaId: String? = null
    var thumbMediaId: String? = null
    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "MediaId" -> {mediaId = reader.elementText; count++}
                    "ThumbMediaId" -> {thumbMediaId = reader.elementText; count++}
                }
            }
        }
        super.read(reader)
    }
}

/**
 * 位置消息
 * @property locationX Location_X	地理位置纬度
 * @property locationY Location_Y	地理位置经度
 * @property scale 地图缩放大小
 * @property label 地理位置信息
 * */
class WorkLocationMsg(base: BaseInfo): WorkBaseMsg(base)
{
    var locationX: Float? = null
    var locationY: Float? = null
    var scale: Int? = null
    var label: String? = null
    val appType = "wxwork"
    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 4) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "Location_X" -> {locationX = reader.elementText?.toFloat(); count++}
                    "Location_Y" -> {locationY = reader.elementText?.toFloat(); count++}
                    "Scale" -> {scale = reader.elementText?.toInt(); count++}
                    "Label" -> {label = reader.elementText; count++}
                }
            }
        }
        super.read(reader)
    }
}

/**
 * 链接消息
 * @property title Title	消息标题
 * @property description Description	消息描述
 * @property url Url	消息链接
 * @property picUrl PicUrl	封面缩略图的url
 * */
class WorkLinkMsg(base: BaseInfo): WorkBaseMsg(base)
{
    var title: String? = null
    var description: String? = null
    var url: String? = null
    var picUrl: String? = null
    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 4) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "Title" -> {title = reader.elementText; count++}
                    "Description" -> {description = reader.elementText; count++}
                    "Url" -> {url = reader.elementText; count++}
                    "PicUrl" -> {picUrl = reader.elementText; count++}
                }
            }
        }
        super.read(reader)
    }
}