package com.github.rwsbillyang.wxSDK.officialAccount.msg

import com.github.rwsbillyang.wxSDK.common.msg.IMsgHandler
import com.github.rwsbillyang.wxSDK.common.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.common.msg.WxBaseMsg

/**
 * 微信推送过来的消息的处理接口
 *
 * */
interface IOAMsgHandler: IMsgHandler
{
    /**
     * 文本消息的处理
     * */
    fun onWxTextMsg(msg: WxTextMsg): ReBaseMSg?

    /**
     * 图片消息的处理
     * */
    fun onWxImgMSg(msg: WxImgMSg): ReBaseMSg?

    /**
     * 语音消息的处理
     * */
    fun onWxVoiceMsg(msg: WxVoiceMsg): ReBaseMSg?

    /**
     * 视频消息的处理
     * */
    fun onWxVideoMsg(msg: WxVideoMsg): ReBaseMSg?

    /**
     * 小视频消息的处理
     * */
    fun onWxShortVideoMsg(msg: WxShortVideoMsg): ReBaseMSg?

    /**
     * 地理位置消息的处理
     * */
    fun onWxLocationMsg(msg: WxLocationMsg): ReBaseMSg?

    /**
     * 链接消息的处理
     * */
    fun onWxLinkMsg(msg: WxLinkMsg): ReBaseMSg?


}

class DefaultOAMsgHandler: IOAMsgHandler
{
    override fun onWxTextMsg(msg: WxTextMsg) = onDefault(msg)

    override fun onWxImgMSg(msg: WxImgMSg) = onDefault(msg)

    override fun onWxVoiceMsg(msg: WxVoiceMsg) = onDefault(msg)

    override fun onWxVideoMsg(msg: WxVideoMsg) = onDefault(msg)

    override fun onWxShortVideoMsg(msg: WxShortVideoMsg) = onDefault(msg)

    override fun onWxLocationMsg(msg: WxLocationMsg) = onDefault(msg)

    override fun onWxLinkMsg(msg: WxLinkMsg) = onDefault(msg)

    override fun onDefault(msg: WxBaseMsg): ReBaseMSg? {
        return ReTextMsg("欢迎关注！", msg.base.fromUserName, msg.base.toUserName)
    }

}