package com.github.rwsbillyang.wxSDK.work.inMsg


import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.InEventType
import com.github.rwsbillyang.wxSDK.msg.SendPicsInfo
import com.github.rwsbillyang.wxSDK.msg.WxBaseEvent
import javax.xml.stream.XMLEventReader

/*
* https://work.weixin.qq.com/api/doc/90000/90135/90240
* 开启接收消息模式后，可以配置接收事件消息。
* 当企业成员通过企业微信APP或微工作台（原企业号）触发进入应用、上报地理位置、点击菜单等事件时，企业微信会将这些事件消息发送给企业后台。
* 如何接收消息已经在使用接收消息说明，本小节是对事件消息结构体的说明。
*
* 注：以下出现的xml包仅是接收的消息包中的Encrypt参数解密后的内容说明
* */

/**
 * 添加了AgentID的事件基类
 * */
open class WorkBaseEvent(base: BaseInfo): WxBaseEvent(base)
{
    companion object{
        const val ENTER_AGENT = "enter_agent"
        const val BATCH_JOB_RESULT = "batch_job_result"
        const val CHANGE_CONTACT = "change_contact"
        const val OPEN_APPROVAL_CHANGE = "open_approval_change"
        const val TASK_CARD_CLICK = "taskcard_click"
    }

    var agentId: String? = null
    fun readEvent(reader: XMLEventReader) = super.read(reader)
    protected fun readAgentId(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "AgentID" -> {
                        agentId = reader.elementText
                        break
                    }
                }
            }
        }
    }

    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 2) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                //实际顺序与文档中不一致
                when(e.asStartElement().name.toString()){
                    "AgentID" -> {
                        agentId = reader.elementText
                        count++
                    }
                    "Event" -> {
                        event = reader.elementText
                        count++
                    }
                }
            }
        }
    }
}

/**
 * 关注事件
 *
 * 小程序在管理端开启接收消息配置后，也可收到关注/取消关注事件
 *
 * 本事件触发时机为：
 * 成员已经加入企业，管理员添加成员到应用可见范围(或移除可见范围)时
 * 成员已经在应用可见范围，成员加入(或退出)企业时
 * 事件类型，subscribe(关注)、unsubscribe(取消关注)
 * */
class WorkSubscribeEvent(base: WorkBaseEvent): WorkBaseEvent(base.base){
    init {
        agentId = base.agentId
        event = base.event
    }
}

/**
 * 取消关注事件
 *
 * 事件类型，subscribe(关注)、unsubscribe(取消关注)
 * */
class WorkUnsubscribeEvent(base: WorkBaseEvent): WorkBaseEvent(base.base){
    init {
        agentId = base.agentId
        event = base.event
    }
}
/**
 * 进入应用
 * 本事件在成员进入企业微信的应用时触发
 * @property eventKey EventKey	事件KEY值，此事件该值为空
 * */
class WorkEnterAgent(base: WorkBaseEvent): WorkBaseEvent(base.base){
    init {
        agentId = base.agentId
        event = base.event
    }

    var eventKey: String? = null
    override fun read(reader: XMLEventReader)
    {
        //super.readEvent(reader)
        while (reader.hasNext()) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "EventKey" -> {
                        eventKey = reader.elementText
                        break
                    }
                }
            }
            //super.readAgentId(reader)
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
open class WorkLocationEvent(base: BaseInfo): WorkBaseEvent(base)
{
    init {
        event = InEventType.LOCATION
    }
    var latitude: Float? = null
    var longitude: Float?= null
    var precision: Float?= null
    val appType = "wxwork"

    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 3) {
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
                }
            }
        }
        super.readAgentId(reader)
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
class WorkBatchJobResultEvent(base: BaseInfo): WorkBaseEvent(base){
    init {
        event = BATCH_JOB_RESULT
    }
    var batchJob: BatchJob? = null
    override fun read(reader: XMLEventReader) {
        var count = 0

        var id: String? = null
        var type: String? = null
        var code: Int?= null
        var msg: String? = null
        while (reader.hasNext() && count < 3) {
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
* 通讯录变更事件
*
* 当企业通过通讯录助手开通通讯录权限后，成员的变更会通知给企业。变更的事件，将推送到企业微信管理端通讯录助手中的‘接收事件服务器’。
* 由通讯录同步助手调用接口触发的变更事件不回调通讯录同步助手本身。管理员在管理端更改组织架构或者成员信息以及企业微信的成员在客户端
* 变更自己的个人信息将推送给通讯录同步助手。第三方通讯录变更事件参见第三方回调协议
* */

/**
 * 通讯录变更事件  base class
 * */
open class WorkChangeContactEvent(base: BaseInfo): WorkBaseEvent(base) {
    init {
        event = CHANGE_CONTACT
    }
    var changeType: String? = null
    companion object{
        const val CREATE_USER = "create_user"
        const val UPDATE_USER = "update_user"
        const val DELETE_USER = "delete_user"
        const val CREATE_PARTY= "create_party"
        const val UPDATE_PARTY= "update_party"
        const val DELETE_PARTY= "delete_party"
        const val UPDATE_TAG= "update_tag"
    }
    override fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "ChangeType" -> {
                        changeType = reader.elementText
                        break
                    }
                }
            }
        }
    }
}

/**
 * 扩展属性
 * @param name 扩展属性名称
 * @param type 扩展属性类型: 0-文本 1-网页
 * */
open class ExtAttr(val name: String?, val type: Int?)
{
    companion object{
        fun fromXml(reader: XMLEventReader):List<ExtAttr>{
            val list = mutableListOf<ExtAttr>()
            while (reader.hasNext()) {
                val event1 = reader.nextEvent()
                if (event1.isStartElement && "item" == event1.asStartElement().name.toString())
                {
                    val map = mutableMapOf<String, String?>()
                    while (reader.hasNext()){
                        val event2 = reader.nextEvent()
                        val tag = event1.asStartElement().name.toString()
                        map[tag] = reader.elementText
                        if(event2.isEndElement && "item" == event2.asEndElement().name.toString()) {
                            break
                        }
                    }

                    val type = map["Type"]?.toInt()
                    when(type){
                        0 -> list.add(ExtAttrText(map["Name"],map["Value"]))
                        1 -> list.add(ExtAttrWeb(map["Name"],map["Title"],map["Url"]))
                        else -> list.add(ExtAttr(map["Name"],type))
                    }
                } else if (event1.isEndElement && "ExtAttr" == event1.asEndElement().name.toString()) {
                    break
                }
            }
            return  list
        }
    }
}
/**
 * 文本扩展属性
 * @param value 文本属性内容
 * */
class ExtAttrText(name: String?,val value: String?):ExtAttr(name, 0)
/**
 * 网页扩展属性
 * @param title 网页的展示标题
 * @param url Url	网页的url
 * */
class ExtAttrWeb(name: String?, val title: String?, val url: String?):ExtAttr(name, 1)
/**
 * 新增成员事件
 * @property userId UserID	变更信息的成员UserID
 * @property newUserID NewUserID	新的UserID，变更时推送（userid由系统生成时可更改一次）
 * @property name Name	成员名称，变更时推送
 * @property department Department	成员部门列表，变更时推送，仅返回该应用有查看权限的部门id
 * @property isLeaderInDept IsLeaderInDept	表示所在部门是否为上级，0-否，1-是，顺序与Department字段的部门逐一对应
 * @property mobile Mobile	手机号码，变更时推送
 * @property position Position	职位信息。长度为0~64个字节，变更时推送
 * @property gender Gender	性别，变更时推送。1表示男性，2表示女性
 * @property email Email	邮箱，变更时推送
 * @property status Status	激活状态：1=激活或关注， 2=禁用， 4=未激活（重新启用未激活用户或者退出企业并且取消关注时触发）
 * @property avatar Avatar	头像url。注：如果要获取小图将url最后的”/0”改成”/100”即可。变更时推送
 * @property alias Alias	成员别名，变更时推送
 * @property telephone Telephone	座机，变更时推送
 * @property address Address	地址
 * */
open class WorkUserCreateEvent(base: BaseInfo): WorkChangeContactEvent(base) {
    init {
        changeType = CREATE_USER
    }
    var userId: String? = null
    var newUserID: String? = null
    var name: String? = null
    var department: String? = null
    var isLeaderInDept: Int? = null
    var mobile: String? = null
    var position: String? = null
    var gender: Int? = null
    var email: String? = null
    var status: Int? = null
    var avatar: String? = null
    var alias: String? = null
    var telephone: String? = null
    var address: String? = null
    var extAttrs: List<ExtAttr>? = null
    override fun read(reader: XMLEventReader)
    {
        var count2 = 0
        while (reader.hasNext() && count2 < 15) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "UserID" ->{ userId = reader.elementText; count2++ }
                    "NewUserID" ->{ newUserID= reader.elementText; count2++}
                    "Name" ->{ name= reader.elementText; count2++}
                    "Department" ->{ department = reader.elementText; count2++ }
                    "IsLeaderInDept" ->{ isLeaderInDept= reader.elementText?.toInt(); count2++}
                    "Mobile" ->{ mobile= reader.elementText; count2++}
                    "Position" ->{ position = reader.elementText; count2++ }
                    "Gender" ->{ gender= reader.elementText?.toInt(); count2++}
                    "Email" ->{ email= reader.elementText; count2++}
                    "Status" ->{ status= reader.elementText?.toInt(); count2++}
                    "Avatar" ->{ avatar = reader.elementText; count2++ }
                    "Alias" ->{ alias= reader.elementText; count2++}
                    "Telephone" ->{ telephone= reader.elementText; count2++}
                    "Address" ->{ address= reader.elementText; count2++}
                    "ExtAttr" -> {extAttrs = ExtAttr.fromXml(reader); count2++}
                }
            }
        }
    }
}

/**
 * 更新部门事件
 * @property userId UserID	变更信息的成员UserID
 * @property newUserID NewUserID	新的UserID，变更时推送（userid由系统生成时可更改一次）
 * @property name Name	成员名称，变更时推送
 * @property department Department	成员部门列表，变更时推送，仅返回该应用有查看权限的部门id
 * @property isLeaderInDept IsLeaderInDept	表示所在部门是否为上级，0-否，1-是，顺序与Department字段的部门逐一对应
 * @property mobile Mobile	手机号码，变更时推送
 * @property position Position	职位信息。长度为0~64个字节，变更时推送
 * @property gender Gender	性别，变更时推送。1表示男性，2表示女性
 * @property email Email	邮箱，变更时推送
 * @property status Status	激活状态：1=激活或关注， 2=禁用， 4=未激活（重新启用未激活用户或者退出企业并且取消关注时触发）
 * @property avatar Avatar	头像url。注：如果要获取小图将url最后的”/0”改成”/100”即可。变更时推送
 * @property alias Alias	成员别名，变更时推送
 * @property telephone Telephone	座机，变更时推送
 * @property address Address	地址
 * */
class WorkUserUpdateEvent(base: BaseInfo): WorkUserCreateEvent(base) {
    init {
        changeType = UPDATE_USER
    }
}

/**
 * 删除成员事件
 * @property userId UserID	变更信息的成员UserID
 * */
class WorkUserDelEvent(base: BaseInfo): WorkChangeContactEvent(base) {
    init {
        changeType = DELETE_USER
    }
    var userId: String? = null
    override fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "UserID" ->{ userId = reader.elementText; break }
                }
            }
        }
    }
}

/**
 * 新增部门事件
 * @property id Id	部门Id
 * @property name Name	部门名称
 * @property parentId ParentId	父部门id
 * @property order Order	部门排序
 * */
class WorkPartyCreateEvent(base: BaseInfo): WorkChangeContactEvent(base) {
    init {
        changeType = CREATE_PARTY
    }
    var id: String? = null
    var name: String? = null
    var parentId: String? = null
    var order: Int? = null
    override fun read(reader: XMLEventReader)
    {
        var count2 = 0
        while (reader.hasNext() && count2 < 4) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "Id" ->{ id = reader.elementText; count2++ }
                    "Name" ->{ name= reader.elementText; count2++}
                    "ParentId" ->{ parentId= reader.elementText; count2++}
                    "Order" ->{ order= reader.elementText?.toInt(); count2++}
                }
            }
        }
    }
}

/**
 * 更新部门事件
 * @property id Id	部门Id
 * @property name Name	部门名称，仅发送变更时传递
 * @property parentId ParentId	父部门id，仅发送变更时传递
 * */
class WorkPartyUpdateEvent(base: BaseInfo): WorkChangeContactEvent(base) {
    init {
        changeType = UPDATE_PARTY
    }
    var id: String? = null
    var name: String? = null
    var parentId: String? = null
    override fun read(reader: XMLEventReader)
    {
        var count2 = 0
        while (reader.hasNext() && count2 < 3) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "Id" ->{ id = reader.elementText; count2++ }
                    "Name" ->{ name= reader.elementText; count2++}
                    "ParentId" ->{ parentId= reader.elementText; count2++}
                }
            }
        }
    }
}

/**
 * 删除部门事件
 * @property id Id	部门Id
 * */
class WorkPartyDelEvent(base: BaseInfo): WorkChangeContactEvent(base) {
    init {
        changeType = DELETE_PARTY
    }
    var id: String? = null
    override fun read(reader: XMLEventReader)
    {
        while (reader.hasNext()) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "Id" ->{ id = reader.elementText; break }
                }
            }
        }
    }
}

/**
 * 标签成员变更事件
 * @property tagId TagId	标签Id
 * @property addUserItems AddUserItems	标签中新增的成员userid列表，用逗号分隔
 * @property delUserItems	标签中删除的成员userid列表，用逗号分隔
 * @property addPartyItems	标签中新增的部门id列表，用逗号分隔
 * @property delPartyItems	标签中删除的部门id列表，用逗号分隔
 * */
class WorkTagUpdateEvent(base: BaseInfo): WorkChangeContactEvent(base) {
    init {
        changeType = UPDATE_TAG
    }
    var tagId: String? = null
    var addUserItems: List<String>? = null
    var delUserItems: List<String>? = null
    var addPartyItems: List<String>? = null
    var delPartyItems: List<String>? = null
    override fun read(reader: XMLEventReader)
    {
        var count = 0
        while (reader.hasNext() && count < 4) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "TagId" ->{ tagId = reader.elementText; count++ }
                    "AddUserItems" ->{ addUserItems = reader.elementText?.split(','); count++ }
                    "DelUserItems" ->{ delUserItems = reader.elementText.split(','); count++ }
                    "AddPartyItems" ->{ addPartyItems = reader.elementText.split(','); count++ }
                    "DelPartyItems" ->{ delPartyItems = reader.elementText.split(','); count++ }
                }
            }
        }
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
class WorkMenuClickEvent(base: BaseInfo): WorkBaseEvent(base)
{
    init {
        event = InEventType.CLICK
    }
    var eventKey: String? = null
    override fun read(reader: XMLEventReader) {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText; break
                    }
                }
            }
        }
        super.readAgentId(reader)
    }
}

/**
 * 点击菜单拉取消息的事件推送
 *
 * @property eventKey EventKey	事件KEY值，设置的跳转URL
 * //@property menuId MenuID	指菜单ID，如果是个性化菜单，则可以通过这个字段，知道是哪个规则的菜单被点击了
 *
 * */
class WorkMenuViewEvent(base: BaseInfo): WorkBaseEvent(base)
{
    init {
        event = InEventType.VIEW
    }
    var eventKey: String? = null
    //var menuId: String? = null
    override fun read(reader: XMLEventReader) {
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                when (event.asStartElement().name.toString()) {
                    "EventKey" -> {
                        eventKey = reader.elementText; break
                    }
                }
            }
        }
        super.readAgentId(reader)
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
open class WorkMenuScanCodePushEvent(base: BaseInfo): WorkBaseEvent(base)
{
    init {
        event = InEventType.SCAN_CODE_PUSH
    }
    var eventKey: String? = null
    var scanType: String? = null
    var scanResult: String? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 2) {
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
                }
            }
        }
        super.readAgentId(reader)
    }
}

/**
 * scancode_waitmsg：扫码推事件且弹出“消息接收中”提示框的事件推送
 *
 * */
class WorkMenuScanCodeWaitEvent(base: BaseInfo): WorkMenuScanCodePushEvent(base)
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
open class WorkMenuPhotoEvent(base: BaseInfo): WorkBaseEvent(base){
    init {
        event = InEventType.PIC_SYS_PHOTO
    }
    var eventKey: String? = null
    var sendPicsInfo: SendPicsInfo? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 2) {
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
                }
            }
        }
        super.readAgentId(reader)
    }


}
/**
 * pic_photo_or_album：弹出拍照或者相册发图的事件推送
 *
 * */
class WorkMenuPhotoOrAlbumEvent(base: BaseInfo): WorkMenuPhotoEvent(base)
{
    init {
        event = InEventType.PIC_PHOTO_OR_ALBUM
    }
}


/**
 * pic_weixin：弹出微信相册发图器的事件推送
 *
 * */
class WorkMenuWorkAlbumEvent(base: BaseInfo): WorkMenuPhotoEvent(base)
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
class WorkMenuLocationEvent(base: BaseInfo): WorkBaseEvent(base)
{
    init {
        event = InEventType.LOCATION_SELECT
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
        while (reader.hasNext() && count < 2) {
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
                }
            }
        }
        super.readAgentId(reader)
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
class WorkApprovalStatusChangeEvent(base: BaseInfo): WorkBaseEvent(base)
{
    init {
        event = OPEN_APPROVAL_CHANGE
    }
    var eventKey: String? = null
    var approvalInfo: ApprovalInfo? = null
    override fun read(reader: XMLEventReader) {
        super.readAgentId(reader)
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
class WorkTaskCardClickEvent(base: BaseInfo): WorkBaseEvent(base){
    init {
        event = TASK_CARD_CLICK
    }
    var eventKey: String? = null
    var taskId: String? = null
    override fun read(reader: XMLEventReader) {
        var count = 0
        while (reader.hasNext() && count < 2) {
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
                }
            }
        }
        super.readAgentId(reader)
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
//class WorkMenuMiniEvent(base: BaseInfo): WorkBaseEvent(base)
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
//class WorkTemplateSendJobFinish(base: BaseInfo): WorkBaseEvent(base)
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
//class WorkScanSubscribeEvent(base: BaseInfo): WorkBaseEvent(base)
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


