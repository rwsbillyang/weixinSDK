package com.github.rwsbillyang.wxSDK.work.inMsg



import com.github.rwsbillyang.wxSDK.msg.*
import com.github.rwsbillyang.wxSDK.work.isv.AgentInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.xml.stream.XMLEventReader

/*
* https://work.weixin.qq.com/api/doc/90000/90135/90240
* 开启接收消息模式后，可以配置接收事件消息。
* 当企业成员通过企业微信APP或微工作台（原企业号）触发进入应用、上报地理位置、点击菜单等事件时，企业微信会将这些事件消息发送给企业后台。
*
* 注：以下出现的xml包仅是接收的消息包中的Encrypt参数解密后的内容说明
* */

object WorkEventType{
    const val ENTER_AGENT = "enter_agent"
    const val BATCH_JOB_RESULT = "batch_job_result"
    const val CHANGE_CONTACT = "change_contact"
    const val OPEN_APPROVAL_CHANGE = "open_approval_change"
    const val TASK_CARD_CLICK = "taskcard_click"


    const val EXTERNAL_CONTACT_CHANGE = "change_external_contact"
    const val EXTERNAL_CONTACT_ADD = "add_external_contact"
    const val EXTERNAL_CONTACT_EDIT = "edit_external_contact"
    const val EXTERNAL_CONTACT_DEL = "del_external_contact"
    //外部联系人添加了配置了客户联系功能且开启了免验证的成员时（此时成员尚未确认添加对方为好友），回调该事件
    const val EXTERNAL_CONTACT_ADD_HALF = "add_half_external_contact"
    const val EXTERNAL_CONTACT_DEL_FOLLOW_USER = "del_follow_user" //成员被外部联系人删除时，即被客户删除拉黑
    const val EXTERNAL_CONTACT_TRANSFER_FAIL = "transfer_fail"

    const val EXTERNAL_CHAT_CHANGE = "change_external_chat"
    const val EXTERNAL_CHAT_ADD = "create"
    const val EXTERNAL_CHAT_EDIT = "update"
    const val EXTERNAL_CHAT_DEL = "dismiss"
}

/**
 * 添加了AgentID的事件基类
 *
 * 注意：各Event的构建参数类型尽量使用读取较多共性数据的类，自己只需要读取较少的剩下的数据。
 * 若有未覆盖到的成员，将导致其为空
 * @param baseInfo 已经读取的xml构成的BaseInfo
 * */
open class AgentEvent(baseInfo: BaseInfo): BaseInfo(baseInfo.toUserName, baseInfo.fromUserName,baseInfo.createTime,  baseInfo.msgType)
{
    companion object{
        val log: Logger = LoggerFactory.getLogger("AgentEvent")
    }
    var event: String? = null
    var agentId: String? = null

    //TODO: 读取到Event字段后退出，否则一直读取下去，顺带保存一下AgentId字段；若Event字段排在后面，将导致其前面的字段信息丢失
    //因 成员关注及取消关注事件 的event在agentId之后，，直到读取到event之后才退出，这样导致其它字段读取机会丧失
    open fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext()) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                count++
                when(e.asStartElement().name.toString()){
                    "Event" -> {
                        event = reader.elementText
                        break
                    }
                    "AgentID" -> { //目的在于兼容：成员关注及取消关注事件
                        agentId = reader.elementText
                    }
                }
            }
        }
        if(count > 2){
            log.warn("read $count times, maybe some values of fields miss")
        }
    }
}

/**
 * 关注事件
 *
 * 小程序在管理端开启接收消息配置后，也可收到关注/取消关注事件
 * <xml><ToUserName><![CDATA[wwb096af219dea3f1c]]></ToUserName><FromUserName><![CDATA[BiDandan]]></FromUserName><CreateTime>1631359354</CreateTime><MsgType><![CDATA[event]]></MsgType><AgentID>1000002</AgentID><Event><![CDATA[subscribe]]></Event></xml>
 * 本事件触发时机为：
 * 成员已经加入企业，管理员添加成员到应用可见范围(或移除可见范围)时
 * 成员已经在应用可见范围，成员加入(或退出)企业时
 * 事件类型，subscribe(关注)、unsubscribe(取消关注)
 *
 * @param agentEvent 已经读取的xml构成的agentEvent
 * */
class WorkSubscribeEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
}

/**
 * 取消关注事件
 *
 * 事件类型，subscribe(关注)、unsubscribe(取消关注)
 * <xml><ToUserName><![CDATA[wwb096af219dea3f1c]]></ToUserName><FromUserName><![CDATA[BiDandan]]></FromUserName><CreateTime>1631610483</CreateTime><MsgType><![CDATA[event]]></MsgType><AgentID>1000002</AgentID><Event><![CDATA[unsubscribe]]></Event></xml>
 * */
class WorkUnsubscribeEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
}

/**
 * 进入应用
 * 本事件在成员进入企业微信的应用时触发
 * @property eventKey EventKey	事件KEY值，此事件该值为空
 * */
class WorkEnterAgent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }

    var eventKey: String? = null

    //event字段必须在前两个位置，否则读取不到
    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "EventKey" -> {
                        eventKey = reader.elementText
                        count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText
                         count++
                    }
                }
            }
        }
    }
}



/**
 * 上报地理位置
 *
 * 成员同意上报地理位置后，每次在进入应用会话时都会上报一次地理位置。
 * 企业可以在管理端修改应用是否需要获取地理位置权限。
 *
 * https://work.weixin.qq.com/api/doc/90000/90135/90240#%E4%B8%8A%E6%8A%A5%E5%9C%B0%E7%90%86%E4%BD%8D%E7%BD%AE
 *
 * @property latitude Latitude	地理位置纬度
 * @property longitude    地理位置经度
 * @property precision Precision	地理位置精度
 * //@property agentId AgentID 企业应用的id，整型。可在应用的设置页面查看
 * @property appType AppType app类型，在企业微信固定返回wxwork，在微信不返回该字段
 * */
open class WorkLocationEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }

    var latitude: Float? = null
    var longitude: Float?= null
    var precision: Float?= null
    val appType = "wxwork"

    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 4) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "Latitude" -> {
                        latitude = reader.elementText?.toFloat(); count++
                    }
                    "Longitude" -> {
                        longitude = reader.elementText?.toFloat(); count++
                    }
                    "Precision" -> {
                        precision = reader.elementText?.toFloat(); count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText
                        count++
                    }
                }
            }
        }
        
    }
}

/**
 * @param id JobId	异步任务id，最大长度为64字符
 * @param type  JobType	操作类型，字符串，目前分别有：sync_user(增量更新成员)、 replace_user(全量覆盖成员）、invite_user(邀请成员关注）、replace_party(全量覆盖部门)
 * @param code ErrCode	返回码
 * @param msg  ErrMsg	对返回码的文本描述内容
 * */
class BatchJob(val id: String?, val type: String?, val code: Int?, val msg: String?)
/**
 * 异步任务完成事件推送
 *
 * 本事件是成员在使用异步任务接口时，用于接收任务执行完毕的结果通知。
 * */
class WorkBatchJobResultEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId //实际无此数据
    }

    var batchJob: BatchJob? = null
    override fun read(reader: XMLEventReader) {
        var id: String? = null
        var type: String? = null
        var code: Int?= null
        var msg: String? = null
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "BatchJob" -> {
                        var count2 = 0
                        while (reader.hasNext() && count2 < 4) {
                            val e = reader.nextEvent()
                            if (e.isStartElement) {
                                when(e.asStartElement().name.toString()){
                                    "JobId" ->{ id = reader.elementText; count2++ }
                                    "JobType" ->{ type= reader.elementText; count2++}
                                    "ErrCode" ->{ code= reader.elementText?.toInt(); count2++}
                                    "ErrMsg" ->{ msg= reader.elementText; count2++}
                                }
                            }
                        }
                        break
                    }
                }
            }
        }
        batchJob = BatchJob(id, type, code, msg)
    }
}



/*
* 菜单事件
*
* 成员点击自定义菜单后，企业微信会把点击事件推送给应用。
* 点击菜单弹出子菜单，不会产生上报。
*
* 企业微信iPhone1.2.2/Android1.2.2版本开始支持菜单事件，旧版本企业微信成员点击后将没有回应，应用不能正常接收到事件推送。
* 自定义菜单可以在管理后台的应用设置界面配置。
* */

/**
 * 点击菜单拉取消息的事件推送
 *
 * 用户点击自定义菜单后，微信会把点击事件推送给开发者，请注意，点击菜单弹出子菜单，不会产生上报。
 *
 * @property eventKey EventKey 事件KEY值，与自定义菜单接口中KEY值对应
 * */
class WorkMenuClickEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
    var eventKey: String? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText; count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText
                        count++
                    }
                }
            }
        }
    }
}

/**
 * 点击菜单拉取消息的事件推送
 *
 * @property eventKey EventKey	事件KEY值，设置的跳转URL
 * //@property menuId MenuID	指菜单ID，如果是个性化菜单，则可以通过这个字段，知道是哪个规则的菜单被点击了
 *
 * */
class WorkMenuViewEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
    var eventKey: String? = null
    //var menuId: String? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText; count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText
                        count++
                    }
                }
            }
        }
    }
}
/**
 * scancode_push：扫码推事件的事件推送
 *
 * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Custom_Menu_Push_Events.html
 *
 * @property eventKey EventKey	事件KEY值，由开发者在创建菜单时设定
 * @property scanType ScanType	扫描类型，一般是qrcode
 * @property scanResult ScanResult	扫描结果，即二维码对应的字符串信息
 * */
open class WorkMenuScanCodePushEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
    var eventKey: String? = null
    var scanType: String? = null
    var scanResult: String? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 3) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText; count++
                    }
                    "ScanCodeInfo" -> {
                        var count2 = 0
                        while (reader.hasNext() && count2 < 2) {
                            val e = reader.nextEvent()
                            if (e.isStartElement) {
                                when(e.asStartElement().name.toString()){
                                    "ScanType" ->{ scanType = reader.elementText; count2++ }
                                    "ScanResult" ->{ scanResult= reader.elementText; count2++}
                                }
                            }
                        }
                        count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText
                        count++
                    }
                }
            }
        }
      
    }
}

/**
 * scancode_waitmsg：扫码推事件且弹出“消息接收中”提示框的事件推送
 *
 * */
class WorkMenuScanCodeWaitEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): WorkMenuScanCodePushEvent(baseInfo, agentEvent)
{
    init {
        event = InEventType.SCAN_CODE_WAIT_MSG
    }
}




/**
 * pic_sysphoto：弹出系统拍照发图的事件推送
 *
 * @property eventKey EventKey	事件KEY值，由开发者在创建菜单时设定
 * @property picsInfo SendPicsInfo 图片的MD5值，开发者若需要，可用于验证接收到图片
 * */
open class WorkMenuPhotoEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
    var eventKey: String? = null
    var sendPicsInfo: SendPicsInfo? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 3) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText
                        count++
                    }
                    "SendPicsInfo" -> {
                        sendPicsInfo = SendPicsInfo.fromXml(reader)
                        count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText
                        count++
                    }
                }
            }
        }
        
    }


}
/**
 * pic_photo_or_album：弹出拍照或者相册发图的事件推送
 *
 * */
class WorkMenuPhotoOrAlbumEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): WorkMenuPhotoEvent(baseInfo, agentEvent)
{
    init {
        event = InEventType.PIC_PHOTO_OR_ALBUM
    }
}


/**
 * pic_weixin：弹出微信相册发图器的事件推送
 *
 * */
class WorkMenuWorkAlbumEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): WorkMenuPhotoEvent(baseInfo,agentEvent)
{
    init {
        event = InEventType.PIC_WEIXIN
    }
}

/**
 * location_select：弹出地理位置选择器的事件推送
 *
 * @property eventKey EventKey	事件KEY值，由开发者在创建菜单时设定
 * @property locationX Location_X	X坐标信息
 * @property locationY Location_Y	Y坐标信息
 * @property scale Scale	精度，可理解为精度或者比例尺、越精细的话 scale越高
 * @property label Label	地理位置的字符串信息
 * @property poiname Poiname	朋友圈POI的名字，可能为空
 * */
class WorkMenuLocationEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
    var eventKey: String? = null
    var locationX: Float? = null
    var locationY: Float? = null
    var scale: Int? = null
    var label: String? = null
    var poiname: String? = null
    val appType = "wxwork"
    /*
    * <EventKey><![CDATA[6]]></EventKey>
        <SendLocationInfo><Location_X><![CDATA[23]]></Location_X>
        <Location_Y><![CDATA[113]]></Location_Y>
        <Scale><![CDATA[15]]></Scale>
        <Label><![CDATA[ 广州市海珠区客村艺苑路 106号]]></Label>
        <Poiname><![CDATA[]]></Poiname>
        </SendLocationInfo>
    * */
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 3) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText; count++
                    }
                    "SendLocationInfo" -> {
                        var count2 = 0
                        while (reader.hasNext() && count2 < 5) {
                            val e = reader.nextEvent()
                            if (e.isStartElement) {
                                when(e.asStartElement().name.toString()){
                                    "Location_X" ->{ locationX = reader.elementText?.toFloat(); count2++ }
                                    "Location_Y" ->{ locationY= reader.elementText?.toFloat(); count2++}
                                    "Scale" ->{ scale = reader.elementText?.toInt(); count2++ }
                                    "Label" ->{ label= reader.elementText; count2++}
                                    "Poiname" ->{ poiname = reader.elementText; count2++ }
                                }
                            }
                        }
                        count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText
                        count++
                    }
                }
            }
        }
        
    }
}

/**
 * TODO: 完善剩余各字段
 * */
class ApprovalInfo(val id: String?, val openSpName: String?)
{
    companion object{
        fun fromXML(reader: XMLEventReader):ApprovalInfo {
            var id: String? = null
            var openSpName: String? = null
            var count = 0
            while (reader.hasNext() && count < 2) {
                val event = reader.nextEvent()
                if (event.isStartElement) {
                    when (event.asStartElement().name.toString()) {
                        "ThirdNo" -> {
                            id = reader.elementText; count++
                        }
                        "OpenSpName" -> {
                            openSpName = reader.elementText; count++
                            count++
                        }
                        //TODO： 其它各字段 ...
                    }
                }
            }
            return ApprovalInfo(id,openSpName)
        }
    }
}
/**
 * 审批状态通知事件
 *
 * 本事件触发时机为：
 * 1.自建/第三方应用调用审批流程引擎发起申请之后，审批状态发生变化时
 * 2.自建/第三方应用调用审批流程引擎发起申请之后，在“审批中”状态，有任意审批人进行审批操作时
 * */
class WorkApprovalStatusChangeEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
    var eventKey: String? = null
    var approvalInfo: ApprovalInfo? = null
    override fun read(reader: XMLEventReader) {
        
        var count = 0
        while (reader.hasNext() && count < 2) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText; count++
                    }
                    "ApprovalInfo" -> {
                        approvalInfo = ApprovalInfo.fromXML(reader); count++
                        count++
                    }
                }
            }
        }
    }
}

/**
 * 任务卡片事件推送
 * @property eventKey EventKey	与发送任务卡片消息时指定的按钮btn:key值相同
 * @property taskId TaskId	与发送任务卡片消息时指定的task_id相同
 * */
class WorkTaskCardClickEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
    }
    var eventKey: String? = null
    var taskId: String? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 3) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText; count++
                    }
                    "TaskId" -> {
                        taskId = reader.elementText; count++
                        count++
                    }
                    "AgentID" -> {
                        agentId = reader.elementText
                        count++
                    }
                }
            }
        }
        
    }
}

//
//
///**
// * 点击菜单跳转小程序的事件推送
// *
// * @param eventKey EventKey	事件KEY值，跳转的小程序路径
// * @param menuId MenuID	菜单ID，如果是个性化菜单，则可以通过这个字段，知道是哪个规则的菜单被点击了
// * */
//class WorkMenuMiniEvent(base: BaseInfo): AgentEvent(baseInfo)
//{
//    var eventKey: String? = null
//    var menuId: String? = null
//    override fun read(reader: XMLEventReader) {
//        var count = 0
//        while (reader.hasNext() && count < 2) {
//            val event = reader.nextEvent()
//            if (event.isStartElement) {
//                when (event.asStartElement().name.toString()) {
//                    "EventKey" -> {
//                        eventKey = reader.elementText; count++
//                    }
//                    "MenuId" -> {
//                        menuId = reader.elementText; count++
//                    }
//                }
//            }
//        }
//    }
//}



/**
 * 模版消息发送结果事件
 *
 * 在模版消息发送任务完成后，微信服务器会将是否送达成功作为通知，发送到开发者中心中填写的服务器配置地址中。
 *
 * @property status success 送达成功;
 * failed:user block: 送达由于用户拒收（用户设置拒绝接收公众号消息）;
 * failed: system failed: 发送状态为发送失败（非用户拒绝）
 * */
//class WorkTemplateSendJobFinish(base: BaseInfo): AgentEvent(baseInfo)
//{
//    var status: String? = null
//    override fun read(reader: XMLEventReader) {
//        while (reader.hasNext()) {
//            val event = reader.nextEvent()
//            if (event.isStartElement) {
//                when (event.asStartElement().name.toString()) {
//                    "Status" -> {
//                        status = reader.elementText; break
//                    }
//                }
//            }
//        }
//    }
//}


/**
 * 用户未关注时，扫码关注后的事件推送
 *
 * @property eventKey EventKey	事件KEY值，qrscene_为前缀，后面为二维码的参数值
 * @property ticket Ticket	二维码的ticket，可用来换取二维码图片
 * */
//class WorkScanSubscribeEvent(base: BaseInfo): AgentEvent(baseInfo)
//{
//    var eventKey: String? = null
//    var ticket: String? = null
//    override fun read(reader: XMLEventReader) {
//        var count = 0
//        while (reader.hasNext() && count < 2) {
//            val event = reader.nextEvent()
//            if (event.isStartElement) {
//                when (event.asStartElement().name.toString()) {
//                    "EventKey" -> {
//                        eventKey = reader.elementText; count++
//                    }
//                    "Ticket" -> {
//                        ticket = reader.elementText; count++
//                    }
//                }
//            }
//        }
//    }
//}


