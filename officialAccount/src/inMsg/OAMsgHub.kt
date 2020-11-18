package com.github.rwsbillyang.wxSDK.officialAccount.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import javax.xml.stream.XMLEventReader

class OAMsgHub(
         val msgHandler: IOAMsgHandler,
         val eventHandler: IOAEventHandler,
        wxBizMsgCrypt: WXBizMsgCrypt?
):WxMsgHub(wxBizMsgCrypt) {
    override fun dispatchMsg(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        return when(base.msgType){
            MsgType.TEXT -> {
                val msg = OACustomerClickMenuMsg(base).apply { read(reader) }
                if(msg.menuId.isNullOrBlank())
                    msgHandler.onOATextMsg(msg)
                else{
                    msgHandler.onOACustomerClickMenuMsg(msg)
                }
            }
            MsgType.IMAGE -> msgHandler.onOAImgMsg(
                OAImgMSg(base).apply { read(reader) }
            )
            MsgType.VOICE -> msgHandler.onOAVoiceMsg(
                OAVoiceMsg(base).apply { read(reader) }
            )
            MsgType.VIDEO -> msgHandler.onOAVideoMsg(
                OAVideoMsg(base).apply { read(reader) }
            )
            MsgType.SHORT_VIDEO -> msgHandler.onOAShortVideoMsg(
                OAShortVideoMsg(base).apply { read(reader) }
            )
            MsgType.LOCATION -> msgHandler.onOALocationMsg(
                OALocationMsg(base).apply { read(reader) }
            )
            MsgType.LINK -> msgHandler.onOALinkMsg(
                OALinkMsg(base).apply { read(reader) }
            )
            else -> msgHandler.onDispatch(reader, base)?: msgHandler.onDefault(WxBaseMsg(base).apply { read(reader) })
        }
    }

    override fun dispatchEvent(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        val baseEvent = WxBaseEvent(base).apply { read(reader) }
        return when (baseEvent.event) {
            InEventType.SUBSCRIBE -> {
                val subscribeEvent = OAScanSubscribeEvent(base).apply { read(reader) }
                if (subscribeEvent.ticket.isNullOrBlank()) {
                    eventHandler.onOASubscribeEvent(OASubscribeEvent(base))
                } else {
                    eventHandler.onOAScanSubscribeEvent(subscribeEvent)
                }
            }
            InEventType.UNSUBSCRIBE -> eventHandler.onOAUnsubscribeEvent(
                OAUnsubscribeEvent(base)
            )
            InEventType.SCAN -> eventHandler.onOAScanEvent(
                OAScanEvent(base).apply { read(reader) }
            )
            InEventType.LOCATION -> eventHandler.onOALocationEvent(
                OALocationEvent(base).apply { read(reader) }
            )
            InEventType.CLICK -> eventHandler.onOAMenuClickEvent(
                OAMenuClickEvent(base).apply { read(reader) }
            )
            InEventType.VIEW -> eventHandler.onOAMenuViewEvent(
                OAMenuViewEvent(base).apply { read(reader) }
            )
            InEventType.SCAN_CODE_PUSH ->
                eventHandler.onOAMenuScanCodePushEvent(
                    OAMenuScanCodePushEvent(base).apply { read(reader) }
                )
            InEventType.SCAN_CODE_WAIT_MSG -> eventHandler.onOAMenuScanCodeWaitEvent(
                OAMenuScanCodeWaitEvent(base).apply { read(reader) }
            )
            InEventType.PIC_SYS_PHOTO -> eventHandler.onOAMenuPhotoEvent(
                OAMenuPhotoEvent(base).apply { read(reader) }
            )
            InEventType.PIC_PHOTO_OR_ALBUM -> eventHandler.onOAMenuPhotoOrAlbumEvent(
                OAMenuPhotoOrAlbumEvent(base).apply { read(reader) }
            )
            InEventType.PIC_WEIXIN -> eventHandler.onOAMenuOAAlbumEvent(
                OAMenuOAAlbumEvent(base).apply { read(reader) }
            )
            InEventType.LOCATION_SELECT -> eventHandler.onOAMenuLocationEvent(
                OAMenuLocationEvent(base).apply { read(reader) }
            )
            InEventType.VIEW_MINI_PROGRAM -> eventHandler.onOAMenuMiniEvent(
                OAMenuMiniEvent(base).apply { read(reader) }
            )
            InEventType.MASS_SEND_JOB_FINISH -> eventHandler.onOAMassSendFinishEvent(
                OAMassSendFinishEvent(base).apply { read(reader) }
            )
            InEventType.TEMPLATE_SEND_JOB_FINISH -> eventHandler.onOATemplateSendJobFinish(
                OATemplateSendJobFinish(base).apply { read(reader) }
            )
            else -> eventHandler.onDispatch(reader, base)?: eventHandler.onDefault(baseEvent)
        }
    }
}