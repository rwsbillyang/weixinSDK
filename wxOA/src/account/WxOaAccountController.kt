/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-10-07 20:06
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

package com.github.rwsbillyang.wxOA.account

import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.Role
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxOA.fan.FanService
import com.github.rwsbillyang.wxUser.account.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.bson.types.ObjectId
import org.koin.core.component.inject


class WxOaAccountController(private val accountService: AccountService)
    : AccountControllerBase(accountService) {

    private val recommendHelper: RecommendHelper by inject()
    private val fanService: FanService by inject()

    private val wxOaAccountService: WxOaAccountService by inject()

    /**
     * @param registerType 1: 明确要求新用户需注册为系统用户； 2 自动注册为系统用户； 其它值：无需注册系统用户（无系统账号）
     *
     * */
    suspend fun login(guest: WxOaGuest, loginType: String,scanQrcodeId: String?,
                      registerType: Int?, rcm: String?, ip: String?, ua: String?
    ): DataBox<WxOaAccountAuthBean>{
        //检查参数合法性
        if(guest.openId.isNullOrBlank()){
            log.warn("invalid parameter: no  openId when login")
            return DataBox.ko("invalid parameter: no openId")
        }

        //是否已经存在账户，存在直接返回authToken；不存在则创建账户，或返回NewUser，前端跳转到用户协议或注册页面
        val wxOaAccount = wxOaAccountService.findWxOaAccount(guest.unionId, guest.openId,guest.appId)

        val box: DataBox<WxOaAccountAuthBean> = if(wxOaAccount == null){// no wxoa account, create it
            val doc = WxOaAccount(ObjectId(), guest.appId,guest.openId, guest.unionId)
            wxOaAccountService.insertOaAccount(doc)
            runBlocking {
                launch {
                    recommendHelper.bonus(doc, rcm)
                }
            }
            tryRegister(registerType,  loginType, guest, doc, ip, ua)
        }else{
            if(wxOaAccount.state == Account.STATE_DISABLED){  //wxOaAccount disabled
                DataBox.ko("no permission: wxOaAccount disabled")
            }else{
                if(wxOaAccount.sysId == null){// not bond to system account
                    tryRegister(registerType, loginType, guest, wxOaAccount, ip, ua)
                }else{//有绑定的system accountId
                    val sysAccount = accountService.findOne(wxOaAccount.sysId)
                    if(sysAccount == null){ //但没找到sysAccount
                        tryRegister(registerType, loginType, guest, wxOaAccount, ip, ua)
                    }else{ //找到了system account
                        if(sysAccount.state == Account.STATE_DISABLED){//检查状态
                            DataBox.ko("no permission: account disabled")
                        }else{//一切OK

                            val p = fanService.getProfile(guest.openId, guest.unionId)
                            val authBean = getAuthBean(
                                mergeRoles(wxOaAccount.roles, sysAccount, listOf(Role.admin.name)),
                                sysAccount,wxOaAccount.expire, p, wxOaAccount.gId,//用于merge信息
                                loginType, wxOaAccount.appId, wxOaAccount._id, ip, ua//统计信息
                            )
                            if(authBean == null)
                                DataBox.ko("fail to generate token")
                            else
                                DataBox.ok(WxOaAccountAuthBean(wxOaAccount._id.to64String(), authBean, guest))
                        }
                    }
                }
            }
        }

        if(scanQrcodeId != null){
            sentAuthBoxToPcBySocket(scanQrcodeId, ApiJson.serializeJson.encodeToString(box))
        }
        return box
    }


    /**
     * @param registerType 1: 明确要求新用户需注册为系统用户； 2 自动注册为系统用户； 其它值：无需注册系统用户（无系统账号），只作为企业微信用户
     *
     * */
    private fun tryRegister(registerType: Int?, loginType: String, guest: WxOaGuest, wxOaAccount: WxOaAccount, ip: String?, ua: String?): DataBox<WxOaAccountAuthBean>
    {
        //需要明确注册
        if(registerType == 1)
            return DataBox("NewUser","new user")//前端根据此结果跳转到注册页面

        val p = fanService.getProfile(guest.openId!!, guest.unionId)
        val sysAccount = if(registerType == 2){//自动注册为系统用户, 一般不要这么做
            Account(ObjectId(), profile = p).also { accountService.insertOne(it) }
        }else null //默认无需注册

        val authBean = getAuthBean(
            mergeRoles(wxOaAccount.roles, sysAccount, listOf(Role.admin.name)),
            sysAccount,wxOaAccount.expire, p, wxOaAccount.gId,//用于merge信息
            loginType, wxOaAccount.appId, wxOaAccount._id, ip, ua//统计信息
        )
            ?: return DataBox.ko("fail to generate token")


        return DataBox.ok(WxOaAccountAuthBean(wxOaAccount._id.to64String(), authBean, guest))
    }





    /**
     * 系统用户，通过企业微信某一个链接，进行绑定认证
     * 获取到的WxWorkGuest信息绑定system account上
     * */
    fun bindAccount(systemAccountId: String?, guest: WxOaGuest): DataBox<Int> {
        if(systemAccountId == null) return DataBox.ko("invalid parameter: no uId")
        if(guest.openId == null){
            return DataBox.ko("invalid parameter: no openId or no appId")
        }
        val wxOaAccount = wxOaAccountService.findWxOaAccount(guest.unionId, guest.openId, guest.appId)
        if(wxOaAccount == null){
            val doc = WxOaAccount(ObjectId(), guest.appId, guest.openId, guest.unionId, systemAccountId.toObjectId())
            wxOaAccountService.insertOaAccount(doc)
        }else{
            wxOaAccountService.updateSystemAccountId(wxOaAccount._id, systemAccountId)
        }


        return DataBox.ok(1)
    }



    fun findAccountList(params: WxOaAccountListParams) = DataBox.ok(wxOaAccountService.findWxAccountList(params))

    fun permitLevel(accountId: String?): DataBox<Int>{
        if(accountId == null){
            return DataBox.ko("invalid parameter, no accountId")
        }
        val a = wxOaAccountService.findWxOaAccount(accountId.toObjectId())
        val e = if(a?.expire == null && a?.sysId != null) {//自己为空时尝试使用关联的系统account中的expireInfo
            val sys = accountService.findOne(a.sysId)
            sys?.expire
        }else a?.expire

        return DataBox.ok(permittedLevel(e))
    }


//
//    /**
//     * 注册
//     * @param bean oauth认证信息
//     * */
//    fun register(bean: GuestOAuthBean, ip: String?, ua: String?): DataBox<AuthBean> {
//        //log.info("register: loginParam=$loginParam")
//        var isInsert = false
//        val account = when {
//            bean.unionId != null -> service.findByUnionId(bean.unionId!!)
//            bean.openId1 != null -> service.findByOpenId(bean.openId1!!)
//            else -> {
//                log.warn("unionId or openId1 is null")
//                return DataBox.ko("invalid parameter: unionId or openId1 is null")
//            }
//        } ?: (Account(
//            ObjectId(), Account.STATE_ENABLED, System.currentTimeMillis(),
//            openId1 = bean.openId1, unionId = bean.unionId, appId = bean.appId
//        ).also {
//            service.insertAccount(it)
//            isInsert = true
//        })
//
//        return tryBonus(account, isInsert, bean.rcm, bean.agentId, LoginLog.LoginType_WECHAT, ip, ua)
//    }
//
//
//    fun isUser(loginParam: LoginParamBean): DataBox<Boolean> {
//        val user: Account? = when (loginParam.type) {
//            LoginParamBean.WECHAT -> {
//                service.findByOpenId(loginParam.name)
//            }
//            else -> {
//                throw BizException("not support type:${loginParam.type}")
//            }
//        }
//        return DataBox.ok(user != null)
//    }
//
//
//    /**
//     * 企业微信中的oauth身份认证信息绑定到微信创建的account账户uId上
//     * */
//    fun bindAccount(uId: String?, bean: GuestOAuthBean): DataBox<Long> {
//        if (uId == null) return DataBox.ko("invalid parameter: no uId")
//        if (bean.corpId == null || (bean.userId == null && bean.openId2 == null)) {
//            return DataBox.ko("invalid parameter: no corpId or userId/openId2 is null")
//        }
//
//        val count = service.updateOneById(
//            uId, combine(
//                setValue(Account::openId2, bean.openId2),
//                setValue(Account::userId, bean.userId),
//                setValue(Account::corpId, bean.corpId)
//            )
//        ).matchedCount
//
//        if (count == 0L) {
//            return DataBox.ko("no account matched, parameter(uId) is correct?")
//        }
//
//        return DataBox.ok(count)
//        // val account = accountService.findById(uId)?: return DataBox.ko("not found account: uId=$uId")
//    }


//    //针对公众号的, 用于注册推荐奖励
//    override fun bonus(account: Account, rcm: String?, agentId: Int?) = recommendHelper.bonus(account, rcm, agentId)
//
//    //针对公众号的, 用于填充登录authBean
//    override fun getFanInfo(account: Account): FanInfo? {
//        return account.openId1?.let {
//            fanClient.getFanInfo(it, account.unionId)
//        }
//    }

    //针对公众号的, 用于填充登录authBean
//    override fun getJwtToken(account: Account, agentId: Int?, uId: String, role: String, level: String): String? {
//        val jti = WXBizMsgCrypt.getRandomStr(8)
//        return jwtHelper.generateToken(
//            jti, hashMapOf(
//                IAuthUserInfo.KEY_UID to uId,
//                AuthUserInfo.KEY_ROLE to role,
//                AuthUserInfo.KEY_LEVEL to level
//            )
//        )
//    }

}
