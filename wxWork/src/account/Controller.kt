/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-03 21:55
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

package com.github.rwsbillyang.wxWork.account


import com.github.rwsbillyang.ktorKit.server.AuthUserInfo
import com.github.rwsbillyang.ktorKit.server.IAuthUserInfo
import com.github.rwsbillyang.ktorKit.server.Role
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.isv.AgentInfo
import com.github.rwsbillyang.wxUser.account.*
import com.github.rwsbillyang.wxUser.account.stats.LoginLog
import com.github.rwsbillyang.wxUser.fakeRpc.FanInfo
import com.github.rwsbillyang.wxWork.agent.AgentService
import com.github.rwsbillyang.wxWork.contacts.ContactHelper
import com.github.rwsbillyang.wxWork.contacts.ContactService
import com.github.rwsbillyang.wxWork.fakeRpc.FanRpcWork
import com.github.rwsbillyang.wxWork.isv.CorpInfo
import com.github.rwsbillyang.wxWork.isv.IsvCorpService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.litote.kmongo.combine
import org.litote.kmongo.setValue


/**
 * 统一第三方和内建应用的登录机制，并添加进版本和期限等权限管理
 *
 * 不使用user中的account，因为不希望引用SOFARpc，简化设计，只用在企业微信中
 * */
class AccountControllerWork(private val accountService: AccountServiceWxWork): AccountControllerBase(accountService) {

    companion object {
        const val KEY_CORP_ID = "corpId"
        const val KEY_AGENT_ID = "agentId"
        const val KEY_USER_ID = "userId"
    }

    private val agentService: AgentService by inject()
    private val isvCorpService: IsvCorpService by inject()

    private val contactHelper: ContactHelper by inject()
    private val contactService: ContactService by inject()

    private val recommendHelper: RecommendHelper by inject()

    private val fanClient: FanRpcWork by inject()


    /**
     * 前端在得到oauth认证身份信息后，将得到的信息提交上来，换取account._id和jwt token。
     * 首先：检查参数合法性
     * 其次：检查登录用户是否在可见范围内
     * 再次：检查用户是否有account账户，没有的话分配一个（同时同步用户的外部联系人及其详情，包括将userId转换得到openId）；若存在的话直接构建AuthBean
     * 最后，生成jwt token返回登录信息
     * */
    suspend fun login(bean: GuestOAuthBean, ip: String?, ua: String?, needRegister: Int, scanQrcodeId: String?): DataBox<AuthBean>{
        //检查参数合法性
        if(bean.userId.isNullOrBlank() || bean.corpId.isNullOrBlank()){
            log.warn("invalid parameter: no userId or no openId when login")
            return DataBox.ko("invalid parameter: no userId or no openId")
        }
        val userId = bean.userId!!
        val corpId = bean.corpId!!
        var agentInfo: AgentInfo? = null
        val agentId = if(Work.isIsv){
            agentInfo = agentInCorpInfo(userId, corpId, bean.suiteId)
            agentInfo?.agentid?: return DataBox.ko("no agentId in corpInfo for ISV")
        }else bean.agentId

        if(agentId == null)
        {
            log.warn("invalid parameter: no agentId")
            return DataBox.ko("invalid parameter: no agentId")
        }

        //检查用户可见性
        val visible = isVisible(userId, corpId, agentId)
        if(Work.isIsv){
            if(!visible)
            {
                return if(agentInfo!!.auth_mode == 1){//1为成员授权
                    log.warn("not add agent: userId=$userId for isv")
                    DataBox("SelfAuth","请到第三方应用市场，添加应用")
                }else{
                    log.warn("no permission: userId=$userId for isv")
                    DataBox.ko("请联系公司企业微信管理员，将您添加到可见范围名单中")
                }
            }
        }else if(!visible)
            return DataBox.ko("您不在应用可见范围内，请联系公司微信管理员")


        //是否已经存在账户，存在直接返回authToken；不存在则创建账户，或返回NewUser，前端跳转到用户协议或注册页面
        val account = accountService.findByCorpIdUserId(corpId,userId,bean.openId2)?:(
                if(needRegister == 1){
                    return DataBox("NewUser","new user")//前端根据此结果跳转到注册页面
                }else{
                    Account(ObjectId(),Account.STATE_ENABLED, System.currentTimeMillis(),userId = userId,   deviceId = bean.deviceId,
                        corpId = corpId, suiteId = bean.suiteId, openId2 = bean.openId2, role = listOf(Role.admin.name)).also {
                        accountService.insert(it)
                        //登录时更新自己的信息，便于比对是否在可见范围内
                        contactHelper.syncContactDetail(userId, corpId, agentId, bean.suiteId)

                        //当用户登录时，获取其外部用户列表: 对于channelHelper应用来说，需要登录用户的全部联系人，而对于zhike来说可能不需要
                        //若是启动时执行同步，将无法确定是哪个用户，如果遍历所有用户，量可能会比较大
                        //登录时执行同步：一是每次登录时同步，二是首次登录时同步
                        runBlocking {
                            launch {
                            contactHelper.syncExternalsOfUser(corpId,agentId,bean.suiteId,userId, 1)
                        } }
                    }
                })
        val box = getAuthBeanBox(account, agentId, LoginLog.LoginType_WXWORK,ip, ua)
        if(scanQrcodeId != null){
            sentAuthBoxToPcBySocket(scanQrcodeId, box)
        }
        return box
    }


    //用于每次访问时检查token有效性，不管是ISV还是内建应用全部基于agent信息表
    //更简单的办法是检查userId是否为空，非空就有可见性，空则不可见。因oauth认证时，只要不可见，就不返回userId
    fun isVisible(userId: String, corpId: String, agentId: Int): Boolean{

        val agent = agentService.findAgent(agentId, corpId)
        if(agent == null){
            log.warn("not found agent: agentId=$agentId, corpId=$corpId")
            return false
        }
        //return agent.userList?.contains(userId)?:false
        //agent.userList 在agent初始化时已包含了部门和tag信息，并且tag更新时也会实时更新
        //使用以下代码对部门进行判断，目的在于更新allow userList出错时，能保证正常登录
        val visible = agent.userList?.contains(userId)?:false
        if(visible) return true

        if(agent.depList.isNullOrEmpty()){
            log.warn("agent no depList and tagList: agentId=$agentId, corpId=$corpId")
            return false
        }

        val contact = contactService.findContact(userId, corpId)
        if(contact == null){
            log.warn("not found contact: userId=$userId, corpId=$corpId")
            return false
        }


        //用户有非空所属部门, 且能与agent中部门匹配上
        val department = contact.department
        if((!department.isNullOrEmpty() && agent.depList.intersect(department).isNotEmpty())
            || agent.depList.contains(contact.mainDep))
            return true

        return false
    }



    /**
     * 注册
     * @param bean oauth认证信息
     * */
    fun register(bean: GuestOAuthBean, ip: String?, ua: String?): DataBox<AuthBean> {
       if(bean.corpId == null || bean.userId == null)
           return DataBox.ko("invalid parameter: corpId or userId is null")

        var account = accountService.findUpsert(bean.corpId!!, bean.openId2,bean.userId!!, bean.deviceId)

        var isInsert = false
        if(account == null){
            account = accountService.findByCorpIdUserId(bean.corpId!!,bean.userId!!, bean.openId2)
            isInsert = true
        }

        return tryBonus(account, isInsert, bean.rcm, bean.agentId, LoginLog.LoginType_WXWORK, ip, ua)
    }

    /**
     * 微信中的oauth身份认证信息绑定到企业微信创建的account账户uId上
     * */
    fun bindAccount(uId: String?, bean: GuestOAuthBean): DataBox<Long> {
        if(uId == null) return DataBox.ko("invalid parameter: no uId")
        if(bean.openId1 == null){
            return DataBox.ko("invalid parameter: no openId")
        }

        val count = accountService.updateOneById(uId, combine(
            setValue(Account::openId1, bean.openId1),
            setValue(Account::unionId, bean.unionId))).matchedCount

        if(count == 0L){
            return DataBox.ko("no account matched, parameter(uId) is correct?")
        }

        return DataBox.ok(count)
       // val account = accountService.findById(uId)?: return DataBox.ko("not found account: uId=$uId")
    }

    override fun bonus(account: Account, rcm: String?, agentId: Int?) = recommendHelper.bonus(account, rcm, agentId)
    override fun getFanInfo(account: Account): FanInfo {
        return fanClient.getFanInfo(account.toVID())
    }
    override fun getJwtToken(account: Account, agentId: Int?, uId: String, role: String, level: String): String?{
        val jti = WXBizMsgCrypt.getRandomStr(8)
        return jwtHelper.generateToken(
            jti, hashMapOf(
                KEY_CORP_ID to account.corpId!!,
                KEY_USER_ID to account.userId!!,
                KEY_AGENT_ID to agentId.toString(),
                IAuthUserInfo.KEY_UID to uId,
                AuthUserInfo.KEY_ROLE to role,
                AuthUserInfo.KEY_LEVEL to level
            )
        )
    }



    //ISV模式下通过企业信息进行判断
    private fun agentInCorpInfo(userId: String?, corpId: String?, suiteId: String?): AgentInfo?{
        if(userId == null || corpId == null || suiteId == null) {
            log.warn("agent is not visible: userId=$userId, corpId=$corpId, suiteId=$suiteId")
            return null
        }

        val corpInfo = isvCorpService.findOneCorpInfo(suiteId, corpId)
        if(corpInfo == null){
            log.warn("not found corpInfo: suiteId=$suiteId，corpId=$corpId")
            return null
        }

        if(corpInfo.status != CorpInfo.STATUS_ENABLED){
            log.warn("corp status not enabled")
            return null
        }

        //对于ISV，也支持从agent中获取isVisible信息，isv的agent也会被同步更新了
        val agent = corpInfo.authInfo?.agent?.firstOrNull()
        if(agent == null){
            log.warn("not found agentInfo: suiteId=$suiteId，corpId=$corpId")
            return null
        }

        //val authMode = agent.auth_mode //授权模式，0为管理员授权；1为成员授权
        return agent

    }
    fun findAccountList(params: AccountListParams) = DataBox.ok(accountService.findAccountList(params))
}