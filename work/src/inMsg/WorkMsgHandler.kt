package com.github.rwsbillyang.wxSDK.work.inMsg


import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.IDispatcher
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import javax.xml.stream.XMLEventReader

//对于ISV， appId 来自于suiteId，agentId为空；内建应用：appId是appId，agentID非空。
// 它们来自于接收消息api中的路径参数
interface IWorkMsgHandler: IDispatcher {
    fun onTextMsg(appId: String, agentId:Int?, msg: WorkTextMsg): ReBaseMSg?
    fun onImgMsg(appId: String, agentId:Int?, msg: WorkImgMSg): ReBaseMSg?
    fun onVoiceMsg(appId: String, agentId:Int?, msg: WorkVoiceMsg): ReBaseMSg?
    fun onVideoMsg(appId: String, agentId:Int?, msg: WorkVideoMsg): ReBaseMSg?
    fun onLocationMsg(appId: String, agentId:Int?, msg: WorkLocationMsg): ReBaseMSg?
    fun onLinkMsg(appId: String, agentId:Int?, msg: WorkLinkMsg): ReBaseMSg?
    fun onDefault(appId: String, agentId:Int?, msg: WorkBaseMsg): ReBaseMSg?
}

open class DefaultWorkMsgHandler : IWorkMsgHandler{
    override fun onTextMsg(appId: String, agentId:Int?, msg: WorkTextMsg) = onDefault(appId, agentId,msg)

    override fun onImgMsg(appId: String, agentId:Int?, msg: WorkImgMSg) = onDefault(appId, agentId,msg)

    override fun onVoiceMsg(appId: String, agentId:Int?, msg: WorkVoiceMsg) = onDefault(appId, agentId,msg)

    override fun onVideoMsg(appId: String, agentId:Int?, msg: WorkVideoMsg) = onDefault(appId, agentId,msg)

    override fun onLocationMsg(appId: String, agentId:Int?, msg: WorkLocationMsg) = onDefault(appId, agentId,msg)

    override fun onLinkMsg(appId: String, agentId:Int?, msg: WorkLinkMsg) = onDefault(appId, agentId, msg)

    override fun onDefault(appId: String, agentId:Int?, msg: WorkBaseMsg): ReBaseMSg? {
        return null
    }
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(appId: String, agentId:Int?,reader: XMLEventReader, base: BaseInfo) = null
}