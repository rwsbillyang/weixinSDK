package com.github.rwsbillyang.wxSDK.work.inMsg

import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.IDispatcher
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.WxBaseEvent
import javax.xml.stream.XMLEventReader

//对于ISV， appId 来自于suiteId，agentId为空；内建应用：appId是corpId，agentID非空。
// 它们来自于接收消息api中的路径参数
interface IWorkEventHandler: IDispatcher {
    fun onDefault(appId: String, agentId:Int?,e: WxBaseEvent): ReBaseMSg?
    fun onSubscribeEvent(appId: String, agentId:Int?,e: WorkSubscribeEvent): ReBaseMSg?
    fun onUnsubscribeEvent(appId: String, agentId:Int?,e: WorkUnsubscribeEvent): ReBaseMSg?
    fun onLocationEvent(appId: String, agentId:Int?,e: WorkLocationEvent): ReBaseMSg?
    fun onMenuClickEvent(appId: String, agentId:Int?,e: WorkMenuClickEvent): ReBaseMSg?
    fun onMenuViewEvent(appId: String, agentId:Int?,e: WorkMenuViewEvent): ReBaseMSg?
    fun onMenuScanCodePushEvent(appId: String, agentId:Int?,e: WorkMenuScanCodePushEvent): ReBaseMSg?
    fun onMenuScanCodeWaitEvent(appId: String, agentId:Int?,e: WorkMenuScanCodeWaitEvent): ReBaseMSg?
    fun onMenuPhotoEvent(appId: String, agentId:Int?,e: WorkMenuPhotoEvent): ReBaseMSg?
    fun onMenuPhotoOrAlbumEvent(appId: String, agentId:Int?,e: WorkMenuPhotoOrAlbumEvent): ReBaseMSg?
    fun onMenuWorkAlbumEvent(appId: String, agentId:Int?,e: WorkMenuWorkAlbumEvent): ReBaseMSg?
    fun onMenuLocationEvent(appId: String, agentId:Int?,e: WorkMenuLocationEvent): ReBaseMSg?
    fun onEnterAgent(appId: String, agentId:Int?,e: WorkEnterAgent): ReBaseMSg?
    fun onBatchJobResultEvent(appId: String, agentId:Int?,e: WorkBatchJobResultEvent): ReBaseMSg?
    fun onApprovalStatusChangeEvent(appId: String, agentId:Int?,e: WorkApprovalStatusChangeEvent): ReBaseMSg?
    fun onTaskCardClickEvent(appId: String, agentId:Int?,e: WorkTaskCardClickEvent): ReBaseMSg?
    fun onPartyCreateEvent(appId: String, agentId:Int?,e: WorkPartyCreateEvent): ReBaseMSg?
    fun onPartyUpdateEvent(appId: String, agentId:Int?,e: WorkPartyUpdateEvent): ReBaseMSg?
    fun onPartyDelEvent(appId: String, agentId:Int?,e: WorkPartyDelEvent): ReBaseMSg?
    fun onUserUpdateEvent(appId: String, agentId:Int?,e: WorkUserUpdateEvent): ReBaseMSg?
    fun onUserCreateEvent(appId: String, agentId:Int?,e: WorkUserCreateEvent): ReBaseMSg?
    fun onUserDelEvent(appId: String, agentId:Int?,e: WorkUserDelEvent): ReBaseMSg?
    fun onTagUpdateEvent(appId: String, agentId:Int?,e: WorkTagUpdateEvent): ReBaseMSg?
}

open class DefaultWorkEventHandler : IWorkEventHandler{
    override fun onDefault(appId: String, agentId:Int?,e: WxBaseEvent): ReBaseMSg? {
        return null
    }

    override fun onSubscribeEvent(appId: String, agentId:Int?,e: WorkSubscribeEvent) = onDefault(appId, agentId, e)

    override fun onUnsubscribeEvent(appId: String, agentId:Int?,e: WorkUnsubscribeEvent) = onDefault(appId, agentId,e)

    override fun onLocationEvent(appId: String, agentId:Int?,e: WorkLocationEvent) = onDefault(appId, agentId,e)

    override fun onMenuClickEvent(appId: String, agentId:Int?,e: WorkMenuClickEvent) = onDefault(appId, agentId,e)

    override fun onMenuViewEvent(appId: String, agentId:Int?,e: WorkMenuViewEvent) = onDefault(appId, agentId,e)

    override fun onMenuScanCodePushEvent(appId: String, agentId:Int?,e: WorkMenuScanCodePushEvent) = onDefault(appId, agentId,e)

    override fun onMenuScanCodeWaitEvent(appId: String, agentId:Int?,e: WorkMenuScanCodeWaitEvent) = onDefault(appId, agentId,e)

    override fun onMenuPhotoEvent(appId: String, agentId:Int?,e: WorkMenuPhotoEvent) = onDefault(appId, agentId,e)

    override fun onMenuPhotoOrAlbumEvent(appId: String, agentId:Int?,e: WorkMenuPhotoOrAlbumEvent) = onDefault(appId, agentId,e)

    override fun onMenuWorkAlbumEvent(appId: String, agentId:Int?,e: WorkMenuWorkAlbumEvent) = onDefault(appId, agentId,e)

    override fun onMenuLocationEvent(appId: String, agentId:Int?,e: WorkMenuLocationEvent) = onDefault(appId, agentId,e)

    override fun onEnterAgent(appId: String, agentId:Int?,e: WorkEnterAgent) = onDefault(appId, agentId,e)

    override fun onBatchJobResultEvent(appId: String, agentId:Int?,e: WorkBatchJobResultEvent) = onDefault(appId, agentId,e)

    override fun onApprovalStatusChangeEvent(appId: String, agentId:Int?,e: WorkApprovalStatusChangeEvent) = onDefault(appId, agentId,e)

    override fun onTaskCardClickEvent(appId: String, agentId:Int?,e: WorkTaskCardClickEvent) = onDefault(appId, agentId,e)

    override fun onPartyCreateEvent(appId: String, agentId:Int?,e: WorkPartyCreateEvent) = onDefault(appId, agentId,e)
    override fun onPartyUpdateEvent(appId: String, agentId:Int?,e: WorkPartyUpdateEvent) = onDefault(appId, agentId,e)

    override fun onPartyDelEvent(appId: String, agentId:Int?,e: WorkPartyDelEvent) = onDefault(appId, agentId,e)

    override fun onUserUpdateEvent(appId: String, agentId:Int?,e: WorkUserUpdateEvent) = onDefault(appId, agentId,e)

    override fun onUserCreateEvent(appId: String, agentId:Int?,e: WorkUserCreateEvent) = onDefault(appId, agentId,e)

    override fun onUserDelEvent(appId: String, agentId:Int?,e: WorkUserDelEvent) = onDefault(appId, agentId,e)

    override fun onTagUpdateEvent(appId: String, agentId:Int?,e: WorkTagUpdateEvent) = onDefault(appId, agentId,e)
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(appId: String, agentId:Int?,reader: XMLEventReader, base: BaseInfo) = null


}