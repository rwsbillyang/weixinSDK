package com.github.rwsbillyang.wxSDK.work.inMsg

import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.IDispatcher
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.WxBaseEvent
import javax.xml.stream.XMLEventReader

interface IWorkEventHandler: IDispatcher {
    fun onDefault(e: WxBaseEvent): ReBaseMSg?
    fun onWorkSubscribeEvent(e: WorkSubscribeEvent): ReBaseMSg?
    fun onWorkUnsubscribeEvent(e: WorkUnsubscribeEvent): ReBaseMSg?
    fun onWorkLocationEvent(e: WorkLocationEvent): ReBaseMSg?
    fun onWorkMenuClickEvent(e: WorkMenuClickEvent): ReBaseMSg?
    fun onWorkMenuViewEvent(e: WorkMenuViewEvent): ReBaseMSg?
    fun onWorkMenuScanCodePushEvent(e: WorkMenuScanCodePushEvent): ReBaseMSg?
    fun onWorkMenuScanCodeWaitEvent(e: WorkMenuScanCodeWaitEvent): ReBaseMSg?
    fun onWorkMenuPhotoEvent(e: WorkMenuPhotoEvent): ReBaseMSg?
    fun onWorkMenuPhotoOrAlbumEvent(e: WorkMenuPhotoOrAlbumEvent): ReBaseMSg?
    fun onWorkMenuWorkAlbumEvent(e: WorkMenuWorkAlbumEvent): ReBaseMSg?
    fun onWorkMenuLocationEvent(e: WorkMenuLocationEvent): ReBaseMSg?
    fun onWorkEnterAgent(e: WorkEnterAgent): ReBaseMSg?
    fun onWorkBatchJobResultEvent(e: WorkBatchJobResultEvent): ReBaseMSg?
    fun onWorkApprovalStatusChangeEvent(e: WorkApprovalStatusChangeEvent): ReBaseMSg?
    fun onWorkTaskCardClickEvent(e: WorkTaskCardClickEvent): ReBaseMSg?
    fun onWorkPartyCreateEvent(e: WorkPartyCreateEvent): ReBaseMSg?
    fun onWorkPartyUpdateEvent(e: WorkPartyUpdateEvent): ReBaseMSg?
    fun onWorkPartyDelEvent(e: WorkPartyDelEvent): ReBaseMSg?
    fun onWorkUserUpdateEvent(e: WorkUserUpdateEvent): ReBaseMSg?
    fun onWorkUserCreateEvent(e: WorkUserCreateEvent): ReBaseMSg?
    fun onWorkUserDelEvent(e: WorkUserDelEvent): ReBaseMSg?
    fun onWorkTagUpdateEvent(e: WorkTagUpdateEvent): ReBaseMSg?
}

open class DefaultWorkEventHandler : IWorkEventHandler{
    override fun onDefault(e: WxBaseEvent): ReBaseMSg? {
        return null
    }

    override fun onWorkSubscribeEvent(e: WorkSubscribeEvent) = onDefault(e)

    override fun onWorkUnsubscribeEvent(e: WorkUnsubscribeEvent) = onDefault(e)

    override fun onWorkLocationEvent(e: WorkLocationEvent) = onDefault(e)

    override fun onWorkMenuClickEvent(e: WorkMenuClickEvent) = onDefault(e)

    override fun onWorkMenuViewEvent(e: WorkMenuViewEvent) = onDefault(e)

    override fun onWorkMenuScanCodePushEvent(e: WorkMenuScanCodePushEvent) = onDefault(e)

    override fun onWorkMenuScanCodeWaitEvent(e: WorkMenuScanCodeWaitEvent) = onDefault(e)

    override fun onWorkMenuPhotoEvent(e: WorkMenuPhotoEvent) = onDefault(e)

    override fun onWorkMenuPhotoOrAlbumEvent(e: WorkMenuPhotoOrAlbumEvent) = onDefault(e)

    override fun onWorkMenuWorkAlbumEvent(e: WorkMenuWorkAlbumEvent) = onDefault(e)

    override fun onWorkMenuLocationEvent(e: WorkMenuLocationEvent) = onDefault(e)

    override fun onWorkEnterAgent(e: WorkEnterAgent) = onDefault(e)

    override fun onWorkBatchJobResultEvent(e: WorkBatchJobResultEvent) = onDefault(e)

    override fun onWorkApprovalStatusChangeEvent(e: WorkApprovalStatusChangeEvent) = onDefault(e)

    override fun onWorkTaskCardClickEvent(e: WorkTaskCardClickEvent) = onDefault(e)

    override fun onWorkPartyCreateEvent(e: WorkPartyCreateEvent) = onDefault(e)
    override fun onWorkPartyUpdateEvent(e: WorkPartyUpdateEvent) = onDefault(e)

    override fun onWorkPartyDelEvent(e: WorkPartyDelEvent) = onDefault(e)

    override fun onWorkUserUpdateEvent(e: WorkUserUpdateEvent) = onDefault(e)

    override fun onWorkUserCreateEvent(e: WorkUserCreateEvent) = onDefault(e)

    override fun onWorkUserDelEvent(e: WorkUserDelEvent) = onDefault(e)

    override fun onWorkTagUpdateEvent(e: WorkTagUpdateEvent) = onDefault(e)
    /**
     * 未知类型的msg或event可以继续进行读取其额外信息，从而可以自定义分发和处理
     * 返回null表示由onDefault继续处理
     * */
    override fun onDispatch(reader: XMLEventReader, base: BaseInfo) = null


}