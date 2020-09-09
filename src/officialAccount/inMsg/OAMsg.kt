package com.github.rwsbillyang.wxSDK.officialAccount.inMsg

import com.github.rwsbillyang.wxSDK.common.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.common.msg.WxBaseMsg
import javax.xml.stream.XMLEventReader


/**
 * 文本消息 微信服务器推送过来的
 * @property content 消息内容
 * */
open class OATextMsg(base: BaseInfo): WxBaseMsg(base)
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
 * 文本消息 微信服务器推送过来的
 * 当客服发送菜单消息进行问卷调查之后，客户可以选择，之后会将选项推送回服务器，依据bizmsgmenuid是否有内容来判断是否是菜单点击消息
 *
 *
 * @property menuId 点击的菜单ID 参见客服消息中的菜单消息中的菜单，用户调查时用户点击的菜单id
 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Service_Center_messages.html#7
 * */
class OACustomerClickMenuMsg(base: BaseInfo): OATextMsg(base)
{
    var menuId: String? = null
    override fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "bizmsgmenuid" -> {
                        menuId = reader.elementText
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
class OAImgMSg(base: BaseInfo): WxBaseMsg(base)
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
 * @property mediaId 语音消息媒体id，可以调用获取临时素材接口拉取数据。
 * @property format 语音格式，如amr，speex等
 * @property recognition 语音识别结果，UTF8编码 开通语音识别后，用户每次发送语音给公众号时，
 * 微信会在推送的语音消息XML数据包中，增加一个Recognition字段（注：由于客户端缓存，
 * 开发者开启或者关闭语音识别功能，对新关注者立刻生效，对已关注用户需要24小时生效。
 * 开发者可以重新关注此帐号进行测试）
 * */
class OAVoiceMsg(base: BaseInfo): WxBaseMsg(base)
{
    var mediaId: String?= null
    var format: String? = null
    var recognition: String?= null

    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 4) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "MediaId" -> {mediaId = reader.elementText; count++}
                    "Format" -> {format = reader.elementText; count++}
                    "Recognition" -> {recognition = reader.elementText; count++}
                    "MsgId" -> { msgId = reader.elementText?.toLong(); count++}
                }
            }
        }
    }
}

/**
 * 视频消息
 * @property mediaId 视频消息媒体id，可以调用获取临时素材接口拉取数据
 * @property thumbMediaId 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据。
 * */
open class OAVideoMsg(base: BaseInfo): WxBaseMsg(base)
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
 * 小视频消息
 * @property thumbMediaId 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据。
 * */
class OAShortVideoMsg(base: BaseInfo): OAVideoMsg(base)

/**
 * 地理位置消息
 * @property locationX Location_X	地理位置纬度
 * @property locationY Location_Y	地理位置经度
 * @property scale 地图缩放大小
 * @property label 地理位置信息
 * */
class OALocationMsg(base: BaseInfo): WxBaseMsg(base)
{
    var locationX: Float? = null
    var locationY: Float? = null
    var scale: Int? = null
    var label: String? = null
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
 * */
class OALinkMsg(base: BaseInfo): WxBaseMsg(base)
{
    var title: String? = null
    var description: String? = null
    var url: String? = null
    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 3) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when(event.asStartElement().name.toString()){
                    "Title" -> {title = reader.elementText; count++}
                    "Description" -> {description = reader.elementText; count++}
                    "Url" -> {url = reader.elementText; count++}
                }
            }
        }
        super.read(reader)
    }
}