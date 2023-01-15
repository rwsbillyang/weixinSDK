package com.github.rwsbillyang.wxSDK.work.inMsg


import com.github.rwsbillyang.wxSDK.msg.WxXmlMsgBase
import org.w3c.dom.Element


/**
 * 企业微信中的接收到的消息基类
 *
 * 另起一个基类，不继承自公众中的消息基类，主要是让公众号和企业微信中的消息分离开来，不形成依赖关系
 *
 * @property msgId MsgId 消息id，64位整型
 * @property agentId 企业应用的id，整型。可在应用的设置页面查看
 * */
open class WxWorkBaseMsg(xml: String, rootDom: Element): WxXmlMsgBase(xml, rootDom){
    val msgId = get(rootDom, "MsgId")
    val agentId = get(rootDom, "AgentID")
}

/**
 * 文本消息 微信服务器推送过来的
 * @property content 消息内容
 * */
class WorkTextMsg(xml: String, rootDom: Element): WxWorkBaseMsg(xml, rootDom){
    val content = get(rootDom, "Content")
}


/**
 * 图片消息 微信服务器推送过来的
 * @property picUrl 图片链接（由系统生成）
 * @property mediaId 图片消息媒体id，可以调用获取临时素材接口拉取数据。
 * */
class WorkImgMSg(xml: String, rootDom: Element): WxWorkBaseMsg(xml, rootDom){
    val picUrl = get(rootDom, "PicUrl")
    val mediaId = get(rootDom, "MediaId")
}


/**
 * 语音消息
 *
 * @property mediaId 语音媒体文件id，可以调用获取媒体文件接口拉取数据，仅三天内有效
 * @property format 语音格式，如amr，speex等
 *
 * */
class WorkVoiceMsg(xml: String, rootDom: Element): WxWorkBaseMsg(xml, rootDom){
    val format = get(rootDom, "Format")
    val mediaId = get(rootDom, "MediaId")
    //val recognition: String?= null
}

/**
 * 视频消息
 * @property mediaId 视频媒体文件id，可以调用获取媒体文件接口拉取数据，仅三天内有效
 * @property thumbMediaId ThumbMediaId	视频消息缩略图的媒体id，可以调用获取媒体文件接口拉取数据，仅三天内有效
 * */
class WorkVideoMsg(xml: String, rootDom: Element): WxWorkBaseMsg(xml, rootDom){
    val mediaId = get(rootDom, "MediaId")
    val thumbMediaId = get(rootDom, "ThumbMediaId")
}

/**
 * 位置消息
 * @property locationX Location_X	地理位置纬度
 * @property locationY Location_Y	地理位置经度
 * @property scale 地图缩放大小
 * @property label 地理位置信息
 * */
class WorkLocationMsg(xml: String, rootDom: Element): WxWorkBaseMsg(xml, rootDom){
    val locationX = get(rootDom, "Location_X")?.toFloat()
    val locationY = get(rootDom, "Location_Y")?.toFloat()
    val scale = get(rootDom, "Scale")?.toInt()
    val label = get(rootDom, "Label")
    val appType = "wxwork"
}

/**
 * 链接消息
 * @property title Title	消息标题
 * @property description Description	消息描述
 * @property url Url	消息链接
 * @property picUrl PicUrl	封面缩略图的url
 * */
class WorkLinkMsg(xml: String, rootDom: Element): WxWorkBaseMsg(xml, rootDom){
    val title = get(rootDom, "Title")
    val description = get(rootDom, "Description")
    val url = get(rootDom, "Url")
    val picUrl = get(rootDom, "PicUrl")
}