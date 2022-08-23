/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-10-07 20:11
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

package com.github.rwsbillyang.wxUser.account.webAdmin

import com.github.rwsbillyang.ktorKit.apiBox.DataBox


import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.server.AuthUserInfo
import com.github.rwsbillyang.ktorKit.server.BizException
import com.github.rwsbillyang.ktorKit.server.IAuthUserInfo
import com.github.rwsbillyang.ktorKit.toObjectId


import com.github.rwsbillyang.wxUser.account.stats.LoginLog
import com.github.rwsbillyang.ktorKit.util.EmailSender
import com.github.rwsbillyang.ktorKit.util.isEmail
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxUser.account.*
import com.github.rwsbillyang.wxUser.fakeRpc.FanInfo



/**
 * 普通网页的账户密码，手机号登录和注册
 * */
class AccountControllerWebAdmin(private val cache: ICache, private val service: AccountServiceWebAdmin): AccountControllerBase(service) {

    private val emailSender = EmailSender("qh_noreply@mail.github.rwsbillyang.com")

    /**
     * 支持账号密码登录、手机号+验证码登录、微信openId和unionId登录
     * */
    fun login(loginParam: LoginParamBean, ip: String?, ua: String?): DataBox<AuthBean> {
        log.info("param: $loginParam")
        val user = when (loginParam.type) {
            LoginParamBean.MOBILE -> {
                val code = cache["phone/${loginParam.name}"]
                if (loginParam.pwd != code) {
                    log.warn("wrong verify code: ${loginParam.pwd}, correct code=$code")
                    return DataBox.ko("wrong verify code:${loginParam.pwd}")
                }
                service.findByTel(loginParam.name)
            }
            LoginParamBean.ACCOUNT -> {
                service.findByName(loginParam.name)
            }
            else -> {
                return DataBox.ko("not support type:${loginParam.type}")
            }
        } ?: return DataBox.newUser("no user account: ${loginParam.name}")

        if (LoginParamBean.ACCOUNT == loginParam.type) {
            if (loginParam.pwd == null) return DataBox.ko("wrong pwd")
            val pwd = Account.encryptPwd(loginParam.pwd, user.salt ?: "")
            if (pwd != user.pwd) {
                log.info("user.pwd=${user.pwd}, input pwd=$pwd")
                return DataBox.ko("wrong account")
            }
        }

        return getAuthBeanBox(user, null, LoginLog.fromLoginParam(loginParam), ip, ua)
    }


    /**
     * 支持账号密码注册、手机号+验证码注册、微信openId和unionId注册
     * @param loginParam
     * @param rcm 推荐人
     * */
    fun register(loginParam: LoginParamBean, rcm: String?, ip: String?, ua: String?): DataBox<AuthBean> {
        log.info("loginParam=$loginParam")
        var isInsert = false
        val user: Account? = when (loginParam.type) {
            LoginParamBean.MOBILE -> {
                val code = cache["phone/${loginParam.name}"]
                if (loginParam.pwd != code) {
                    log.warn("wrong verify code: ${loginParam.pwd}, correct code=$code")
                    throw BizException("wrong verify code:${loginParam.pwd}")
                }
                val a = service.findUpsertByTel(loginParam.name)
                if(a == null) isInsert = true
                a?:service.findByTel(loginParam.name)
            }
            LoginParamBean.ACCOUNT -> {
                if (loginParam.pwd == null) return DataBox.ko("wrong pwd")
                val salt = WXBizMsgCrypt.getRandomStr(8)
                val a = service.findUpsertByName(loginParam.name, Account.encryptPwd(loginParam.pwd, salt), salt)
                if(a == null) isInsert = true
                a?:service.findByName(loginParam.name)
            }

            else -> {
                throw BizException("not support type:${loginParam.type}")
            }
        }

        return tryBonus(user, isInsert, rcm, null, LoginLog.LoginType_ACCOUNT, ip, ua)
    }



    /**
     * 获取验证码
     * */
    fun code(phone: String?): DataBox<String> {
        if (phone.isNullOrBlank())
            throw BizException("invalid parameter")
        val code = WXBizMsgCrypt.getRandomStr(4)
        //TODO: send code to phone

        //TODO: 若多app部署，则需使用redisCache
        cache.put("phone/$phone", code)
        return DataBox.ok(code)
    }


    private fun sendCodeByEmail(name: String, email: String, code: String): Boolean {
        val pair = subjectAndBody(name, code)
        return emailSender.sendEmail(
            pair.first, pair.second,
            email, emailSender.smtpHost != null
        )
    }

    private fun subjectAndBody(name: String, code: String, isReset: Boolean = false): Pair<String, String> {
        val str = if (isReset) "reset" else "created"
        return Pair(
            "$name, your password was $str.",
            "Hello $name,<br/><br/>We wanted to let you know that your QH password was $str: $code, please don't tell anyone else.<br/><br/> " +
                    "This mail was automatically created by qh system, please don't reply."
        )
    }

    fun resetPwd(email: String?): DataBox<Long> {
        if (email.isNullOrBlank()) {
            return DataBox.ko("invalid parameter")
        }
        if (!email.isEmail()) {
            return DataBox.ko("invalid email")
        }
        val u = service.findByName(email)
        if (u == null) {
            return DataBox.ko("not found account: $email")
        } else {
            val rawCode = WXBizMsgCrypt.getRandomStr(6)
            val salt = WXBizMsgCrypt.getRandomStr(8)
            val pwd = Account.encryptPwd(rawCode, salt)

            val pair = subjectAndBody(u.name ?: "", rawCode, true)
            val ret = emailSender.sendEmail(pair.first, pair.second, email, emailSender.smtpHost != null)
            return if (ret) {
                val result = service.updatePwdAndSalt(u, pwd, salt)
                DataBox.ok(result.modifiedCount)
            } else {
                log.error("fail to send email when reset pwd: $email ")
                DataBox.ko("fail to send email, please contact to administrator")
            }
        }
    }

    fun findProfile(uId: String?): DataBox<Profile> {
        return if (uId.isNullOrBlank())
            DataBox.ko("invalid parameter")
        else {
            //antd admin 必须返回有数据，否则导致重新登录
            val p = service.findProfile(uId)
                ?: service.findById(uId)?.let {
                    val account = it
//                    val p2 = it.oId?.let {
//                        fanClient.getFanInfo(it, account.uId)?.let {
//                            Profile(account._id, it.nick, it.avatar)
//                        }
//                    }
//                    p2 ?: Profile(uId.toObjectId(), account.name)
                    Profile(uId.toObjectId(), account.name)
                } ?: return DataBox.ko("not found account: id=$uId")

            DataBox.ok(p)
        }

    }

    fun isUser(loginParam: LoginParamBean): DataBox<Boolean> {
        val user: Account? = when (loginParam.type) {
            LoginParamBean.MOBILE -> {
                service.findByTel(loginParam.name)
            }
            LoginParamBean.ACCOUNT -> {
                service.findByName(loginParam.name)
            }
            else -> {
                throw BizException("not support type:${loginParam.type}")
            }
        }
        return DataBox.ok(user != null)
    }

    override fun bonus(account: Account, rcm: String?, agentId: Int?) {
        log.info("Not yet implemented")
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
    override fun getFanInfo(account: Account) = FanInfo(account.openId1?:"noOpenId", account.unionId, null, account.name)
}