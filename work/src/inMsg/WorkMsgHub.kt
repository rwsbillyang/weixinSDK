package com.github.rwsbillyang.wxSDK.work.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import javax.xml.stream.XMLEventReader


/**
 * xml消息事件通知的解包、解析、分发处理
 * */
open class WorkMsgHub(
    private val msgHandler: IWorkMsgHandler?,
    private val eventHandler: IWorkEventHandler?,
    wxBizMsgCrypt: WXBizMsgCrypt
): WxMsgHub(wxBizMsgCrypt) {

    override fun dispatchMsg(appId: String, agentId: Int?, reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        if(msgHandler == null) return null
        return when(base.msgType){
            MsgType.TEXT -> msgHandler.onTextMsg(appId, agentId,
                WorkTextMsg(base).apply { read(reader) }
            )
            MsgType.IMAGE -> msgHandler.onImgMsg(appId, agentId,
                WorkImgMSg(base).apply { read(reader) }
            )
            MsgType.VOICE -> msgHandler.onVoiceMsg(appId, agentId,
                WorkVoiceMsg(base).apply { read(reader) }
            )
            MsgType.VIDEO -> msgHandler.onVideoMsg(appId, agentId,
                WorkVideoMsg(base).apply { read(reader) }
            )

            MsgType.LOCATION -> msgHandler.onLocationMsg(appId, agentId,
                WorkLocationMsg(base).apply { read(reader) }
            )
            MsgType.LINK -> msgHandler.onLinkMsg(appId, agentId,
                WorkLinkMsg(base).apply { read(reader) }
            )
            else -> msgHandler.onDispatch(appId, agentId,reader, base)?: msgHandler.onDefault(appId, agentId,WorkBaseMsg(base).apply { read(reader) })
        }
    }

    /**
     * TODO: 某些event里面的数据与文档中不一致，导致值为空，需根据实际情况来读取
     * */
    override fun dispatchEvent(appId: String, agentId: Int?, reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        if(eventHandler == null) return null
        val baseEvent = WorkBaseEvent(base).apply { read(reader) }
        return when (baseEvent.event) {
            InEventType.SUBSCRIBE -> eventHandler.onSubscribeEvent(appId, agentId,
                WorkSubscribeEvent(baseEvent).apply { read(reader) }
            )
            InEventType.UNSUBSCRIBE -> eventHandler.onUnsubscribeEvent(appId, agentId,
                WorkUnsubscribeEvent(baseEvent).apply { read(reader) }
            )
            WorkBaseEvent.ENTER_AGENT -> eventHandler.onEnterAgent(appId, agentId,
                WorkEnterAgent(baseEvent).apply { read(reader) }
            )
            InEventType.LOCATION -> eventHandler.onLocationEvent(appId, agentId,
                WorkLocationEvent(base).apply { read(reader) }
            )
            InEventType.CLICK -> eventHandler.onMenuClickEvent(appId, agentId,
                WorkMenuClickEvent(base).apply { read(reader) }
            )
            InEventType.VIEW -> eventHandler.onMenuViewEvent(appId, agentId,
                WorkMenuViewEvent(base).apply { read(reader) }
            )
            InEventType.SCAN_CODE_PUSH ->
                eventHandler.onMenuScanCodePushEvent(appId, agentId,
                    WorkMenuScanCodePushEvent(base).apply { read(reader) }
                )
            InEventType.SCAN_CODE_WAIT_MSG -> eventHandler.onMenuScanCodeWaitEvent(appId, agentId,
                WorkMenuScanCodeWaitEvent(base).apply { read(reader) }
            )
            InEventType.PIC_SYS_PHOTO -> eventHandler.onMenuPhotoEvent(appId, agentId,
                WorkMenuPhotoEvent(base).apply { read(reader) }
            )
            InEventType.PIC_PHOTO_OR_ALBUM -> eventHandler.onMenuPhotoOrAlbumEvent(appId, agentId,
                WorkMenuPhotoOrAlbumEvent(base).apply { read(reader) }
            )
            InEventType.PIC_WEIXIN -> eventHandler.onMenuWorkAlbumEvent(appId, agentId,
                WorkMenuWorkAlbumEvent(base).apply { read(reader) }
            )
            InEventType.LOCATION_SELECT -> eventHandler.onMenuLocationEvent(appId, agentId,
                WorkMenuLocationEvent(base).apply { read(reader) }
            )


            WorkBaseEvent.BATCH_JOB_RESULT -> eventHandler.onBatchJobResultEvent(appId, agentId,
                WorkBatchJobResultEvent(base).apply { read(reader) }
            )
            WorkBaseEvent.OPEN_APPROVAL_CHANGE -> eventHandler.onApprovalStatusChangeEvent(appId, agentId,
                WorkApprovalStatusChangeEvent(base).apply { read(reader) }
            )
            WorkBaseEvent.TASK_CARD_CLICK -> eventHandler.onTaskCardClickEvent(appId, agentId,
                WorkTaskCardClickEvent(base).apply { read(reader) }
            )
            WorkBaseEvent.CHANGE_CONTACT ->{
                val e = WorkChangeContactEvent(base).apply { read(reader) }
                when(e.changeType){
                    WorkChangeContactEvent.CREATE_PARTY -> eventHandler.onPartyCreateEvent(appId, agentId,
                        WorkPartyCreateEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_PARTY -> eventHandler.onPartyUpdateEvent(appId, agentId,
                        WorkPartyUpdateEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.DELETE_PARTY -> eventHandler.onPartyDelEvent(appId, agentId,
                        WorkPartyDelEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_USER -> eventHandler.onUserUpdateEvent(appId, agentId,
                        WorkUserUpdateEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.CREATE_USER -> eventHandler.onUserCreateEvent(appId, agentId,
                        WorkUserCreateEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.DELETE_USER -> eventHandler.onUserDelEvent(appId, agentId,
                        WorkUserDelEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_TAG -> eventHandler.onTagUpdateEvent(appId, agentId,
                        WorkTagUpdateEvent(base).apply { read(reader) }
                    )
                    else -> {
                        log.warn("not support changeType: ${e.changeType}")
                        eventHandler.onDefault(appId, agentId, e)
                    }
                }
            }
            else -> {
                log.warn("not support event: ${baseEvent.event}")
                eventHandler.onDispatch(appId, agentId,reader, base)?: eventHandler.onDefault(appId, agentId, baseEvent)
            }
        }
    }

}