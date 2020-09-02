package com.github.rwsbillyang.wxSDK.officialAccount.msg

import com.github.rwsbillyang.wxSDK.common.msg.*
import javax.xml.stream.XMLEventReader

class WxOAMsgHub(
    private val msgHandler: IOAMsgHandler,
    private val eventHandler: IOAEventHandler,
    aesKey: String?
):WxMsgHub(aesKey) {
    override fun dispatchMsg(reader: XMLEventReader, base: BaseMsg): ReBaseMSg?{
        return when(base.msgType){
            BaseMsg.TEXT -> msgHandler.onWxTextMsg(
                WxTextMsg(base).apply { read(reader) }
            )
            BaseMsg.IMAGE -> msgHandler.onWxImgMSg(
                WxImgMSg(base).apply { read(reader) }
            )
            BaseMsg.VOICE -> msgHandler.onWxVoiceMsg(
                WxVoiceMsg(base).apply { read(reader) }
            )
            BaseMsg.VIDEO -> msgHandler.onWxVideoMsg(
                WxVideoMsg(base).apply { read(reader) }
            )
            BaseMsg.SHORT_VIDEO -> msgHandler.onWxShortVideoMsg(
                WxShortVideoMsg(base).apply { read(reader) }
            )
            BaseMsg.LOCATION -> msgHandler.onWxLocationMsg(
                WxLocationMsg(base).apply { read(reader) }
            )
            BaseMsg.LINK -> msgHandler.onWxLinkMsg(
                WxLinkMsg(base).apply { read(reader) }
            )
            else -> msgHandler.onDefault(WxBaseMsg(base).apply { read(reader) })
        }
    }

    override fun dispatchEvent(reader: XMLEventReader, base: BaseMsg): ReBaseMSg?{
        val baseEvent = WxBaseEvent(base).apply { read(reader) }
        return when (baseEvent.event) {
            WxBaseEvent.SUBSCRIBE -> {
                val subscribeEvent = WxScanSubscribeEvent(base).apply { read(reader) }
                if (subscribeEvent.ticket.isNullOrBlank()) {
                    eventHandler.onWxSubscribeEvent(WxSubscribeEvent(base))
                } else {
                    eventHandler.onWxScanSubscribeEvent(subscribeEvent)
                }
            }
            WxBaseEvent.UNSUBSCRIBE -> eventHandler.onWxUnsubscribeEvent(
                WxUnsubscribeEvent(base)
            )
            WxBaseEvent.SCAN -> eventHandler.onWxScanEvent(
                WxScanEvent(base).apply { read(reader) }
            )
            WxBaseEvent.LOCATION -> eventHandler.onWxLocationEvent(
                WxLocationEvent(base).apply { read(reader) }
            )
            WxBaseEvent.CLICK -> eventHandler.onWxMenuClickEvent(
                WxMenuClickEvent(base).apply { read(reader) }
            )
            WxBaseEvent.VIEW -> eventHandler.onWxMenuViewEvent(
                WxMenuViewEvent(base).apply { read(reader) }
            )
            WxBaseEvent.SCAN_CODE_PUSH ->
                eventHandler.onWxMenuScanCodePushEvent(
                    WxMenuScanCodePushEvent(base).apply { read(reader) }
                )
            WxBaseEvent.SCAN_CODE_WAIT_MSG -> eventHandler.onWxMenuScanCodeWaitEvent(
                WxMenuScanCodeWaitEvent(base).apply { read(reader) }
            )
            WxBaseEvent.PIC_SYS_PHOTO -> eventHandler.onWxMenuPhotoEvent(
                WxMenuPhotoEvent(base).apply { read(reader) }
            )
            WxBaseEvent.PIC_PHOTO_OR_ALBUM -> eventHandler.onWxMenuPhotoOrAlbumEvent(
                WxMenuPhotoOrAlbumEvent(base).apply { read(reader) }
            )
            WxBaseEvent.PIC_WEIXIN -> eventHandler.onWxMenuWxAlbumEvent(
                WxMenuWxAlbumEvent(base).apply { read(reader) }
            )
            WxBaseEvent.LOCATION_SELECT -> eventHandler.onWxMenuLocationEvent(
                WxMenuLocationEvent(base).apply { read(reader) }
            )
            WxBaseEvent.VIEW_MINI_PROGRAM -> eventHandler.onWxMenuMiniEvent(
                WxMenuMiniEvent(base).apply { read(reader) }
            )
            WxBaseEvent.MASS_SEND_JOB_FINISH -> eventHandler.onWxWxMassSendFinishEvent(
                WxMassSendFinishEvent(base).apply { read(reader) }
            )
            WxBaseEvent.TEMPLATE_SEND_JOB_FINISH -> eventHandler.onWxTemplateSendJobFinish(
                WxTemplateSendJobFinish(base).apply { read(reader) }
            )
            else -> eventHandler.onDefault(baseEvent)
        }
    }
}