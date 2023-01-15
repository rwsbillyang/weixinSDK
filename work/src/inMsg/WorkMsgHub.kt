package com.github.rwsbillyang.wxSDK.work.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.WorkMulti
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Element

/**
 * xml消息事件通知的解包、解析、分发处理
 * */
open class WorkMsgHub(
    wxBizMsgCrypt: WXBizMsgCrypt,
    val workMsgHandler: IWorkMsgHandler? = null,
    val workEventHandler: IWorkEventHandler? = null
): WxMsgHub(wxBizMsgCrypt) {
    private val log: Logger = LoggerFactory.getLogger("WorkMsgHub")
    override fun dispatchMsg(appId: String, agentId: String?, xml: String, rootDom: Element, msgType: String?): ReBaseMSg?{
        val msgHandler = workMsgHandler?: WorkMulti.defaultWorkMsgHandler ?: return null
        return when(msgType){
            MsgType.TEXT -> msgHandler.onTextMsg(appId, agentId,
                WorkTextMsg(xml, rootDom)
            )
            MsgType.IMAGE -> msgHandler.onImgMsg(appId, agentId,
                WorkImgMSg(xml, rootDom)
            )
            MsgType.VOICE -> msgHandler.onVoiceMsg(appId, agentId,
                WorkVoiceMsg(xml, rootDom)
            )
            MsgType.VIDEO -> msgHandler.onVideoMsg(appId, agentId,
                WorkVideoMsg(xml, rootDom)
            )

            MsgType.LOCATION -> msgHandler.onLocationMsg(appId, agentId,
                WorkLocationMsg(xml, rootDom)
            )
            MsgType.LINK -> msgHandler.onLinkMsg(appId, agentId,
                WorkLinkMsg(xml, rootDom)
            )
            else -> msgHandler.onDispatch(appId, agentId, xml, rootDom, msgType)?: msgHandler.onDefault(appId, agentId, WxWorkBaseMsg(xml, rootDom))
        }
    }

    /**
     * TODO: 某些event里面的数据与文档中不一致，导致值为空，需根据实际情况来读取
     * */
    override fun dispatchEvent(appId: String, agentId: String?, xml: String, rootDom: Element, eventType: String?): ReBaseMSg?{
        val eventHandler = workEventHandler?: WorkMulti.defaultWorkEventHandler ?: return null

        return when (eventType) {
            WorkEventType.WxkfMsgEvent -> eventHandler.onWxkfMsgEvent(appId, WxkfEvent(xml, rootDom))

            InEventType.SUBSCRIBE -> eventHandler.onSubscribeEvent(appId, agentId,
                WorkSubscribeEvent(xml, rootDom) //无需额外数据，故不需再读
            )
            InEventType.UNSUBSCRIBE -> eventHandler.onUnsubscribeEvent(appId, agentId,
                WorkUnsubscribeEvent(xml, rootDom)//无需额外数据，故不需再读
            )
            WorkEventType.ENTER_AGENT -> eventHandler.onEnterAgent(appId, agentId,
                WorkEnterAgent(xml, rootDom)
            )
            InEventType.LOCATION -> eventHandler.onLocationEvent(appId, agentId,
                WorkLocationEvent(xml, rootDom)
            )
            InEventType.CLICK -> eventHandler.onMenuClickEvent(appId, agentId,
                WorkMenuClickEvent(xml, rootDom)
            )
            InEventType.VIEW -> eventHandler.onMenuViewEvent(appId, agentId,
                WorkMenuViewEvent(xml, rootDom)
            )
            InEventType.SCAN_CODE_PUSH ->
                eventHandler.onMenuScanCodePushEvent(appId, agentId,
                    WorkMenuScanCodePushEvent(xml, rootDom)
                )
            InEventType.SCAN_CODE_WAIT_MSG -> eventHandler.onMenuScanCodeWaitEvent(appId, agentId,
                WorkMenuScanCodeWaitEvent(xml, rootDom)
            )
            InEventType.PIC_SYS_PHOTO -> eventHandler.onMenuPhotoEvent(appId, agentId,
                WorkMenuPhotoEvent(xml, rootDom)
            )
            InEventType.PIC_PHOTO_OR_ALBUM -> eventHandler.onMenuPhotoOrAlbumEvent(appId, agentId,
                WorkMenuPhotoOrAlbumEvent(xml, rootDom)
            )
            InEventType.PIC_WEIXIN -> eventHandler.onMenuWorkAlbumEvent(appId, agentId,
                WorkMenuWorkAlbumEvent(xml, rootDom)
            )
            InEventType.LOCATION_SELECT -> eventHandler.onMenuLocationEvent(appId, agentId,
                WorkMenuLocationEvent(xml, rootDom)
            )


            WorkEventType.BATCH_JOB_RESULT -> eventHandler.onBatchJobResultEvent(appId, agentId,
                WorkBatchJobResultEvent(xml, rootDom)
            )
            WorkEventType.OPEN_APPROVAL_CHANGE -> eventHandler.onApprovalStatusChangeEvent(appId, agentId,
                WorkApprovalStatusChangeEvent(xml, rootDom)
            )
            WorkEventType.TASK_CARD_CLICK -> eventHandler.onTaskCardClickEvent(appId, agentId,
                WorkTaskCardClickEvent(xml, rootDom)
            )
            WorkEventType.CHANGE_CONTACT ->{
                when(val changeType = rootDom.get("ChangeType")){
                    WorkChangeContactEvent.CREATE_PARTY -> eventHandler.onPartyCreateEvent(appId, agentId,
                        WorkPartyCreateEvent(xml, rootDom)
                    )
                    WorkChangeContactEvent.UPDATE_PARTY -> eventHandler.onPartyUpdateEvent(appId, agentId,
                        WorkPartyUpdateEvent(xml, rootDom)
                    )
                    WorkChangeContactEvent.DELETE_PARTY -> eventHandler.onPartyDelEvent(appId, agentId,
                        WorkPartyDelEvent(xml, rootDom)
                    )
                    WorkChangeContactEvent.UPDATE_USER -> eventHandler.onUserUpdateEvent(appId, agentId,
                        WorkUserUpdateEvent(xml, rootDom)
                    )
                    WorkChangeContactEvent.CREATE_USER -> eventHandler.onUserCreateEvent(appId, agentId,
                        WorkUserCreateEvent(xml, rootDom)
                    )
                    WorkChangeContactEvent.DELETE_USER -> eventHandler.onUserDelEvent(appId, agentId,
                        WorkUserDelEvent(xml, rootDom)
                    )
                    WorkChangeContactEvent.UPDATE_TAG -> eventHandler.onTagUpdateEvent(appId, agentId,
                        WorkTagUpdateEvent(xml, rootDom)
                    )
                    else -> {
                        log.warn("not support changeType: ${changeType}")
                        eventHandler.onDefault(appId, agentId, WorkChangeContactEvent(xml, rootDom))
                    }
                }
            }
            WorkEventType.EXTERNAL_CONTACT_CHANGE ->{
                when(val changeType = rootDom.get("ChangeType")){
                    WorkEventType.EXTERNAL_CONTACT_ADD -> eventHandler.onExternalContactAddEvent(appId, agentId, ExternalContactAddEvent(xml, rootDom))
                    WorkEventType.EXTERNAL_CONTACT_ADD_HALF -> eventHandler.onExternalContactHalfAddEvent(appId, agentId, ExternalContactAddEvent(xml, rootDom))
                    WorkEventType.EXTERNAL_CONTACT_EDIT -> eventHandler.onExternalContactUpdateEvent(appId, agentId, ExternalContactUpdateEvent(xml, rootDom))
                    WorkEventType.EXTERNAL_CONTACT_DEL -> eventHandler.onExternalContactDelEvent(appId, agentId, ExternalContactDelEvent(xml, rootDom))
                    WorkEventType.EXTERNAL_CONTACT_DEL_FOLLOW_USER -> eventHandler.onExternalContactDelFollowEvent(appId, agentId, ExternalContactUpdateEvent(xml, rootDom))
                    WorkEventType.EXTERNAL_CONTACT_TRANSFER_FAIL -> eventHandler.onExternalContactTransferFailEvent(appId, agentId, ExternalContactTransferFailEvent(xml, rootDom))
                    else -> {
                        log.warn("not support EXTERNAL_CHAT_CHANGE changeType: ${changeType}")
                        eventHandler.onDefault(appId, agentId, ExternalContactChangeEvent(xml, rootDom))
                    }
                }
            }
            WorkEventType.EXTERNAL_CHAT_CHANGE ->{
                val e = ExternalChatChangeEvent(xml, rootDom)
                when(e.changeType){
                    WorkEventType.EXTERNAL_CHAT_ADD -> eventHandler.onExternalGroupChatCreateEvent(appId, agentId, e)
                    WorkEventType.EXTERNAL_CHAT_EDIT -> eventHandler.onExternalGroupChatUpdateEvent(appId, agentId, ExternalChatUpdateEvent(xml, rootDom))
                    WorkEventType.EXTERNAL_CHAT_DEL -> eventHandler.onExternalGroupChatDelEvent(appId, agentId, e)
                    else -> {
                        log.warn("not support EXTERNAL_CHAT_CHANGE changeType: ${e.changeType}")
                        eventHandler.onDefault(appId, agentId, ExternalChatChangeEvent(xml, rootDom))
                    }
                }
            }
            null -> {
                log.warn("not read event value? appId=$appId, agentID=${agentId}, xml=$xml")
                null
            }
            else -> {
                log.warn("not support event: ${eventType}")
                eventHandler.onDispatch(appId, agentId, xml, rootDom, eventType)?: eventHandler.onDefault(appId, agentId, WxXmlEvent(xml, rootDom))
            }
        }
    }
}