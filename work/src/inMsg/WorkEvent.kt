package com.github.rwsbillyang.wxSDK.work.inMsg



import com.github.rwsbillyang.wxSDK.msg.*
import org.w3c.dom.Element

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


    const val WxkfMsgEvent = "kf_msg_or_event"
}

/**
 * 添加了AgentID的事件基类
 *
 * 注意：各Event的构建参数类型尽量使用读取较多共性数据的类，自己只需要读取较少的剩下的数据。
 * 若有未覆盖到的成员，将导致其为空
 * @param baseInfo 已经读取的xml构成的BaseInfo
 * */
open class AgentEvent(xml: String, rootDom: Element): WxXmlEvent(xml, rootDom)
{
    val agentId = get(rootDom, "AgentID")
}

/**
 * 关注事件
 *
 * 小程序在管理端开启接收消息配置后，也可收到关注/取消关注事件
 * <xml>
 *     <ToUserName><![CDATA[wwb096af219dea3f1c]]></ToUserName>
 *     <FromUserName><![CDATA[BiDandan]]></FromUserName>
 *     <CreateTime>1631359354</CreateTime>
 *     <MsgType><![CDATA[event]]></MsgType>
 *     <AgentID>1000002</AgentID>
 *     <Event><![CDATA[subscribe]]></Event>
 * </xml>
 * 本事件触发时机为：
 * 成员已经加入企业，管理员添加成员到应用可见范围(或移除可见范围)时
 * 成员已经在应用可见范围，成员加入(或退出)企业时
 * 事件类型，subscribe(关注)、unsubscribe(取消关注)
 *
 * @param agentEvent 已经读取的xml构成的agentEvent
 * */
class WorkSubscribeEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)


/**
 * 取消关注事件
 *
 * 事件类型，subscribe(关注)、unsubscribe(取消关注)
 * <xml><ToUserName><![CDATA[wwb096af219dea3f1c]]></ToUserName><FromUserName><![CDATA[BiDandan]]></FromUserName><CreateTime>1631610483</CreateTime><MsgType><![CDATA[event]]></MsgType><AgentID>1000002</AgentID><Event><![CDATA[unsubscribe]]></Event></xml>
 * */
class WorkUnsubscribeEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)


/**
 * 进入应用
 * 本事件在成员进入企业微信的应用时触发
 * @property eventKey EventKey	事件KEY值，此事件该值为空
 * */
class WorkEnterAgent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    val eventKey = get(rootDom, "EventKey")
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
open class WorkLocationEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    val latitude = get(rootDom, "Latitude")?.toFloat()
    val longitude = get(rootDom, "Longitude")?.toFloat()
    val precision = get(rootDom, "Precision")?.toFloat()
    val appType = "wxwork"
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
class WorkBatchJobResultEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    val batchJob: BatchJob = BatchJob(
        get(rootDom, "JobId"),
        get(rootDom, "JobType"),
        get(rootDom, "ErrCode")?.toInt(),
        get(rootDom, "ErrMsg"))
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
class WorkMenuClickEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    val eventKey = get(rootDom, "EventKey")
}

/**
 * 点击菜单拉取消息的事件推送
 *
 * @property eventKey EventKey	事件KEY值，设置的跳转URL
 * //@property menuId MenuID	指菜单ID，如果是个性化菜单，则可以通过这个字段，知道是哪个规则的菜单被点击了
 *
 * */
class WorkMenuViewEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    val eventKey = get(rootDom, "EventKey")
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
open class WorkMenuScanCodePushEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    val eventKey = get(rootDom, "EventKey")
    val scanType = get(rootDom, "ScanType") //in ScanCodeInfo
    val scanResult = get(rootDom, "ScanResult") //in ScanCodeInfo
}

/**
 * scancode_waitmsg：扫码推事件且弹出“消息接收中”提示框的事件推送
 *
 * */
class WorkMenuScanCodeWaitEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)





/**
 * pic_sysphoto：弹出系统拍照发图的事件推送
 *
 * @property eventKey EventKey	事件KEY值，由开发者在创建菜单时设定
 * @property picsInfo SendPicsInfo 图片的MD5值，开发者若需要，可用于验证接收到图片
 * */
open class WorkMenuPhotoEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    val eventKey = get(rootDom, "EventKey")
    val sendPicsInfo: SendPicsInfo? = SendPicsInfo.fromXml(getChild(rootDom,"SendPicsInfo"))
}
/**
 * pic_photo_or_album：弹出拍照或者相册发图的事件推送
 *
 * */
class WorkMenuPhotoOrAlbumEvent(xml: String, rootDom: Element): WorkMenuPhotoEvent(xml, rootDom)


/**
 * pic_weixin：弹出微信相册发图器的事件推送
 *
 * */
class WorkMenuWorkAlbumEvent(xml: String, rootDom: Element): WorkMenuPhotoEvent(xml, rootDom)

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
class WorkMenuLocationEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    /*
    * <EventKey><![CDATA[6]]></EventKey>
        <SendLocationInfo><Location_X><![CDATA[23]]></Location_X>
        <Location_Y><![CDATA[113]]></Location_Y>
        <Scale><![CDATA[15]]></Scale>
        <Label><![CDATA[ 广州市海珠区客村艺苑路 106号]]></Label>
        <Poiname><![CDATA[]]></Poiname>
        </SendLocationInfo>
    * */
    val eventKey = get(rootDom, "EventKey")
    val locationX = get(rootDom, "Location_X")?.toFloat()
    val locationY = get(rootDom, "Location_Y")?.toFloat()
    val scale = get(rootDom, "Scale")?.toInt()
    val label = get(rootDom, "Label")
    val poiname = get(rootDom, "Poiname")
    val appType = "wxwork"
}

/**
 * TODO: 完善剩余各字段
 * */
class ApprovalInfo(val id: String?, val openSpName: String?)
{

}
/**
 * 审批状态通知事件
 *
 * 本事件触发时机为：
 * 1.自建/第三方应用调用审批流程引擎发起申请之后，审批状态发生变化时
 * 2.自建/第三方应用调用审批流程引擎发起申请之后，在“审批中”状态，有任意审批人进行审批操作时
 * */
class WorkApprovalStatusChangeEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{

    val eventKey = get(rootDom, "EventKey")
    val approvalInfo: ApprovalInfo? = null
}

/**
 * 任务卡片事件推送
 * @property eventKey EventKey	与发送任务卡片消息时指定的按钮btn:key值相同
 * @property taskId TaskId	与发送任务卡片消息时指定的task_id相同
 * */
class WorkTaskCardClickEvent(xml: String, rootDom: Element): AgentEvent(xml, rootDom)
{
    val eventKey = get(rootDom, "EventKey")
    val taskId = get(rootDom, "TaskId")
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
//    val eventKey: String? = null
//    val menuId: String? = null
//    override fun read(reader: XMLEventReader) {
//        val count = 0
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
//    val status: String? = null
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
//    val eventKey: String? = null
//    val ticket: String? = null
//    override fun read(reader: XMLEventReader) {
//        val count = 0
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


