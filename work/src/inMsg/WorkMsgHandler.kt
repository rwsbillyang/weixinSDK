package com.github.rwsbillyang.wxSDK.work.inMsg


import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.IDispatcher
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import javax.xml.stream.XMLEventReader


interface IWorkMsgHandler: IDispatcher {
    fun onWorkTextMsg(msg: WorkTextMsg): ReBaseMSg?
    fun onWorkImgMsg(msg: WorkImgMSg): ReBaseMSg?
    fun onWorkVoiceMsg(msg: WorkVoiceMsg): ReBaseMSg?
    fun onWorkVideoMsg(msg: WorkVideoMsg): ReBaseMSg?
    fun onWorkLocationMsg(msg: WorkLocationMsg): ReBaseMSg?
    fun onWorkLinkMsg(msg: WorkLinkMsg): ReBaseMSg?
    fun onDefault(msg: WorkBaseMsg): ReBaseMSg?
}

open class DefaultWorkMsgHandler : IWorkMsgHandler{
    override fun onWorkTextMsg(msg: WorkTextMsg) = onDefault(msg)

    override fun onWorkImgMsg(msg: WorkImgMSg) = onDefault(msg)

    override fun onWorkVoiceMsg(msg: WorkVoiceMsg) = onDefault(msg)

    override fun onWorkVideoMsg(msg: WorkVideoMsg) = onDefault(msg)

    override fun onWorkLocationMsg(msg: WorkLocationMsg) = onDefault(msg)

    override fun onWorkLinkMsg(msg: WorkLinkMsg) = onDefault(msg)

    override fun onDefault(msg: WorkBaseMsg): ReBaseMSg? {
        return null
    }
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(reader: XMLEventReader, base: BaseInfo) = null
}