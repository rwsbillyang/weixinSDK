package com.github.rwsbillyang.wxSDK.work.inMsg

import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.WorkMulti
import javax.xml.stream.XMLEventReader


/**
 * xml消息事件通知的解包、解析、分发处理
 * */
open class WorkMsgHub(
    wxBizMsgCrypt: WXBizMsgCrypt,
    val workMsgHandler: IWorkMsgHandler? = null,
    val workEventHandler: IWorkEventHandler? = null
): WxMsgHub(wxBizMsgCrypt) {

    override fun dispatchMsg(appId: String, agentId: String?, reader: XMLEventReader, baseInfo: BaseInfo): ReBaseMSg?{
        val msgHandler = workMsgHandler?: WorkMulti.defaultWorkMsgHandler
        if(msgHandler == null) return null
        return when(baseInfo.msgType){
            MsgType.TEXT -> msgHandler.onTextMsg(appId, agentId,
                WorkTextMsg(baseInfo).apply { read(reader) }
            )
            MsgType.IMAGE -> msgHandler.onImgMsg(appId, agentId,
                WorkImgMSg(baseInfo).apply { read(reader) }
            )
            MsgType.VOICE -> msgHandler.onVoiceMsg(appId, agentId,
                WorkVoiceMsg(baseInfo).apply { read(reader) }
            )
            MsgType.VIDEO -> msgHandler.onVideoMsg(appId, agentId,
                WorkVideoMsg(baseInfo).apply { read(reader) }
            )

            MsgType.LOCATION -> msgHandler.onLocationMsg(appId, agentId,
                WorkLocationMsg(baseInfo).apply { read(reader) }
            )
            MsgType.LINK -> msgHandler.onLinkMsg(appId, agentId,
                WorkLinkMsg(baseInfo).apply { read(reader) }
            )
            else -> msgHandler.onDispatch(appId, agentId,reader, baseInfo)?: msgHandler.onDefault(appId, agentId,WorkBaseMsg(baseInfo).apply { read(reader) })
        }
    }

    /**
     * TODO: 某些event里面的数据与文档中不一致，导致值为空，需根据实际情况来读取
     * */
    override fun dispatchEvent(appId: String, agentId: String?, reader: XMLEventReader, baseInfo: BaseInfo): ReBaseMSg?{
        val eventHandler = workEventHandler?: WorkMulti.defaultWorkEventHandler
        if(eventHandler == null) return null
        //若使用WxBaseEvent可能event字段在后面，而agentID在前面，即使再去读，也读取不到了。
        //这里使用AgentEvent，比WxBaseEvent多读取一个agentId字段，即使没有此字段，便让其为空罢了
        val agentEvent = AgentEvent(baseInfo).apply { read(reader) } // 读取agentId和event

        return when (agentEvent.event) {
            WorkEventType.WxkfMsgEvent -> eventHandler.onWxkfMsgEvent(appId,  WxkfEvent(baseInfo).apply { read(reader) } )

            InEventType.SUBSCRIBE -> eventHandler.onSubscribeEvent(appId, agentId,
                WorkSubscribeEvent(baseInfo, agentEvent) //无需额外数据，故不需再读
            )
            InEventType.UNSUBSCRIBE -> eventHandler.onUnsubscribeEvent(appId, agentId,
                WorkUnsubscribeEvent(baseInfo, agentEvent)//无需额外数据，故不需再读
            )
            WorkEventType.ENTER_AGENT -> eventHandler.onEnterAgent(appId, agentId,
                WorkEnterAgent(baseInfo, agentEvent).apply { read(reader) }
            )
            InEventType.LOCATION -> eventHandler.onLocationEvent(appId, agentId,
                WorkLocationEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            InEventType.CLICK -> eventHandler.onMenuClickEvent(appId, agentId,
                WorkMenuClickEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            InEventType.VIEW -> eventHandler.onMenuViewEvent(appId, agentId,
                WorkMenuViewEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            InEventType.SCAN_CODE_PUSH ->
                eventHandler.onMenuScanCodePushEvent(appId, agentId,
                    WorkMenuScanCodePushEvent(baseInfo, agentEvent).apply { read(reader) }
                )
            InEventType.SCAN_CODE_WAIT_MSG -> eventHandler.onMenuScanCodeWaitEvent(appId, agentId,
                WorkMenuScanCodeWaitEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            InEventType.PIC_SYS_PHOTO -> eventHandler.onMenuPhotoEvent(appId, agentId,
                WorkMenuPhotoEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            InEventType.PIC_PHOTO_OR_ALBUM -> eventHandler.onMenuPhotoOrAlbumEvent(appId, agentId,
                WorkMenuPhotoOrAlbumEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            InEventType.PIC_WEIXIN -> eventHandler.onMenuWorkAlbumEvent(appId, agentId,
                WorkMenuWorkAlbumEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            InEventType.LOCATION_SELECT -> eventHandler.onMenuLocationEvent(appId, agentId,
                WorkMenuLocationEvent(baseInfo, agentEvent).apply { read(reader) }
            )


            WorkEventType.BATCH_JOB_RESULT -> eventHandler.onBatchJobResultEvent(appId, agentId,
                WorkBatchJobResultEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            WorkEventType.OPEN_APPROVAL_CHANGE -> eventHandler.onApprovalStatusChangeEvent(appId, agentId,
                WorkApprovalStatusChangeEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            WorkEventType.TASK_CARD_CLICK -> eventHandler.onTaskCardClickEvent(appId, agentId,
                WorkTaskCardClickEvent(baseInfo, agentEvent).apply { read(reader) }
            )
            WorkEventType.CHANGE_CONTACT ->{
                val e = WorkChangeContactEvent(baseInfo, agentEvent).apply { read(reader) }
                when(e.changeType){
                    WorkChangeContactEvent.CREATE_PARTY -> eventHandler.onPartyCreateEvent(appId, agentId,
                        WorkPartyCreateEvent(baseInfo, agentEvent).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_PARTY -> eventHandler.onPartyUpdateEvent(appId, agentId,
                        WorkPartyUpdateEvent(baseInfo, agentEvent).apply { read(reader) }
                    )
                    WorkChangeContactEvent.DELETE_PARTY -> eventHandler.onPartyDelEvent(appId, agentId,
                        WorkPartyDelEvent(baseInfo, agentEvent).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_USER -> eventHandler.onUserUpdateEvent(appId, agentId,
                        WorkUserUpdateEvent(baseInfo, agentEvent).apply { read(reader) }
                    )
                    WorkChangeContactEvent.CREATE_USER -> eventHandler.onUserCreateEvent(appId, agentId,
                        WorkUserCreateEvent(baseInfo, agentEvent).apply { read(reader) }
                    )
                    WorkChangeContactEvent.DELETE_USER -> eventHandler.onUserDelEvent(appId, agentId,
                        WorkUserDelEvent(baseInfo, agentEvent).apply { read(reader) }
                    )
                    WorkChangeContactEvent.UPDATE_TAG -> eventHandler.onTagUpdateEvent(appId, agentId,
                        WorkTagUpdateEvent(baseInfo, agentEvent).apply { read(reader) }
                    )
                    else -> {
                        log.warn("not support changeType: ${e.changeType}")
                        eventHandler.onDefault(appId, agentId, e)
                    }
                }
            }
            WorkEventType.EXTERNAL_CONTACT_CHANGE ->{
                val e = ExternalContactChangeEvent(baseInfo, agentEvent).apply { read(reader) }
                when(e.changeType){
                    WorkEventType.EXTERNAL_CONTACT_ADD -> eventHandler.onExternalContactAddEvent(appId, agentId, ExternalContactAddEvent(baseInfo, agentEvent).apply { read(reader) })
                    WorkEventType.EXTERNAL_CONTACT_ADD_HALF -> eventHandler.onExternalContactHalfAddEvent(appId, agentId, ExternalContactAddEvent(baseInfo, agentEvent).apply { read(reader) })
                    WorkEventType.EXTERNAL_CONTACT_EDIT -> eventHandler.onExternalContactUpdateEvent(appId, agentId, ExternalContactUpdateEvent(baseInfo, agentEvent).apply { read(reader) })
                    WorkEventType.EXTERNAL_CONTACT_DEL -> eventHandler.onExternalContactDelEvent(appId, agentId, ExternalContactDelEvent(baseInfo, agentEvent).apply { read(reader) })
                    WorkEventType.EXTERNAL_CONTACT_DEL_FOLLOW_USER -> eventHandler.onExternalContactDelFollowEvent(appId, agentId, ExternalContactUpdateEvent(baseInfo, agentEvent).apply { read(reader) })
                    WorkEventType.EXTERNAL_CONTACT_TRANSFER_FAIL -> eventHandler.onExternalContactTransferFailEvent(appId, agentId, ExternalContactTransferFailEvent(baseInfo, agentEvent).apply { read(reader) })
                    else -> {
                        log.warn("not support EXTERNAL_CHAT_CHANGE changeType: ${e.changeType}")
                        eventHandler.onDefault(appId, agentId, e)
                    }
                }
            }
            WorkEventType.EXTERNAL_CHAT_CHANGE ->{
                val e = ExternalChatChangeEvent(baseInfo, agentEvent).apply { read(reader) }
                when(e.changeType){
                    WorkEventType.EXTERNAL_CHAT_ADD -> eventHandler.onExternalGroupChatCreateEvent(appId, agentId, e)
                    WorkEventType.EXTERNAL_CHAT_EDIT -> eventHandler.onExternalGroupChatUpdateEvent(appId, agentId, ExternalChatUpdateEvent(baseInfo, agentEvent).apply { read(reader) })
                    WorkEventType.EXTERNAL_CHAT_DEL -> eventHandler.onExternalGroupChatDelEvent(appId, agentId, e)
                    else -> {
                        log.warn("not support EXTERNAL_CHAT_CHANGE changeType: ${e.changeType}")
                        eventHandler.onDefault(appId, agentId, e)
                    }
                }
            }
            null -> {
                log.warn("not read event value? appId=$appId, agentEvent.agentID=${agentEvent.agentId}")
                null
            }
            else -> {
                log.warn("not support event: ${agentEvent.event}")
                eventHandler.onDispatch(appId, agentId,reader, baseInfo)?: eventHandler.onDefault(appId, agentId, agentEvent)
            }
        }
    }
}