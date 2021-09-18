/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:38
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

package com.github.rwsbillyang.wxSDK.work


import com.github.rwsbillyang.wxSDK.ResponseCallbackIps
import com.github.rwsbillyang.wxSDK.WxApi
import com.github.rwsbillyang.wxSDK.accessToken.ITimelyRefreshValue
import com.github.rwsbillyang.wxSDK.accessToken.TimelyRefreshAccessToken
import com.github.rwsbillyang.wxSDK.accessToken.TimelyRefreshValue
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkMulti
import com.github.rwsbillyang.wxSDK.work.isv.IsvWorkSingle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import javax.lang.model.element.NestingKind

/**
 * 不同的api可能属于不同的agent，需要维护着自己的secret和accessToken
 *
 * 基础应用secret。某些基础应用（如“审批”“打卡”应用）通讯录管理、外部联系人管理、审批、打卡等基础应用是企业微信默认集
 * 成的应用，可以直接开启并拿到secret。支持通过API进行操作。
 * 如果企业需要开发自定义的应用，进入“企业应用”页面，在“自建应用”栏点击“创建应用”，完成应用的添加和配置，详细步骤请参见应用概述
 *
 * 自建应用secret。在管理后台->“应用与小程序”->“应用”->“自建”，点进某个应用，即可看到。
 *
 * 在管理后台->“应用与小程序”->“应用->”“基础”，点进某个应用，点开“API”小按钮，即可看到。
 *
 * 通讯录管理secret。在“管理工具”-“通讯录同步”里面查看（需开启“API接口同步”）；
 *
 * 客户联系管理secret。在“客户联系”栏，点开“API”小按钮，此为外部联系人secret。
 * 如需使用自建应用调用外部联系人相关的接口，需要在“可调用应用”中进行配置。
 *
 * access_token是企业后台去企业微信的后台获取信息时的重要票据，由corpid和secret产生。
 * 所有接口在通信时都需要携带此信息用于验证接口的访问权限
 *
 * */
abstract class WorkBaseApi(val corpId: String?, val agentId: Int?, val suiteId: String?): WxApi() {
    init {
        val errMsg = checkValid(corpId, agentId, suiteId)
        if(errMsg != null){
            throw IllegalArgumentException(errMsg)
        }
    }

    val log = LoggerFactory.getLogger("WorkBaseApi")

    override val base = "https://qyapi.weixin.qq.com/cgi-bin"

    //若被子类重载指定了值，则试图使用该key，获取企业微信基础应用的accessToken
    open var sysAccessTokenKey: String? = null

    /**
     * ChatMsg中可能需要, 优先获取高权限key
     * */
    val secret: String?
    get() = if(Work.isIsv){
        if(Work.isMulti){
            IsvWorkMulti.ApiContextMap[suiteId]?.secret
        }else{
            IsvWorkSingle.ctx.secret
        }
    }else{
        if(Work.isMulti){
            WorkMulti.ApiContextMap[corpId]?.sysSecretMap?.get(sysAccessTokenKey)
        }else{
            WorkSingle.sysSecretMap[sysAccessTokenKey]
        }
    }



    /**
     * 检查构造api的参数是否正常
     * @return 返回错误信息，没错误返回null
     * */
    fun checkValid(corpId: String?, agentId: Int?, suiteId: String?): String?{
        var errMsg: String? = null
        if(Work.isIsv){
            if(corpId == null){
                errMsg = "no corpId for isv mode, suiteId=$suiteId"
                log.warn(errMsg)
                return errMsg
            }
            if(Work.isMulti){
                if(suiteId == null){
                    errMsg = "no suiteId or corpId, suiteId=$suiteId, corpId=$corpId"
                    log.warn(errMsg)
                    return errMsg
                }
            }
        }else{
            if(Work.isMulti){
                if(corpId == null || agentId == null){
                    errMsg = "no agentId or corpId=$corpId, agentId=$agentId in multi mode"
                    log.warn(errMsg)
                    return errMsg
                }
            }
        }
        return errMsg
    }

    override fun accessToken() = (if(Work.isIsv){
        if(Work.isMulti){
            IsvWorkMulti.ApiContextMap[suiteId]?.accessTokenMap?.get(corpId)
        }else{
            IsvWorkSingle.ctx.accessTokenMap[corpId]
        }
    }else{
        if(Work.isMulti){
            val corpCtx = WorkMulti.ApiContextMap[corpId]
            if(corpCtx == null){
                println("no corpCtx in multi mode?: corpId=$corpId")
                null
            }else{
                //存在的话使用系统级别的secret及accessToken
                sysAccessTokenKey?.let { corpCtx.sysAccessTokenMap[it] }?:
                if(agentId != null) {
                    WorkMulti.ApiContextMap[corpId]?.agentMap?.get(agentId)?.accessToken
                }else {
                    println("no agentId in multi mode? corpId=$corpId")
                    null
                }
            }
        }else{
            //存在的话使用系统级别的secret及accessToken
            sysAccessTokenKey?.let { WorkSingle.sysAccessTokenMap[it] }?:WorkSingle.agentContext.accessToken
        }
    })?.get()



    /**
     * 企业微信在回调企业指定的URL时，是通过特定的IP发送出去的。如果企业需要做防火墙配置，那么可以通过这个接口获取到所有相关的IP段。
     * IP段有变更可能，当IP段变更时，新旧IP段会同时保留一段时间。建议企业每天定时拉取IP段，更新防火墙设置，避免因IP段变更导致网络不通。
     *
     * 若调用失败，会返回errcode及errmsg（判断是否调用失败，根据errcode存在并且值非0）
     * */
    suspend fun getCallbackIp(): ResponseCallbackIps
            = doGetByUrl("$base/getcallbackip?access_token=${accessToken()}")
}


@Serializable
class QyBaseResponse(
    @SerialName("errcode")
    val errCode: Int?,
    @SerialName("errmsg")
    val errMsg: String?
){
    fun isOK() = (errCode != null && errCode == 0)
}