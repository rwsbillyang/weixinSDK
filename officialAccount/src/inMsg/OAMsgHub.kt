package com.github.rwsbillyang.wxSDK.officialAccount.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import javax.xml.stream.XMLEventReader

/**
 * xml消息事件通知的解包、解析、分发处理
 * */
class OAMsgHub(
         val msgHandler: IOAMsgHandler,
         val eventHandler: IOAEventHandler,
        wxBizMsgCrypt: WXBizMsgCrypt?
):WxMsgHub(wxBizMsgCrypt) {
    override fun dispatchMsg(appId:String, agentId:Int?, reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        return when(base.msgType){
            MsgType.TEXT -> {
                val msg = OACustomerClickMenuMsg(base).apply { read(reader) }
                if(msg.menuId.isNullOrBlank())
                    msgHandler.onOATextMsg(appId, msg)
                else{
                    msgHandler.onOACustomerClickMenuMsg(appId, msg)
                }
            }
            MsgType.IMAGE -> msgHandler.onOAImgMsg(appId,
                OAImgMSg(base).apply { read(reader) }
            )
            MsgType.VOICE -> msgHandler.onOAVoiceMsg(appId,
                OAVoiceMsg(base).apply { read(reader) }
            )
            MsgType.VIDEO -> msgHandler.onOAVideoMsg(appId,
                OAVideoMsg(base).apply { read(reader) }
            )
            MsgType.SHORT_VIDEO -> msgHandler.onOAShortVideoMsg(appId,
                OAShortVideoMsg(base).apply { read(reader) }
            )
            MsgType.LOCATION -> msgHandler.onOALocationMsg(appId,
                OALocationMsg(base).apply { read(reader) }
            )
            MsgType.LINK -> msgHandler.onOALinkMsg(appId,
                OALinkMsg(base).apply { read(reader) }
            )
            else -> msgHandler.onDispatch(appId, agentId, reader, base)?: msgHandler.onDefault(appId,WxBaseMsg(base).apply { read(reader) })
        }
    }

    override fun dispatchEvent(appId:String, agentId:Int?, reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        val baseEvent = WxBaseEvent(base).apply { read(reader) }
        return when (baseEvent.event) {
            InEventType.SUBSCRIBE -> {
                val subscribeEvent = OAScanSubscribeEvent(baseEvent).apply { read(reader) }
                if (subscribeEvent.ticket.isNullOrBlank()) {
                    eventHandler.onOASubscribeEvent(appId, OASubscribeEvent(baseEvent))
                } else {
                    eventHandler.onOAScanSubscribeEvent(appId,subscribeEvent)
                }
            }
            InEventType.UNSUBSCRIBE -> eventHandler.onOAUnsubscribeEvent(appId,
                OAUnsubscribeEvent(baseEvent)
            )
            InEventType.SCAN -> eventHandler.onOAScanEvent(appId,
                OAScanEvent(baseEvent).apply { read(reader) }
            )
            InEventType.LOCATION -> eventHandler.onOALocationEvent(appId,
                OALocationEvent(baseEvent).apply { read(reader) }
            )
            InEventType.CLICK -> eventHandler.onOAMenuClickEvent(appId,
                OAMenuClickEvent(baseEvent).apply { read(reader) }
            )
            InEventType.VIEW -> eventHandler.onOAMenuViewEvent(appId,
                OAMenuViewEvent(baseEvent).apply { read(reader) }
            )
            InEventType.SCAN_CODE_PUSH ->
                eventHandler.onOAMenuScanCodePushEvent(appId,
                    OAMenuScanCodePushEvent(baseEvent).apply { read(reader) }
                )
            InEventType.SCAN_CODE_WAIT_MSG -> eventHandler.onOAMenuScanCodeWaitEvent(appId,
                OAMenuScanCodeWaitEvent(baseEvent).apply { read(reader) }
            )
            InEventType.PIC_SYS_PHOTO -> eventHandler.onOAMenuPhotoEvent(appId,
                OAMenuPhotoEvent(baseEvent).apply { read(reader) }
            )
            InEventType.PIC_PHOTO_OR_ALBUM -> eventHandler.onOAMenuPhotoOrAlbumEvent(appId,
                OAMenuPhotoOrAlbumEvent(baseEvent).apply { read(reader) }
            )
            InEventType.PIC_WEIXIN -> eventHandler.onOAMenuOAAlbumEvent(appId,
                OAMenuOAAlbumEvent(baseEvent).apply { read(reader) }
            )
            InEventType.LOCATION_SELECT -> eventHandler.onOAMenuLocationEvent(appId,
                OAMenuLocationEvent(baseEvent).apply { read(reader) }
            )
            InEventType.VIEW_MINI_PROGRAM -> eventHandler.onOAMenuMiniEvent(appId,
                OAMenuMiniEvent(baseEvent).apply { read(reader) }
            )
            InEventType.MASS_SEND_JOB_FINISH -> eventHandler.onOAMassSendFinishEvent(appId,
                OAMassSendFinishEvent(baseEvent).apply { read(reader) }
            )
            InEventType.TEMPLATE_SEND_JOB_FINISH -> eventHandler.onOATemplateSendJobFinish(appId,
                OATemplateSendJobFinish(baseEvent).apply { read(reader) }
            )
            else -> eventHandler.onDispatch(appId,null, reader, base)?: eventHandler.onDefault(appId,baseEvent)
        }
    }
}