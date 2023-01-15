package com.github.rwsbillyang.wxSDK.officialAccount.inMsg


import com.github.rwsbillyang.wxSDK.msg.WxXmlMsg
import org.w3c.dom.Element

/**
 * 文本消息 微信服务器推送过来的
 * @property content 消息内容
 * */
open class OATextMsg(xml: String, rootDom: Element): WxXmlMsg(xml, rootDom)
{
    val content = get(rootDom, "Content")
}
/**
 * 文本消息 微信服务器推送过来的
 * 当客服发送菜单消息进行问卷调查之后，客户可以选择，之后会将选项推送回服务器，依据bizmsgmenuid是否有内容来判断是否是菜单点击消息
 *
 *
 * @property menuId 点击的菜单ID 参见客服消息中的菜单消息中的菜单，用户调查时用户点击的菜单id
 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Service_Center_messages.html#7
 * */
class OACustomerClickMenuMsg(xml: String, rootDom: Element): WxXmlMsg(xml, rootDom)
{
    val menuId = get(rootDom, "bizmsgmenuid")
}

/**
 * 图片消息 微信服务器推送过来的
 * @property picUrl 图片链接（由系统生成）
 * @property mediaId 图片消息媒体id，可以调用获取临时素材接口拉取数据。
 * */
class OAImgMSg(xml: String, rootDom: Element): WxXmlMsg(xml, rootDom)
{
    val picUrl = get(rootDom, "PicUrl")
    val mediaId = get(rootDom, "MediaId")
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
class OAVoiceMsg(xml: String, rootDom: Element): WxXmlMsg(xml, rootDom)
{
    val mediaId = get(rootDom, "MediaId")
    val format = get(rootDom, "Format")
    val recognition = get(rootDom, "Recognition")
}

/**
 * 视频消息
 * @property mediaId 视频消息媒体id，可以调用获取临时素材接口拉取数据
 * @property thumbMediaId 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据。
 * */
open class OAVideoMsg(xml: String, rootDom: Element): WxXmlMsg(xml, rootDom)
{
    val mediaId = get(rootDom, "MediaId")
    val thumbMediaId = get(rootDom, "ThumbMediaId")
}
/**
 * 小视频消息
 * @property thumbMediaId 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据。
 * */
class OAShortVideoMsg(xml: String, rootDom: Element): OAVideoMsg(xml, rootDom)

/**
 * 地理位置消息
 * @property locationX Location_X	地理位置纬度
 * @property locationY Location_Y	地理位置经度
 * @property scale 地图缩放大小
 * @property label 地理位置信息
 * */
class OALocationMsg(xml: String, rootDom: Element): WxXmlMsg(xml, rootDom)
{
    val locationX = get(rootDom, "Location_X")?.toFloat()
    val locationY = get(rootDom, "Location_Y")?.toFloat()
    val scale = get(rootDom, "Scale")?.toInt()
    val label = get(rootDom, "Label")
}
/**
 * 链接消息
 * @property title Title	消息标题
 * @property description Description	消息描述
 * @property url Url	消息链接
 * */
class OALinkMsg(xml: String, rootDom: Element): WxXmlMsg(xml, rootDom)
{
    val title = get(rootDom, "Title")
    val description = get(rootDom, "Description")
    val url = get(rootDom, "Url")
}