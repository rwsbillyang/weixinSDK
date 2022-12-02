/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-02-02 16:40
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

package com.github.rwsbillyang.wxSDK.wxMini.account


import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.Role
import com.github.rwsbillyang.ktorKit.to64String
import com.github.rwsbillyang.wxSDK.wxMini.WxMiniApi
import com.github.rwsbillyang.wxUser.account.Account
import com.github.rwsbillyang.wxUser.account.AccountControllerBase
import com.github.rwsbillyang.wxUser.account.AccountService
import com.github.rwsbillyang.wxUser.account.LoginParamBean
import org.bson.types.ObjectId
import org.koin.core.component.inject

class WxMiniAccountController(private val accountService: AccountService) : AccountControllerBase(accountService) {

    private val wxMiniAccountService: WxMiniAccountService by inject()
    //https://developers.weixin.qq.com/miniprogram/dev/api-backend/open-api/login/auth.code2Session.html
    fun code2Session(appId: String?, jsCode: String?, registerType: Int?, ip: String?, ua: String?): DataBox<WxMiniAccountAuthBean> {
        if (appId == null || jsCode == null) {
            return DataBox.ko("invalid parameter")
        }

        val req = WxMiniApi().code2Session(appId, jsCode) ?: return DataBox.ko("not config wxMini config?")

        if (req.isOK() && req.openId != null) {
            val openId = req.openId
            val unionId = req.unionId
            val guest = WxMiniGuest(appId, openId, unionId)
            //是否已经存在账户，存在直接返回authToken；不存在则创建账户，或返回NewUser，前端跳转到用户协议或注册页面
            val user = wxMiniAccountService.findWxMiniAccount(unionId, openId, appId)

            val loginType = LoginParamBean.WXMINI
            val box: DataBox<WxMiniAccountAuthBean> = if (user == null) {// no wxoa account, create it
                val doc = WxMiniAccount(ObjectId(), appId, openId, unionId)
                wxMiniAccountService.insertMiniAccount(doc)

                tryRegister(registerType, loginType, guest, doc, ip, ua)
            } else {
                if (user.state == Account.STATE_DISABLED) {  //wxOaAccount disabled
                    DataBox.ko("no permission: wxMiniAccount disabled")
                } else {
                    if (user.sysId == null) {// not bond to system account
                        tryRegister(registerType, loginType, guest, user, ip, ua)
                    } else {//有绑定的system accountId
                        val sysAccount = accountService.findOne(user.sysId)
                        if (sysAccount == null) { //但没找到sysAccount
                            tryRegister(registerType, loginType, guest, user, ip, ua)
                        } else { //找到了system account
                            if (sysAccount.state == Account.STATE_DISABLED) {//检查状态
                                DataBox.ko("no permission: account disabled")
                            } else {//一切OK

                                //val p = fanService.getProfile(guest.openId, guest.unionId)
                                val authBean = getAuthBean(
                                    mergeRoles(user.roles, sysAccount, listOf(Role.admin.name)),
                                    sysAccount,user.expire, null, user.gId,//用于merge信息
                                    loginType, user.appId, user._id, ip, ua//统计信息
                                )
                                if (authBean == null)
                                    DataBox.ko("fail to generate token")
                                else
                                    DataBox.ok(WxMiniAccountAuthBean(user._id.to64String(), authBean, guest))
                            }
                        }
                    }
                }
            }


            return box
        }else
            return DataBox.ko(  "${req.errCode}: ${req.errMsg}" )
    }

        /**
         * @param registerType 1: 明确要求新用户需注册为系统用户； 2 自动注册为系统用户； 其它值：无需注册系统用户（无系统账号），只作为企业微信用户
         *
         * */
        private fun tryRegister(registerType: Int?, loginType: String, guest: WxMiniGuest, wxMiniAccount: WxMiniAccount, ip: String?, ua: String?): DataBox<WxMiniAccountAuthBean>
        {
            //需要明确注册
            if(registerType == 1)
                return DataBox("NewUser","new user")//前端根据此结果跳转到注册页面

            //val p = fanService.getProfile(guest.openId!!, guest.unionId)
            val sysAccount = if(registerType == 2){//自动注册为系统用户, 一般不要这么做
                Account(ObjectId()).also { accountService.insertOne(it) }
            }else null //默认无需注册

            val authBean = getAuthBean(
                mergeRoles(wxMiniAccount.roles, sysAccount, listOf(Role.admin.name)),
                sysAccount,wxMiniAccount.expire, null, wxMiniAccount.gId,//用于merge信息
                loginType, wxMiniAccount.appId, wxMiniAccount._id, ip, ua//统计信息
            )?: return DataBox.ko("fail to generate token")


            return DataBox.ok(WxMiniAccountAuthBean(wxMiniAccount._id.to64String(), authBean, guest))
        }


}