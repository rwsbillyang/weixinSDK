package com.github.rwsbillyang.wxSDK.work.inMsg


import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.IDispatcher
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import javax.xml.stream.XMLEventReader

//对于ISV， appId 来自于suiteId，agentId为空；内建应用：appId是appId，agentID非空。
// 它们来自于接收消息api中的路径参数
interface IWorkMsgHandler: IDispatcher {
    fun onTextMsg(appId: String, agentId:String?, msg: WorkTextMsg): ReBaseMSg?
    fun onImgMsg(appId: String, agentId:String?, msg: WorkImgMSg): ReBaseMSg?
    fun onVoiceMsg(appId: String, agentId:String?, msg: WorkVoiceMsg): ReBaseMSg?
    fun onVideoMsg(appId: String, agentId:String?, msg: WorkVideoMsg): ReBaseMSg?
    fun onLocationMsg(appId: String, agentId:String?, msg: WorkLocationMsg): ReBaseMSg?
    fun onLinkMsg(appId: String, agentId:String?, msg: WorkLinkMsg): ReBaseMSg?
    fun onDefault(appId: String, agentId:String?, msg: WorkBaseMsg): ReBaseMSg?
}

open class DefaultWorkMsgHandler : IWorkMsgHandler{
    override fun onTextMsg(appId: String, agentId:String?, msg: WorkTextMsg) = onDefault(appId, agentId,msg)

    override fun onImgMsg(appId: String, agentId:String?, msg: WorkImgMSg) = onDefault(appId, agentId,msg)

    override fun onVoiceMsg(appId: String, agentId:String?, msg: WorkVoiceMsg) = onDefault(appId, agentId,msg)

    override fun onVideoMsg(appId: String, agentId:String?, msg: WorkVideoMsg) = onDefault(appId, agentId,msg)

    override fun onLocationMsg(appId: String, agentId:String?, msg: WorkLocationMsg) = onDefault(appId, agentId,msg)

    override fun onLinkMsg(appId: String, agentId:String?, msg: WorkLinkMsg) = onDefault(appId, agentId, msg)

    override fun onDefault(appId: String, agentId:String?, msg: WorkBaseMsg): ReBaseMSg? {
        return null
    }
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(appId: String, agentId:String?,reader: XMLEventReader, base: BaseInfo): ReBaseMSg? = null
}