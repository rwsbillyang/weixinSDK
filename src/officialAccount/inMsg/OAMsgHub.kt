package com.github.rwsbillyang.wxSDK.officialAccount.inMsg

import com.github.rwsbillyang.wxSDK.common.msg.*
import com.github.rwsbillyang.wxSDK.common.aes.WXBizMsgCrypt
import javax.xml.stream.XMLEventReader

class OAMsgHub(
        private val msgHandler: IOAMsgHandler,
        private val eventHandler: IOAEventHandler,
        wxBizMsgCrypt: WXBizMsgCrypt?
):WxMsgHub(wxBizMsgCrypt) {
    override fun dispatchMsg(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        return when(base.msgType){
            BaseInfo.TEXT -> {
                val msg = OACustomerClickMenuMsg(base).apply { read(reader) }
                if(msg.menuId.isNullOrBlank())
                    msgHandler.onOATextMsg(msg)
                else{
                    msgHandler.onOACustomerClickMenuMsg(msg)
                }
            }
            BaseInfo.IMAGE -> msgHandler.onOAImgMsg(
                OAImgMSg(base).apply { read(reader) }
            )
            BaseInfo.VOICE -> msgHandler.onOAVoiceMsg(
                OAVoiceMsg(base).apply { read(reader) }
            )
            BaseInfo.VIDEO -> msgHandler.onOAVideoMsg(
                OAVideoMsg(base).apply { read(reader) }
            )
            BaseInfo.SHORT_VIDEO -> msgHandler.onOAShortVideoMsg(
                OAShortVideoMsg(base).apply { read(reader) }
            )
            BaseInfo.LOCATION -> msgHandler.onOALocationMsg(
                OALocationMsg(base).apply { read(reader) }
            )
            BaseInfo.LINK -> msgHandler.onOALinkMsg(
                OALinkMsg(base).apply { read(reader) }
            )
            else -> msgHandler.onDefault(WxBaseMsg(base).apply { read(reader) })
        }
    }

    override fun dispatchEvent(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        val baseEvent = WxBaseEvent(base).apply { read(reader) }
        return when (baseEvent.event) {
            WxBaseEvent.SUBSCRIBE -> {
                val subscribeEvent = OAScanSubscribeEvent(base).apply { read(reader) }
                if (subscribeEvent.ticket.isNullOrBlank()) {
                    eventHandler.onOASubscribeEvent(OASubscribeEvent(base))
                } else {
                    eventHandler.onOAScanSubscribeEvent(subscribeEvent)
                }
            }
            WxBaseEvent.UNSUBSCRIBE -> eventHandler.onOAUnsubscribeEvent(
                OAUnsubscribeEvent(base)
            )
            WxBaseEvent.SCAN -> eventHandler.onOAScanEvent(
                OAScanEvent(base).apply { read(reader) }
            )
            WxBaseEvent.LOCATION -> eventHandler.onOALocationEvent(
                OALocationEvent(base).apply { read(reader) }
            )
            WxBaseEvent.CLICK -> eventHandler.onOAMenuClickEvent(
                OAMenuClickEvent(base).apply { read(reader) }
            )
            WxBaseEvent.VIEW -> eventHandler.onOAMenuViewEvent(
                OAMenuViewEvent(base).apply { read(reader) }
            )
            WxBaseEvent.SCAN_CODE_PUSH ->
                eventHandler.onOAMenuScanCodePushEvent(
                    OAMenuScanCodePushEvent(base).apply { read(reader) }
                )
            WxBaseEvent.SCAN_CODE_WAIT_MSG -> eventHandler.onOAMenuScanCodeWaitEvent(
                OAMenuScanCodeWaitEvent(base).apply { read(reader) }
            )
            WxBaseEvent.PIC_SYS_PHOTO -> eventHandler.onOAMenuPhotoEvent(
                OAMenuPhotoEvent(base).apply { read(reader) }
            )
            WxBaseEvent.PIC_PHOTO_OR_ALBUM -> eventHandler.onOAMenuPhotoOrAlbumEvent(
                OAMenuPhotoOrAlbumEvent(base).apply { read(reader) }
            )
            WxBaseEvent.PIC_WEIXIN -> eventHandler.onOAMenuOAAlbumEvent(
                OAMenuOAAlbumEvent(base).apply { read(reader) }
            )
            WxBaseEvent.LOCATION_SELECT -> eventHandler.onOAMenuLocationEvent(
                OAMenuLocationEvent(base).apply { read(reader) }
            )
            WxBaseEvent.VIEW_MINI_PROGRAM -> eventHandler.onOAMenuMiniEvent(
                OAMenuMiniEvent(base).apply { read(reader) }
            )
            WxBaseEvent.MASS_SEND_JOB_FINISH -> eventHandler.onOAOAMassSendFinishEvent(
                OAMassSendFinishEvent(base).apply { read(reader) }
            )
            WxBaseEvent.TEMPLATE_SEND_JOB_FINISH -> eventHandler.onOATemplateSendJobFinish(
                OATemplateSendJobFinish(base).apply { read(reader) }
            )
            else -> eventHandler.onDefault(baseEvent)
        }
    }
}