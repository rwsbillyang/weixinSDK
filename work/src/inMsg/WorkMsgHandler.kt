package com.github.rwsbillyang.wxSDK.work.inMsg


import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.IDispatcher
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import javax.xml.stream.XMLEventReader


interface IWorkMsgHandler: IDispatcher {
    fun onTextMsg(msg: WorkTextMsg): ReBaseMSg?
    fun onImgMsg(msg: WorkImgMSg): ReBaseMSg?
    fun onVoiceMsg(msg: WorkVoiceMsg): ReBaseMSg?
    fun onVideoMsg(msg: WorkVideoMsg): ReBaseMSg?
    fun onLocationMsg(msg: WorkLocationMsg): ReBaseMSg?
    fun onLinkMsg(msg: WorkLinkMsg): ReBaseMSg?
    fun onDefault(msg: WorkBaseMsg): ReBaseMSg?
}

open class DefaultWorkMsgHandler : IWorkMsgHandler{
    override fun onTextMsg(msg: WorkTextMsg) = onDefault(msg)

    override fun onImgMsg(msg: WorkImgMSg) = onDefault(msg)

    override fun onVoiceMsg(msg: WorkVoiceMsg) = onDefault(msg)

    override fun onVideoMsg(msg: WorkVideoMsg) = onDefault(msg)

    override fun onLocationMsg(msg: WorkLocationMsg) = onDefault(msg)

    override fun onLinkMsg(msg: WorkLinkMsg) = onDefault(msg)

    override fun onDefault(msg: WorkBaseMsg): ReBaseMSg? {
        return null
    }
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(reader: XMLEventReader, base: BaseInfo) = null
}