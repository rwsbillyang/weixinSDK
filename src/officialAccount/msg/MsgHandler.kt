package com.github.rwsbillyang.wxSDK.officialAccount.msg

import com.github.rwsbillyang.wxSDK.common.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.common.msg.ReTextMsg
import com.github.rwsbillyang.wxSDK.common.msg.WxBaseMsg


/**
 * 微信推送过来的消息的处理接口
 *
 * */
interface IOAMsgHandler
{
    /**
     * 文本消息的处理
     * */
    fun onOATextMsg(msg: OATextMsg): ReBaseMSg?

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

class DefaultOAMsgHandler: IOAMsgHandler
{
    override fun onOATextMsg(msg: OATextMsg) = onDefault(msg)

    override fun onOAImgMsg(msg: OAImgMSg) = onDefault(msg)

    override fun onOAVoiceMsg(msg: OAVoiceMsg) = onDefault(msg)

    override fun onOAVideoMsg(msg: OAVideoMsg) = onDefault(msg)

    override fun onOAShortVideoMsg(msg: OAShortVideoMsg) = onDefault(msg)

    override fun onOALocationMsg(msg: OALocationMsg) = onDefault(msg)

    override fun onOALinkMsg(msg: OALinkMsg) = onDefault(msg)

    override fun onDefault(msg: WxBaseMsg): ReBaseMSg? {
        return ReTextMsg("欢迎关注！", msg.base.fromUserName, msg.base.toUserName)
    }

}