package com.github.rwsbillyang.wxSDK.officialAccount.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import javax.xml.stream.XMLEventReader


/**
 * 微信推送过来的消息的处理接口
 *
 * */
interface IOAMsgHandler: IDispatcher
{
    /**
     * 文本消息的处理
     * */
    fun onOATextMsg(msg: OATextMsg): ReBaseMSg?

    /**
     * 当客服发送菜单消息进行问卷调查之后，客户可以选择，之后会将选项推送回服务器，里面包含了菜单id： menuId
     * */
    fun onOACustomerClickMenuMsg(msg: OACustomerClickMenuMsg): ReBaseMSg?

    /**
     * 图片消息的处理
     * */
    fun onOAImgMsg(msg: OAImgMSg): ReBaseMSg?

    /**
     * 语音消息的处理
     * */
    fun onOAVoiceMsg(msg: OAVoiceMsg): ReBaseMSg?

    /**
     * 视频消息的处理
     * */
    fun onOAVideoMsg(msg: OAVideoMsg): ReBaseMSg?

    /**
     * 小视频消息的处理
     * */
    fun onOAShortVideoMsg(msg: OAShortVideoMsg): ReBaseMSg?

    /**
     * 地理位置消息的处理
     * */
    fun onOALocationMsg(msg: OALocationMsg): ReBaseMSg?

    /**
     * 链接消息的处理
     * */
    fun onOALinkMsg(msg: OALinkMsg): ReBaseMSg?

    fun onDefault(msg: WxBaseMsg): ReBaseMSg?
}

open class DefaultOAMsgHandler: IOAMsgHandler
{
    override fun onOATextMsg(msg: OATextMsg) = onDefault(msg)
    override fun onOACustomerClickMenuMsg(msg: OACustomerClickMenuMsg) = onDefault(msg)

    override fun onOAImgMsg(msg: OAImgMSg) = onDefault(msg)

    override fun onOAVoiceMsg(msg: OAVoiceMsg) = onDefault(msg)

    override fun onOAVideoMsg(msg: OAVideoMsg) = onDefault(msg)

    override fun onOAShortVideoMsg(msg: OAShortVideoMsg) = onDefault(msg)

    override fun onOALocationMsg(msg: OALocationMsg) = onDefault(msg)

    override fun onOALinkMsg(msg: OALinkMsg) = onDefault(msg)

    override fun onDefault(msg: WxBaseMsg): ReBaseMSg? {
        return ReTextMsg("欢迎关注！", msg.base.fromUserName, msg.base.toUserName)
    }
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(reader: XMLEventReader, base: BaseInfo) = null

}