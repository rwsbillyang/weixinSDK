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

import com.github.rwsbillyang.ktorKit.*

import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.wxOA.fakeRpc.FanRpcOA
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxUser.account.*

import com.github.rwsbillyang.wxUser.account.stats.LoginLog
import com.github.rwsbillyang.wxUser.fakeRpc.FanInfo

import org.bson.types.ObjectId
import org.koin.core.inject
import org.litote.kmongo.*


class AccountControllerOA(private val service: AccountServiceOA) : AccountControllerBase(service) {

    private val recommendHelper: RecommendHelper by inject()
    private val fanClient: FanRpcOA by inject()

    /**
     * 微信openId和unionId登录
     * */
    suspend fun login(loginParam: LoginParamBean, ip: String?, ua: String?): DataBox<AuthBean> {
        //log.info("param: $loginParam")
        val user = when (loginParam.type) {
            LoginParamBean.WECHAT -> {
                val openId = loginParam.name
                val unionId = loginParam.pwd
                if (unionId != null) {
                    service.findByUnionId(unionId)
                } else {
                    service.findByOpenId(openId)
                }
            }
            LoginParamBean.SCAN_QRCODE -> {
                val openId = loginParam.name
                service.findByOpenId(openId)
            }
            else -> {
                throw BizException("not support type:${loginParam.type}")
            }
        } ?: return DataBox.newUser("no user account: ${loginParam.name}")

        if (user.state == Account.STATE_DISABLED) {
            return DataBox.ko("user account is disabled")
        }

        //公众号中的权限信息存于Account中
        val box = getAuthBeanBox(user, null, LoginLog.LoginType_WECHAT, ip, ua)

        //扫码登录成功后，通过wsSocket通知PC端
        if (loginParam.type == LoginParamBean.SCAN_QRCODE) {
            sentAuthBoxToPcBySocket(loginParam.pwd, box)
        }
        return box
    }


    /**
     * 注册
     * @param bean oauth认证信息
     * */
    fun register(bean: GuestOAuthBean, ip: String?, ua: String?): DataBox<AuthBean> {
        //log.info("register: loginParam=$loginParam")
        var isInsert = false
        val account = when {
            bean.unionId != null -> service.findByUnionId(bean.unionId!!)
            bean.openId1 != null -> service.findByOpenId(bean.openId1!!)
            else -> {
                log.warn("unionId or openId1 is null")
                return DataBox.ko("invalid parameter: unionId or openId1 is null")
            }
        } ?: (Account(
            ObjectId(), Account.STATE_ENABLED, System.currentTimeMillis(),
            openId1 = bean.openId1, unionId = bean.unionId, appId = bean.appId
        ).also {
            service.insertAccount(it)
            isInsert = true
        })

        return tryBonus(account, isInsert, bean.rcm, bean.agentId, LoginLog.LoginType_WECHAT, ip, ua)
    }


    fun isUser(loginParam: LoginParamBean): DataBox<Boolean> {
        val user: Account? = when (loginParam.type) {
            LoginParamBean.WECHAT -> {
                service.findByOpenId(loginParam.name)
            }
            else -> {
                throw BizException("not support type:${loginParam.type}")
            }
        }
        return DataBox.ok(user != null)
    }


    /**
     * 企业微信中的oauth身份认证信息绑定到微信创建的account账户uId上
     * */
    fun bindAccount(uId: String?, bean: GuestOAuthBean): DataBox<Long> {
        if (uId == null) return DataBox.ko("invalid parameter: no uId")
        if (bean.corpId == null || (bean.userId == null && bean.openId2 == null)) {
            return DataBox.ko("invalid parameter: no corpId or userId/openId2 is null")
        }

        val count = service.updateOneById(
            uId, combine(
                setValue(Account::openId2, bean.openId2),
                setValue(Account::userId, bean.userId),
                setValue(Account::corpId, bean.corpId)
            )
        ).matchedCount

        if (count == 0L) {
            return DataBox.ko("no account matched, parameter(uId) is correct?")
        }

        return DataBox.ok(count)
        // val account = accountService.findById(uId)?: return DataBox.ko("not found account: uId=$uId")
    }


    //针对公众号的, 用于注册推荐奖励
    override fun bonus(account: Account, rcm: String?, agentId: Int?) = recommendHelper.bonus(account, rcm, agentId)

    //针对公众号的, 用于填充登录authBean
    override fun getFanInfo(account: Account): FanInfo? {
        return account.openId1?.let {
            fanClient.getFanInfo(it, account.unionId)
        }
    }

    //针对公众号的, 用于填充登录authBean
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

    fun findAccountList(params: AccountListParams): DataBox<List<AccountBean>> {
        val list = service.findAccountList(params.toFilter(), params.pagination, params.lastId)
        list.forEach {
            if (it.name == null && it.openId1 != null) {
                val f = fanClient.getFanInfo(it.openId1!!, null)
                it.name = f.nick
            }
        }
        return DataBox.ok(list)
    }
}
