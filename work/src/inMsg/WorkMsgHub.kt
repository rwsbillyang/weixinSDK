package com.github.rwsbillyang.wxSDK.work.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import javax.xml.stream.XMLEventReader


/**
 * xml消息事件通知的解包、解析、分发处理
 * */
open class WorkMsgHub(
    private val msgHandler: IWorkMsgHandler,
    private val eventHandler: IWorkEventHandler,
    wxBizMsgCrypt: WXBizMsgCrypt
): WxMsgHub(wxBizMsgCrypt) {

    override fun dispatchMsg(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        return when(base.msgType){
            MsgType.TEXT -> msgHandler.onTextMsg(
                WorkTextMsg(base).apply { read(reader) }
            )
            MsgType.IMAGE -> msgHandler.onImgMsg(
                WorkImgMSg(base).apply { read(reader) }
            )
            MsgType.VOICE -> msgHandler.onVoiceMsg(
                WorkVoiceMsg(base).apply { read(reader) }
            )
            MsgType.VIDEO -> msgHandler.onVideoMsg(
                WorkVideoMsg(base).apply { read(reader) }
            )

            MsgType.LOCATION -> msgHandler.onLocationMsg(
                WorkLocationMsg(base).apply { read(reader) }
            )
            MsgType.LINK -> msgHandler.onLinkMsg(
                WorkLinkMsg(base).apply { read(reader) }
            )
            else -> msgHandler.onDispatch(reader, base)?: msgHandler.onDefault(WorkBaseMsg(base).apply { read(reader) })
        }
    }

    override fun dispatchEvent(reader: XMLEventReader, base: BaseInfo): ReBaseMSg?{
        val baseEvent = WorkBaseEvent(base).apply { read(reader) }
        return when (baseEvent.event) {
            InEventType.SUBSCRIBE -> eventHandler.onSubscribeEvent(
                WorkSubscribeEvent(base).apply { read(reader) }
            )
            InEventType.UNSUBSCRIBE -> eventHandler.onUnsubscribeEvent(
                WorkUnsubscribeEvent(base).apply { read(reader) }
            )
            InEventType.LOCATION -> eventHandler.onLocationEvent(
                WorkLocationEvent(base).apply { read(reader) }
            )
            InEventType.CLICK -> eventHandler.onMenuClickEvent(
                WorkMenuClickEvent(base).apply { read(reader) }
            )
            InEventType.VIEW -> eventHandler.onMenuViewEvent(
                WorkMenuViewEvent(base).apply { read(reader) }
            )
            InEventType.SCAN_CODE_PUSH ->
                eventHandler.onMenuScanCodePushEvent(
                    WorkMenuScanCodePushEvent(base).apply { read(reader) }
                )
            InEventType.SCAN_CODE_WAIT_MSG -> eventHandler.onMenuScanCodeWaitEvent(
                WorkMenuScanCodeWaitEvent(base).apply { read(reader) }
            )
            InEventType.PIC_SYS_PHOTO -> eventHandler.onMenuPhotoEvent(
                WorkMenuPhotoEvent(base).apply { read(reader) }
            )
            InEventType.PIC_PHOTO_OR_ALBUM -> eventHandler.onMenuPhotoOrAlbumEvent(
                WorkMenuPhotoOrAlbumEvent(base).apply { read(reader) }
            )
            InEventType.PIC_WEIXIN -> eventHandler.onMenuWorkAlbumEvent(
                WorkMenuWorkAlbumEvent(base).apply { read(reader) }
            )
            InEventType.LOCATION_SELECT -> eventHandler.onMenuLocationEvent(
                WorkMenuLocationEvent(base).apply { read(reader) }
            )

            WorkBaseEvent.ENTER_AGENT -> eventHandler.onEnterAgent(
                WorkEnterAgent(base).apply { read(reader) }
            )
            WorkBaseEvent.BATCH_JOB_RESULT -> eventHandler.onBatchJobResultEvent(
                WorkBatchJobResultEvent(base).apply { read(reader) }
            )
            WorkBaseEvent.OPEN_APPROVAL_CHANGE -> eventHandler.onApprovalStatusChangeEvent(
                WorkApprovalStatusChangeEvent(base).apply { read(reader) }
            )
            WorkBaseEvent.TASK_CARD_CLICK -> eventHandler.onTaskCardClickEvent(
                WorkTaskCardClickEvent(base).apply { read(reader) }
            )
            WorkBaseEvent.CHANGE_CONTACT ->{
                val e = WorkChangeContactEvent(base).apply { read(reader) }
                when(e.changeType){
                    WorkChangeContactEvent.CREATE_PARTY -> eventHandler.onPartyCreateEvent(
                        WorkPartyCreateEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_PARTY -> eventHandler.onPartyUpdateEvent(
                        WorkPartyUpdateEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.DELETE_PARTY -> eventHandler.onPartyDelEvent(
                        WorkPartyDelEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_USER -> eventHandler.onUserUpdateEvent(
                        WorkUserUpdateEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.CREATE_USER -> eventHandler.onUserCreateEvent(
                        WorkUserCreateEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.DELETE_USER -> eventHandler.onUserDelEvent(
                        WorkUserDelEvent(base).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_TAG -> eventHandler.onTagUpdateEvent(
                        WorkTagUpdateEvent(base).apply { read(reader) }
                    )
                    else -> {
                        log.warn("not support changeType: ${e.changeType}")
                        eventHandler.onDefault(e)
                    }
                }
            }
            else -> {
                log.warn("not support event: ${baseEvent.event}")
                eventHandler.onDispatch(reader, base)?: eventHandler.onDefault(baseEvent)
            }
        }
    }

}