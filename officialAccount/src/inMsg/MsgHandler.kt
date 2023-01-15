package com.github.rwsbillyang.wxSDK.officialAccount.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import org.w3c.dom.Element


/**
 * 微信推送过来的消息的处理接口
 *
 * */
interface IOAMsgHandler: IDispatcher
{
    /**
     * 文本消息的处理
     * */
    fun onOATextMsg(appId: String, msg: OATextMsg): ReBaseMSg?

    /**
     * 当客服发送菜单消息进行问卷调查之后，客户可以选择，之后会将选项推送回服务器，里面包含了菜单id： menuId
     * */
    fun onOACustomerClickMenuMsg(appId: String, msg: OACustomerClickMenuMsg): ReBaseMSg?

    /**
     * 图片消息的处理
     * */
    fun onOAImgMsg(appId: String, msg: OAImgMSg): ReBaseMSg?

    /**
     * 语音消息的处理
     * */
    fun onOAVoiceMsg(appId: String, msg: OAVoiceMsg): ReBaseMSg?

    /**
     * 视频消息的处理
     * */
    fun onOAVideoMsg(appId: String, msg: OAVideoMsg): ReBaseMSg?

    /**
     * 小视频消息的处理
     * */
    fun onOAShortVideoMsg(appId: String, msg: OAShortVideoMsg): ReBaseMSg?

    /**
     * 地理位置消息的处理
     * */
    fun onOALocationMsg(appId: String, msg: OALocationMsg): ReBaseMSg?

    /**
     * 链接消息的处理
     * */
    fun onOALinkMsg(appId: String, msg: OALinkMsg): ReBaseMSg?

    fun onDefault(appId: String, msg: WxXmlMsg): ReBaseMSg?
}

open class DefaultOAMsgHandler: IOAMsgHandler
{
    override fun onOATextMsg(appId: String, msg: OATextMsg) = onDefault(appId, msg)
    override fun onOACustomerClickMenuMsg(appId: String, msg: OACustomerClickMenuMsg) = onDefault(appId, msg)

    override fun onOAImgMsg(appId: String, msg: OAImgMSg) = onDefault(appId, msg)

    override fun onOAVoiceMsg(appId: String, msg: OAVoiceMsg) = onDefault(appId, msg)

    override fun onOAVideoMsg(appId: String, msg: OAVideoMsg) = onDefault(appId, msg)

    override fun onOAShortVideoMsg(appId: String, msg: OAShortVideoMsg) = onDefault(appId, msg)

    override fun onOALocationMsg(appId: String, msg: OALocationMsg) = onDefault(appId, msg)

    override fun onOALinkMsg(appId: String, msg: OALinkMsg) = onDefault(appId, msg)

    override fun onDefault(appId: String, msg: WxXmlMsg): ReBaseMSg? {
        return ReTextMsg("欢迎关注！", msg.fromUserName, msg.toUserName)
    }
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(appId: String, agentId:String?, xml: String, rootDom: Element, msgOrEventType: String?) = null

}