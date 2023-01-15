package com.github.rwsbillyang.wxSDK.officialAccount.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Element

/**
 * xml消息事件通知的解包、解析、分发处理
 * */
class OAMsgHub(
         val msgHandler: IOAMsgHandler?,
         val eventHandler: IOAEventHandler?,
        wxBizMsgCrypt: WXBizMsgCrypt?
):WxMsgHub(wxBizMsgCrypt) {
    private val log: Logger = LoggerFactory.getLogger("OAMsgHub")
    override fun dispatchMsg(appId: String, agentId: String?, xml: String, rootDom: Element, msgType: String?): ReBaseMSg?{
        if(msgHandler == null) return null
        return when(msgType){
            MsgType.TEXT -> {
                val menuId = rootDom.get("bizmsgmenuid")
                if(menuId.isNullOrBlank())
                    msgHandler.onOATextMsg(appId, OATextMsg(xml, rootDom))
                else{
                    msgHandler.onOACustomerClickMenuMsg(appId, OACustomerClickMenuMsg(xml, rootDom))
                }
            }
            MsgType.IMAGE -> msgHandler.onOAImgMsg(appId,
                OAImgMSg(xml, rootDom)
            )
            MsgType.VOICE -> msgHandler.onOAVoiceMsg(appId,
                OAVoiceMsg(xml, rootDom)
            )
            MsgType.VIDEO -> msgHandler.onOAVideoMsg(appId,
                OAVideoMsg(xml, rootDom)
            )
            MsgType.SHORT_VIDEO -> msgHandler.onOAShortVideoMsg(appId,
                OAShortVideoMsg(xml, rootDom)
            )
            MsgType.LOCATION -> msgHandler.onOALocationMsg(appId,
                OALocationMsg(xml, rootDom)
            )
            MsgType.LINK -> msgHandler.onOALinkMsg(appId,
                OALinkMsg(xml, rootDom)
            )
            else -> msgHandler.onDispatch(appId, agentId, xml, rootDom, msgType)?: msgHandler.onDefault(appId, WxXmlMsg(xml, rootDom))
        }
    }

    override fun dispatchEvent(appId: String, agentId: String?, xml: String, rootDom: Element, eventType: String?): ReBaseMSg?{
        if(eventHandler == null) return null
        return when (eventType) {
            InEventType.SUBSCRIBE -> {
                val subscribeEvent = OAScanSubscribeEvent(xml, rootDom)
                if (subscribeEvent.ticket.isNullOrBlank()) {
                    eventHandler.onOASubscribeEvent(appId, OASubscribeEvent(xml, rootDom))
                } else {
                    eventHandler.onOAScanSubscribeEvent(appId,subscribeEvent)
                }
            }
            InEventType.UNSUBSCRIBE -> eventHandler.onOAUnsubscribeEvent(appId,
                OAUnsubscribeEvent(xml, rootDom)
            )
            InEventType.SCAN -> eventHandler.onOAScanEvent(appId,
                OAScanEvent(xml, rootDom)
            )
            InEventType.LOCATION -> eventHandler.onOALocationEvent(appId,
                OALocationEvent(xml, rootDom)
            )
            InEventType.CLICK -> eventHandler.onOAMenuClickEvent(appId,
                OAMenuClickEvent(xml, rootDom)
            )
            InEventType.VIEW -> eventHandler.onOAMenuViewEvent(appId,
                OAMenuViewEvent(xml, rootDom)
            )
            InEventType.SCAN_CODE_PUSH ->
                eventHandler.onOAMenuScanCodePushEvent(appId,
                    OAMenuScanCodePushEvent(xml, rootDom)
                )
            InEventType.SCAN_CODE_WAIT_MSG -> eventHandler.onOAMenuScanCodeWaitEvent(appId,
                OAMenuScanCodeWaitEvent(xml, rootDom)
            )
            InEventType.PIC_SYS_PHOTO -> eventHandler.onOAMenuPhotoEvent(appId,
                OAMenuPhotoEvent(xml, rootDom)
            )
            InEventType.PIC_PHOTO_OR_ALBUM -> eventHandler.onOAMenuPhotoOrAlbumEvent(appId,
                OAMenuPhotoOrAlbumEvent(xml, rootDom)
            )
            InEventType.PIC_WEIXIN -> eventHandler.onOAMenuOAAlbumEvent(appId,
                OAMenuOAAlbumEvent(xml, rootDom)
            )
            InEventType.LOCATION_SELECT -> eventHandler.onOAMenuLocationEvent(appId,
                OAMenuLocationEvent(xml, rootDom)
            )
            InEventType.VIEW_MINI_PROGRAM -> eventHandler.onOAMenuMiniEvent(appId,
                OAMenuMiniEvent(xml, rootDom)
            )
            InEventType.MASS_SEND_JOB_FINISH -> eventHandler.onOAMassSendFinishEvent(appId,
                OAMassSendFinishEvent(xml, rootDom)
            )
            InEventType.TEMPLATE_SEND_JOB_FINISH -> eventHandler.onOATemplateSendJobFinish(appId,
                OATemplateSendJobFinish(xml, rootDom)
            )
            else -> eventHandler.onDispatch(appId, agentId, xml, rootDom, eventType)?: eventHandler.onDefault(appId, WxXmlEvent(xml, rootDom))
        }
    }
}