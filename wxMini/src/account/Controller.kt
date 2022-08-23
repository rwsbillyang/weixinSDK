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
import com.github.rwsbillyang.ktorKit.server.AuthUserInfo
import com.github.rwsbillyang.ktorKit.server.IAuthUserInfo
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.wxMini.WxMiniApi
import com.github.rwsbillyang.wxUser.account.Account
import com.github.rwsbillyang.wxUser.account.AccountControllerBase
import com.github.rwsbillyang.wxUser.account.AuthBean
import com.github.rwsbillyang.wxUser.account.stats.LoginLog
import com.github.rwsbillyang.wxUser.fakeRpc.FanInfo

import org.bson.types.ObjectId

class AccountController(private val service: AccountServiceMini) : AccountControllerBase(service) {

    //https://developers.weixin.qq.com/miniprogram/dev/api-backend/open-api/login/auth.code2Session.html
    fun code2Session(appId: String?, jsCode: String?, needRegister: Int, ip: String?, ua: String?): DataBox<AuthBean> {
        if(appId == null || jsCode == null){
            return DataBox.ko("invalid parameter")
        }

        val req = WxMiniApi().code2Session(appId,jsCode) ?: return DataBox.ko("not config wxMini config?")

        if(req.isOK() && req.openId != null){
            val openId = req.openId
            val unionId = req.unionId
            val user = if (unionId != null) {
                service.findByUnionId(unionId)
            } else {
                service.findByOpenId(openId)
            }

            if(user != null){
                if (user.state == Account.STATE_DISABLED) {
                    return DataBox.ko("user account is disabled")
                }
                return getAuthBeanBox(user, null, LoginLog.LoginType_WECHAT, ip, ua)
            }

            return if(needRegister == 1){
                DataBox("NewUser","new user")//前端根据此结果跳转到注册页面
            }else {
                val newUser = Account(
                    ObjectId(), Account.STATE_ENABLED, System.currentTimeMillis(),
                    openId1 = openId, unionId = unionId, miniId = appId
                )
                getAuthBeanBox(newUser, null, LoginLog.LoginType_WECHAT, ip, ua)
            }

        }else{
            return DataBox.ko(  "${req.errCode}: ${req.errMsg}")
        }
    }

    override fun bonus(account: Account, rcm: String?, agentId: Int?) {
        TODO("Not yet implemented")
    }

    override fun getFanInfo(account: Account): FanInfo? {
        TODO("Not yet implemented")
    }

    override fun getJwtToken(account: Account, agentId: Int?, uId: String, role: String, level: String): String? {
        val jti = WXBizMsgCrypt.getRandomStr(8)
        return jwtHelper.generateToken(
            jti, hashMapOf(
                IAuthUserInfo.KEY_UID to uId,
                AuthUserInfo.KEY_ROLE to role,
                AuthUserInfo.KEY_LEVEL to level
            )
        )
    }
}