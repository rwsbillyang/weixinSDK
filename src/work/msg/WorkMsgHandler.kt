package com.github.rwsbillyang.wxSDK.work.msg


import com.github.rwsbillyang.wxSDK.common.msg.ReBaseMSg


interface IWorkMsgHandler{
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

}