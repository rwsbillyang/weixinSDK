/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-21 18:03
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

package com.github.rwsbillyang.wxUser.account


import com.github.rwsbillyang.ktorKit.AbstractJwtHelper
import com.github.rwsbillyang.ktorKit.Role
import com.github.rwsbillyang.ktorKit.WsSessions
import com.github.rwsbillyang.ktorKit.apiJson.ApiJson
import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.apiJson.to64String
import com.github.rwsbillyang.ktorKit.apiJson.toObjectId
import com.github.rwsbillyang.wxUser.account.stats.LoginLog
import com.github.rwsbillyang.wxUser.account.stats.StatsService
import com.github.rwsbillyang.wxUser.fakeRpc.EditionLevel
import com.github.rwsbillyang.wxUser.fakeRpc.FanInfo
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.apache.commons.codec.digest.DigestUtils
import org.bson.types.ObjectId
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.litote.kmongo.setValue
import org.slf4j.LoggerFactory

abstract class AccountControllerBase(private val accountService: AccountServiceBase) : KoinComponent {
    protected val log = LoggerFactory.getLogger("AccountControllerBase")

    protected val statsService: StatsService by inject() //只需一个子类实例化
    protected val jwtHelper: AbstractJwtHelper by inject()

    private val wsSessions: WsSessions by inject()

    fun md5(ua: String?) = ua?.let {
        val md5 = DigestUtils.md5Hex(it)
        statsService.upsertMd5Ua(md5, it)
        md5
    }

    //用于添加统计数据，和生成authBean
    fun insertStats(account: Account, ip: String?, ua: String?, loginType: Char, token: String) = GlobalScope.launch {
        val now = System.currentTimeMillis()
        val loginLog = LoginLog(ObjectId(), account._id, now, loginType, ip, md5(ua))
        statsService.insertLoginLog(loginLog)
        statsService.upsertLoginToken(account._id, now, token)
    }

    suspend fun sentAuthBoxToPcBySocket(sessionId: String?, box: DataBox<AuthBean> ){
        if (sessionId != null) {
            //手机显示登录成功，PC显示登录登录成功后跳转到后台，需给PC发送登录认证消息
            val text = ApiJson.json.encodeToString(box)
            wsSessions.session(sessionId)?.send(Frame.Text("json=$text"))
            wsSessions.removeSession(sessionId)
        }else{
            log.warn("sessionId is null when scanCode done")
        }
    }
    /**
     * 生成JWT token后构造一个AuthBean，用于给前端
     * */
    fun getAuthBeanBox(account: Account, agentId: Int?, loginType: Char, ip: String?, ua: String?): DataBox<AuthBean> {
        if (account.state == Account.STATE_DISABLED) {
            return DataBox.ko("user account is disabled")
        }
        val uId = account._id.to64String()
        val ae = accountService.getExpireInfo(uId, agentId)
        val role = ae?.role ?: listOf(Role.user.name)
        val level = ae?.level ?: EditionLevel.Free

        //val token = (jwtHelper as UserInfoJwtHelper).generateToken(uId, level, role)
        val token = getJwtToken(account, agentId, uId, role.joinToString(","), level.toString())
            ?: return DataBox.ko("fail to generate token")
        val fanInfo = getFanInfo(account)

        insertStats(account, ip, ua, loginType, token)

        return DataBox.ok(
            AuthBean(
                uId,
                token,
                level,
                ae?.expire,
                role,
                account.gId?.map { it.to64String() },
                account.unionId,
                account.openId1,
                account.openId2,
                account.appId,
                account.miniId,
                account.userId,
                null,
                account.corpId,
                agentId,
                account.suiteId,
                avatar = fanInfo?.avatar,
                nick = account.nick?:account.name?:fanInfo?.nick, //优先使用account中的名称
                ext = account.ext,
                permittedlevel = accountService.permittedLevel(uId, agentId)
            )
        )
    }

    fun updateExt(id: String, ext: String?) =
        DataBox.ok(accountService.updateOneById(id, setValue(Account::ext, ext)).modifiedCount)
    fun updateNick(id: String, nick: String?) =
        DataBox.ok(accountService.updateOneById(id, setValue(Account::nick, nick)).modifiedCount)

    fun tryBonus(
        account: Account?,
        isInsert: Boolean,
        rcm: String?,
        agentId: Int?,
        loginType: Char,
        ip: String?,
        ua: String?
    ): DataBox<AuthBean> {
        if (account == null) {
            val msg = "fail to insert"
            log.warn(msg)
            return DataBox.ko(msg)
        } else {
            if (isInsert && rcm != null) {
                bonus(account, rcm, agentId)
            }
        }
        //val ae = service.findAccountExpireById(AccountExpire.id(account._id.to64String(), agentId))
        return getAuthBeanBox(account, agentId, loginType, ip, ua)
    }

    /**
     * 每个用户只能使用最新的token进行登录，过去的作废 增加安全性
     * */
    fun isTokenValid(uId: String, token: String?) =
        !token.isNullOrBlank() && token == statsService.findLoginToken(uId.toObjectId())?.token

    /**
     * 前端需要根据用户的level进行显示控制，如文章详情页中的名片，
     * 过期后的用户统一归集到免费用户
     * */
    fun permittedLevel(owner: String?, agentId: Int?): DataBox<Int> {
        return if (owner.isNullOrBlank())
            DataBox.ko("invalid parameter")
        else {
            DataBox.ok(accountService.permittedLevel(owner, agentId))
        }
    }

    //公众号和企业微信，token里面包含的内容有所不同
    abstract fun getJwtToken(account: Account, agentId: Int?, uId: String, role: String, level: String): String?

    //注册后的推荐奖励
    abstract fun bonus(account: Account, rcm: String?, agentId: Int?)

    //登录时nick和avatar
    abstract fun getFanInfo(account: Account): FanInfo?

    fun findGroupList(params: GroupListParams) = accountService.findGroupList(params.toFilter(), params.pagination, params.lastId)
    fun findGroup(id: String) = accountService.findGroup(id)
    fun saveGroup(doc: GroupBean) = accountService.saveGroup(doc)
    fun delGroup(id: String) = accountService.delGroup(id)

    fun joinGroup(uId:String, groupId: String) = accountService.joinGroup(uId, groupId)
}