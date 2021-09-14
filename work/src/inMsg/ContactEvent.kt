/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-13 17:24
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.wxSDK.work.inMsg


import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.work.*
import javax.xml.stream.XMLEventReader


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
open class WorkChangeContactEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): AgentEvent(baseInfo)
{
    init {
        event = agentEvent.event
        agentId = agentEvent.agentId
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
                    var name: String?
                    //val map = mutableMapOf<String, String?>()
                    while (reader.hasNext()){
                        val e2 = reader.nextEvent()
                        if(e2.isEndElement && "item" == e2.asEndElement().name.toString()) {
                            readExtAttr(reader)?.let { list.add(it) }
                        }
                    }
                } else if (event1.isEndElement && "ExtAttr" == event1.asEndElement().name.toString()) {
                    break
                }
            }
            return  list
        }

        fun readExtAttr(reader: XMLEventReader): ExtAttr?{
            var name: String? = null
            var type: Int? = null
            while (reader.hasNext()) {
                val e = reader.nextEvent()
                if (e.isStartElement) {
                    when(e.asStartElement().name.toString()){
                        "Name" -> {
                            name = reader.elementText
                            break
                        }
                        "Type" -> {
                            type = reader.elementText?.toInt()
                            break
                        }
                        "Text" -> {
                            val e2 = reader.nextEvent()
                            while (reader.hasNext()) {
                                if (e2.isStartElement) {
                                    when(e2.asStartElement().name.toString()){
                                        "Value" -> {
                                            return ExtAttrText(name, reader.elementText)
                                        }
                                    }
                                }
                            }
                        }
                        "Web" -> {
                            var url: String? = null
                            var title: String? = null
                            var count = 0
                            val e2 = reader.nextEvent()
                            while (reader.hasNext() && count < 2) {
                                if (e2.isStartElement) {
                                    when(e.asStartElement().name.toString()){
                                        "Title" -> {
                                            title = reader.elementText; count++
                                        }
                                        "Url" -> {
                                            url = reader.elementText; count++
                                        }
                                    }
                                }
                            }
                            return ExtAttrWeb(name, title, url)
                        }
                        "MiniProgram" ->{ //此段内容，文档中没有，根据读取成员详情添加进来的
                            var appId: String? = null
                            var pagepath: String? = null
                            var title: String? = null
                            var count = 0
                            val e2 = reader.nextEvent()
                            while (reader.hasNext() && count < 3) {
                                if (e2.isStartElement) {
                                    when(e.asStartElement().name.toString()){
                                        "Title" -> {
                                            title = reader.elementText; count++
                                        }
                                        "AppID" -> {
                                            appId = reader.elementText; count++
                                        }
                                        "PagePath" -> {
                                            pagepath = reader.elementText; count++
                                        }
                                    }
                                }
                            }
                            return ExtAttrMiniProgram(name, title, appId, pagepath)
                        }
                    }
                }
            }
            return null
        }
    }

    fun toAttr():Attr?{
        return when(this){
            is ExtAttrText -> TextAttr(0, name?:"", Text(value?:""))
            is ExtAttrWeb -> WebAttr(1, name?:"", Web(url?:"", title?:"") )
            is ExtAttrMiniProgram -> MiniProgramAttr(2,name?:"", MiniProgram(appId?:"", pagePath?:"", title?:""))
            else -> null
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
class ExtAttrMiniProgram(name: String?, val title: String?, val appId: String?, val pagePath: String?):ExtAttr(name, 2)
/**
 * 新增成员事件
 * @property userId UserID	变更信息的成员UserID
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
open class WorkUserCreateEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): WorkChangeContactEvent(baseInfo, agentEvent) {
    init {
        changeType = CREATE_USER
    }
    var userId: String? = null
    var mainDepartment: String? = null
    var name: String? = null
    var department: String? = null
    var isLeaderInDept: String? = null
    var mobile: String? = null
    var position: String? = null
    var gender: String? = null
    var email: String? = null
    var status: String? = null
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
                    "Name" ->{ name= reader.elementText; count2++}
                    "Department" ->{ department = reader.elementText; count2++ }
                    "MainDepartment" ->{ mainDepartment= reader.elementText; count2++}
                    "IsLeaderInDept" ->{ isLeaderInDept= reader.elementText; count2++}
                    "Mobile" ->{ mobile= reader.elementText; count2++}
                    "Position" ->{ position = reader.elementText; count2++ }
                    "Gender" ->{ gender= reader.elementText; count2++}
                    "Email" ->{ email= reader.elementText; count2++}
                    "Status" ->{ status= reader.elementText; count2++}
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
 *
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
class WorkUserUpdateEvent(baseInfo: BaseInfo, agentEvent: AgentEvent):  WorkChangeContactEvent(baseInfo, agentEvent) {
    init {
        changeType = CREATE_USER
    }
    var userId: String? = null
    var newUserID: String? = null
    var name: String? = null
    var department: String? = null
    var mainDepartment: String? = null
    var isLeaderInDept: String? = null
    var mobile: String? = null
    var position: String? = null
    var gender: String? = null
    var email: String? = null
    var status: String? = null
    var avatar: String? = null
    var alias: String? = null
    var telephone: String? = null
    var address: String? = null
    var extAttrs: List<ExtAttr>? = null
    override fun read(reader: XMLEventReader)
    {
        var count2 = 0
        while (reader.hasNext() && count2 < 16) {
            val e = reader.nextEvent()
            if (e.isStartElement) {
                when(e.asStartElement().name.toString()){
                    "UserID" ->{ userId = reader.elementText; count2++ }
                    "NewUserID" ->{ newUserID= reader.elementText; count2++}
                    "Name" ->{ name= reader.elementText; count2++}
                    "Department" ->{ department = reader.elementText; count2++ }
                    "MainDepartment" ->{ mainDepartment= reader.elementText; count2++}
                    "IsLeaderInDept" ->{ isLeaderInDept= reader.elementText; count2++}
                    "Mobile" ->{ mobile= reader.elementText; count2++}
                    "Position" ->{ position = reader.elementText; count2++ }
                    "Gender" ->{ gender= reader.elementText; count2++}
                    "Email" ->{ email= reader.elementText; count2++}
                    "Status" ->{ status= reader.elementText; count2++}
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
 * 删除成员事件
 * @property userId UserID	变更信息的成员UserID
 * */
class WorkUserDelEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): WorkChangeContactEvent(baseInfo, agentEvent) {
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
class WorkPartyCreateEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): WorkChangeContactEvent(baseInfo, agentEvent) {
    init {
        changeType = CREATE_PARTY
    }
    var id: String? = null
    var name: String? = null
    var parentId: String? = null
    var order: String? = null
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
                    "Order" ->{ order= reader.elementText; count2++}
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
class WorkPartyUpdateEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): WorkChangeContactEvent(baseInfo, agentEvent) {
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
class WorkPartyDelEvent(baseInfo: BaseInfo,  agentEvent: AgentEvent): WorkChangeContactEvent(baseInfo, agentEvent) {
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
class WorkTagUpdateEvent(baseInfo: BaseInfo, agentEvent: AgentEvent): WorkChangeContactEvent(baseInfo, agentEvent) {
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