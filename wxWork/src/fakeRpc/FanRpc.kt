///*
// * Copyright © 2021 rwsbillyang@qq.com
// *
// * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-21 22:21
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.github.rwsbillyang.wxWork.fakeRpc
//
//
//
//import com.github.rwsbillyang.ktorKit.ApiJson
//import com.github.rwsbillyang.wxSDK.work.Work
//
//import com.github.rwsbillyang.wxUser.account.VID
//import com.github.rwsbillyang.wxUser.fakeRpc.FanInfo
//import com.github.rwsbillyang.wxUser.fakeRpc.IFan
//import com.github.rwsbillyang.wxWork.account.AccountServiceWxWork
//import com.github.rwsbillyang.wxWork.agent.AgentService
//import com.github.rwsbillyang.wxWork.config.ConfigService
//
//import com.github.rwsbillyang.wxWork.contacts.Contact
//import com.github.rwsbillyang.wxWork.contacts.ContactService
//import com.github.rwsbillyang.wxWork.contacts.ExternalContact
//import com.github.rwsbillyang.wxWork.isv.CorpInfo
//import com.github.rwsbillyang.wxWork.isv.IsvCorpService
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.decodeFromString
//import kotlinx.serialization.encodeToString
//import org.koin.core.component.KoinComponent
//import org.koin.core.component.inject
//
//
////从未登录过的可见范围内部成员提交的VID中将无openID，需从agent初始化时获取到的客户信息中提取openId
//private fun Contact.toFanInfo(vId: VID) = FanInfo(vId.openId?:openId?:"noOpenId",null, thumb?:avatar, name, gender?.toInt(), address)
//private fun ExternalContact.toFanInfo(vId: VID) = FanInfo(vId.openId?:openId?:"noOpenId",null, avatar, name, gender, null)
//
//
//
//@Serializable
//class AgentAllowUsersInfo(
//    val allowUsers: List<String>?,
//    val corpId: String,
//    val agentId: Int?,
//    val suiteId: String? = null
//){
//    fun toJson() = ApiJson.serializeJson.encodeToString(this)
//    companion object{
//        fun fromJson(str: String) = ApiJson.serializeJson.decodeFromString<AgentAllowUsersInfo>(str)
//    }
//}
//
//
//
//class FanRpcWork: IFan, KoinComponent {
//
//    private val accountService: AccountServiceWxWork by inject()
//    private val contactService: ContactService by inject()
//
//    //从contact或external contacts里获取用户信息
//    override fun getFanInfo(vId: VID): FanInfo {
//        return when {
//            vId.userId != null && vId.corpId != null-> {
//                //只是为了获取nick avatar等信息，
//                // 因为里面的openId权限和更新等错误，很可能为null 而前端各种参数传递然后查询后端依赖于fanInfo.openId
//                val contact = contactService.findContact(vId.userId!!, vId.corpId!!)
//                if(vId.openId == null) vId.openId = contact?.openId//可见范围客户从未登录，因此前端storage中无openId，提交的VID信息中将无openId，需要获取其openId
//                contact?.toFanInfo(vId)
//            }
//            vId.externalId != null && vId.corpId != null -> {//外部成员
//                contactService.findExternalContact(vId.externalId!!, vId.corpId!!)?.toFanInfo(vId)
//            }
//            vId.openId != null && vId.corpId != null-> { //对应用不可见的内部成员
//                contactService.findContactByOpenId(vId.openId!!, vId.corpId!!)?.toFanInfo(vId)
//            }
//            else -> {
//                null
//            }
//
//        }?:FanInfo(vId.openId?:"NoOpenId")
//    }
//
//    /**
//     * 返回关注agent，即可见范围内的成员userId，为了保证接口统一一致，列表中元素是SubscribeUserInfo的json后的字符串
//     * */
//    override fun getSubscribedList(page: Int, pageSize: Int): List<String> {
//        return if(Work.isIsv){
//            //从CorpInfo中读取授权过的: corpId, agentId, suiteId, 以及allowUsers
//            val isvCorpService: IsvCorpService by inject()
//            isvCorpService.findCorpInfoList(CorpInfo.STATUS_ENABLED).map {
//                val agent = it.authInfo?.agent?.firstOrNull()
//                AgentAllowUsersInfo(agent?.privilege?.allow_user, it.corpId, agent?.agentid, it.suiteId).toJson()
//            }
//        }else{
//            //从WxWorkConfig中读取corpId，agentId，以及Agent中读取对应的allowUsers
//            val configService: ConfigService by inject()
//            val agentService: AgentService by inject()
//            configService.findAgentConfigList(true).map{
//                val agent = agentService.findAgent(it.agentId, it.corpId)
//                AgentAllowUsersInfo(agent?.userList, it.corpId, it.agentId).toJson()
//            }
//        }
//    }
//
//    override fun getFanInfo(openId: String, unionId: String?): FanInfo {
//        return FanInfo(openId, unionId)
//    }
//
//
//    fun getFanInfoByUId(uId: String): FanInfo? {
//        return accountService.findById(uId)?.let {
//            if(it.corpId != null)
//                getFanInfo(it.toVID())
//            else null
//        }
//    }
//}