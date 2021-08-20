package com.github.rwsbillyang.wxSDK.work.inMsg

import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.IDispatcher
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.WxBaseEvent
import javax.xml.stream.XMLEventReader

interface IWorkEventHandler: IDispatcher {
    fun onDefault(e: WxBaseEvent): ReBaseMSg?
    fun onSubscribeEvent(e: WorkSubscribeEvent): ReBaseMSg?
    fun onUnsubscribeEvent(e: WorkUnsubscribeEvent): ReBaseMSg?
    fun onLocationEvent(e: WorkLocationEvent): ReBaseMSg?
    fun onMenuClickEvent(e: WorkMenuClickEvent): ReBaseMSg?
    fun onMenuViewEvent(e: WorkMenuViewEvent): ReBaseMSg?
    fun onMenuScanCodePushEvent(e: WorkMenuScanCodePushEvent): ReBaseMSg?
    fun onMenuScanCodeWaitEvent(e: WorkMenuScanCodeWaitEvent): ReBaseMSg?
    fun onMenuPhotoEvent(e: WorkMenuPhotoEvent): ReBaseMSg?
    fun onMenuPhotoOrAlbumEvent(e: WorkMenuPhotoOrAlbumEvent): ReBaseMSg?
    fun onMenuWorkAlbumEvent(e: WorkMenuWorkAlbumEvent): ReBaseMSg?
    fun onMenuLocationEvent(e: WorkMenuLocationEvent): ReBaseMSg?
    fun onEnterAgent(e: WorkEnterAgent): ReBaseMSg?
    fun onBatchJobResultEvent(e: WorkBatchJobResultEvent): ReBaseMSg?
    fun onApprovalStatusChangeEvent(e: WorkApprovalStatusChangeEvent): ReBaseMSg?
    fun onTaskCardClickEvent(e: WorkTaskCardClickEvent): ReBaseMSg?
    fun onPartyCreateEvent(e: WorkPartyCreateEvent): ReBaseMSg?
    fun onPartyUpdateEvent(e: WorkPartyUpdateEvent): ReBaseMSg?
    fun onPartyDelEvent(e: WorkPartyDelEvent): ReBaseMSg?
    fun onUserUpdateEvent(e: WorkUserUpdateEvent): ReBaseMSg?
    fun onUserCreateEvent(e: WorkUserCreateEvent): ReBaseMSg?
    fun onUserDelEvent(e: WorkUserDelEvent): ReBaseMSg?
    fun onTagUpdateEvent(e: WorkTagUpdateEvent): ReBaseMSg?
}

open class DefaultWorkEventHandler : IWorkEventHandler{
    override fun onDefault(e: WxBaseEvent): ReBaseMSg? {
        return null
    }

    override fun onSubscribeEvent(e: WorkSubscribeEvent) = onDefault(e)

    override fun onUnsubscribeEvent(e: WorkUnsubscribeEvent) = onDefault(e)

    override fun onLocationEvent(e: WorkLocationEvent) = onDefault(e)

    override fun onMenuClickEvent(e: WorkMenuClickEvent) = onDefault(e)

    override fun onMenuViewEvent(e: WorkMenuViewEvent) = onDefault(e)

    override fun onMenuScanCodePushEvent(e: WorkMenuScanCodePushEvent) = onDefault(e)

    override fun onMenuScanCodeWaitEvent(e: WorkMenuScanCodeWaitEvent) = onDefault(e)

    override fun onMenuPhotoEvent(e: WorkMenuPhotoEvent) = onDefault(e)

    override fun onMenuPhotoOrAlbumEvent(e: WorkMenuPhotoOrAlbumEvent) = onDefault(e)

    override fun onMenuWorkAlbumEvent(e: WorkMenuWorkAlbumEvent) = onDefault(e)

    override fun onMenuLocationEvent(e: WorkMenuLocationEvent) = onDefault(e)

    override fun onEnterAgent(e: WorkEnterAgent) = onDefault(e)

    override fun onBatchJobResultEvent(e: WorkBatchJobResultEvent) = onDefault(e)

    override fun onApprovalStatusChangeEvent(e: WorkApprovalStatusChangeEvent) = onDefault(e)

    override fun onTaskCardClickEvent(e: WorkTaskCardClickEvent) = onDefault(e)

    override fun onPartyCreateEvent(e: WorkPartyCreateEvent) = onDefault(e)
    override fun onPartyUpdateEvent(e: WorkPartyUpdateEvent) = onDefault(e)

    override fun onPartyDelEvent(e: WorkPartyDelEvent) = onDefault(e)

    override fun onUserUpdateEvent(e: WorkUserUpdateEvent) = onDefault(e)

    override fun onUserCreateEvent(e: WorkUserCreateEvent) = onDefault(e)

    override fun onUserDelEvent(e: WorkUserDelEvent) = onDefault(e)

    override fun onTagUpdateEvent(e: WorkTagUpdateEvent) = onDefault(e)
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(reader: XMLEventReader, base: BaseInfo) = null


}