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


import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.*

import com.github.rwsbillyang.ktorKit.to64String

import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxUser.account.stats.LoginLog
import com.github.rwsbillyang.wxUser.account.stats.StatsService

import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.digest.DigestUtils
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.div
import org.litote.kmongo.setValue
import org.slf4j.LoggerFactory

abstract class AccountControllerBase(private val accountService: AccountService) : KoinComponent {
    protected val log = LoggerFactory.getLogger("AccountControllerBase")

    protected val statsService: StatsService by inject() //只需一个子类实例化
    protected val jwtHelper: AbstractJwtHelper by inject()

    private val wsSessions: WsSessions by inject()

    fun md5(ua: String?) = ua?.let {
        val md5 = DigestUtils.md5Hex(it)
        statsService.upsertMd5Ua(md5, it)
        md5
    }

    /**
     * 每个用户只能使用最新的token进行登录，过去的作废 增加安全性
     * */
    fun isTokenValid(uId: String, token: String?) =
        !token.isNullOrBlank() && token == statsService.findLoginToken(uId.toObjectId())?.token



    /**
     * 用于添加统计数据
     * @param accountId wxWorkAccount or wxOA or Account id
     * */
    fun insertStats(sysId: ObjectId?, accountId: ObjectId, appId: String?, ip: String?, ua: String?, loginType: String, token: String) = runBlocking {
        launch {
            val now = System.currentTimeMillis()
            val loginLog = LoginLog(ObjectId(), sysId, accountId, appId, now, loginType, ip, md5(ua))
            statsService.insertLoginLog(loginLog)
            statsService.upsertLoginToken(accountId, now, token)
        }
    }


    suspend fun sentAuthBoxToPcBySocket(sessionId: String?, authBeanJsonStr: String){
        if (sessionId != null) {
            //手机显示登录成功，PC显示登录登录成功后跳转到后台，需给PC发送登录认证消息
            wsSessions.session(sessionId)?.send(Frame.Text("json=$authBeanJsonStr"))
            wsSessions.removeSession(sessionId)
        }else{
            log.warn("sessionId is null when scanCode done")
        }
    }




    fun updateNick(id: String, nick: String?) =
        DataBox.ok(accountService.updateOneById(id, setValue(Account::profile / Profile::nick, nick)).modifiedCount)



    fun findGroupList(params: GroupListParams) = accountService.findGroupList(params)
    fun findGroup(id: String) = accountService.findGroup(id)
    fun saveGroup(doc: GroupBean) = accountService.saveGroup(doc)
    fun delGroup(id: String) = accountService.delGroup(id)

    fun joinGroup(uId:String, groupId: String) = accountService.joinGroup(uId, groupId)



    /**
     * 生成JWT token后构造一个AuthBean，用于给前端
     * @param loginType
     * @param appAccountId  wxWorkAccount or wxOA or wxmini Account id
     * @param sysAccount 绑定的全局账号
     * @param appExpire wxWork或wxOA提供的话，则使用它，否则使用绑定的全局账号中的
     * @param appProfile wxWork或wxOA提供的话，则使用它，否则使用绑定的全局账号中的
     * @param ip
     * @param ua
     * */
    fun getAuthBean(mergedRoles: List<String>?,
                    sysAccount: Account?,appExpire: ExpireInfo?, appProfile: Profile?, appGroupId: List<ObjectId>?,//用于merge信息
                    loginType: String, appId: String?, appAccountId: ObjectId, ip: String?, ua: String? //统计信息
    ): AuthBean? {
        val e = appExpire?: sysAccount?.expire
        val r = mergedRoles?: listOf(Role.guest.name)
        val token = getJwtToken(appAccountId.to64String(), r.joinToString(","), e?.level?:EditionLevel.Free)
            ?: return null

        val sysId = sysAccount?._id?.to64String()
        val p = appProfile?: sysAccount?.profile
        val gId = mergeGroupId(appGroupId, sysAccount)?.map { it.to64String() }

        insertStats(sysAccount?._id, appAccountId, appId, ip, ua, loginType, token)

        return AuthBean(token, sysId, r, e, p, gId)
    }

    /**
     * 优先使用appId中的appRoles，没有的话使用系统级账号roles，再没有使用app级默认roles
     * */
    fun mergeRoles(appRoles: List<String>?, sysAccount: Account?, defaultRoles: List<String>?)
        = (appRoles?:sysAccount?.roles)?:defaultRoles
    fun mergeRoles(appRoles: List<String>?, sysAccountId: ObjectId?, defaultRoles: List<String>?)
            = (appRoles?:sysAccountId?.let{accountService.findOne(it)?.roles})?:defaultRoles

    fun mergeGroupId(appGroupId: List<ObjectId>?, sysAccount: Account?):List<ObjectId>?{
        return if(appGroupId.isNullOrEmpty()) {
            sysAccount?.gId
        }else{
            val list = sysAccount?.gId
            if(list.isNullOrEmpty()){
                appGroupId
            }else{
                appGroupId.union(list).toList()
            }
        }
    }
    fun mergeGroupId(appGroupId: List<ObjectId>?, sysAccountId: ObjectId?) = mergeGroupId(appGroupId, sysAccountId?.let{accountService.findOne(it)})

    fun getJwtToken(uId: String, role: String, level: Int): String? {
        val jti = WXBizMsgCrypt.getRandomStr(8)
        return jwtHelper.generateToken(
            jti, hashMapOf(
                IAuthUserInfo.KEY_UID to uId,
                AuthUserInfo.KEY_ROLE to role,
                AuthUserInfo.KEY_LEVEL to level.toString()
            )
        )
    }

    /**
     * 检查用户权限后的重新设置后的level
     * */
    fun permittedLevel(expireInfo: ExpireInfo?): Int {
        if(expireInfo == null) return EditionLevel.Free
        else {
            val now = System.currentTimeMillis()
            return if (expireInfo.expire < now) EditionLevel.Free else expireInfo.level
        }
    }

}