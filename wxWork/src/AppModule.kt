package com.github.rwsbillyang.wxWork


import com.github.rwsbillyang.ktorKit.server.AppModule
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkMsgHandler
import com.github.rwsbillyang.wxUser.account.stats.StatsService
import com.github.rwsbillyang.wxWork.account.RecommendHelper
import com.github.rwsbillyang.wxWork.account.WxWorkAccountController
import com.github.rwsbillyang.wxWork.account.WxWorkAccountService
import com.github.rwsbillyang.wxWork.account.workAccountApi
import com.github.rwsbillyang.wxWork.agent.agentApi
import com.github.rwsbillyang.wxWork.agent.agentModule
import com.github.rwsbillyang.wxWork.channel.ChannelController
import com.github.rwsbillyang.wxWork.channel.ChannelService
import com.github.rwsbillyang.wxWork.channel.channelApi
import com.github.rwsbillyang.wxWork.chatViewer.*
import com.github.rwsbillyang.wxWork.config.ConfigController
import com.github.rwsbillyang.wxWork.config.ConfigService
import com.github.rwsbillyang.wxWork.config.wxWorkConfigApi
import com.github.rwsbillyang.wxWork.contacts.contactApi
import com.github.rwsbillyang.wxWork.contacts.contactModule
import com.github.rwsbillyang.wxWork.isv.IsvCorpService
import com.github.rwsbillyang.wxWork.isv.OnStartConfigWxWork3rd
import com.github.rwsbillyang.wxWork.isv.UserDetail3rd
import com.github.rwsbillyang.wxWork.isv.isvModule
import com.github.rwsbillyang.wxWork.msg.PayMsgNotifier
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject


val ApplicationCall.agentId
    get() = this.request.headers["X-Auth-AgentId"]?.toInt()

val ApplicationCall.userId
    get() = this.request.headers["X-Auth-UserId"]

val ApplicationCall.externalUserId
    get() = this.request.headers["X-Auth-ExternalUserId"]


/**
 * 企业微信基础接入模块: 企业联系人、agent应用等基础模块,接入企业微信，支持收发消息和事件、oauth认证登录,js签名等功能
 *
 * 警告：若有多个APP，且共享同一个数据库，将会导致每个app都加载所有agent，并执行初始化，导致重复执行初始化并获取腾讯token，
 * 老的token会失效，因此会出问题，因而不可有多个app共享一个库，应该各自使用自己的库
 *
 * 在启动时，会使用OnStartConfigWxWork配置数据库中所有enabled的agent配置，
 * 而agent信息不存在时，则会同步agent信息，并同步其可见联系人详情，以及可见联系人的外部联系详情
 *
 * 配置信息可以动态修改更新，但route api不支持动态卸载
 * */
val wxWorkModule = AppModule(
    listOf(contactModule, agentModule,
        module {
            single<IWorkEventHandler> { WxWorkEventHandler() }
            single<IWorkMsgHandler> { WxWorkMsgHandler() }
        },
        //        module(createdAtStart = true) {
//            single { ExpireNotifierWork(get()) }
//        },
        module {
            //single { FanRpcWork() }
            single { PayMsgNotifier() }
            single { StatsService(get()) }
            single { RecommendHelper() }
            single { WxWorkAccountController(get()) }
            single { WxWorkAccountService(get()) }
            single { ChannelController() }
            single { ChannelService(get()) }
        }
    ) + if (Work.isIsv) {
        listOf(isvModule, module(createdAtStart = true) {
            single { OnStartConfigWxWork3rd(get()) }
            //single { WxWorkSofaRpc(get()) }
        })
    } else {
        listOf(
            module {
                single { ConfigController() }
                single { ConfigService(get()) }
            },
            module(createdAtStart = true) {
                single { OnStartConfigWxWork(get()) } //将加载当前库中的所有agent，若部署多个应用，需要放到不同的库中
                //single { WxWorkSofaRpc(get()) }
            })
    },
    if (Work.isIsv) "WxWorkIsv" else "WxWork"
) {
    workAccountApi()
    agentApi()
    contactApi()
    channelApi()
    workJsSdkSignature()

    wxWorkOAuthApi {
        val isvCorpService: IsvCorpService by inject()
        if (it.isOK()) {
            isvCorpService.saveUserDetail3rd(
                UserDetail3rd(
                    it.corpId,
                    it.userId,
                    it.name,
                    it.gender,
                    it.avatar,
                    it.qrCode
                )
            )
        }
    }

    if (Work.isIsv) {
        isvDispatchMsgApi()
    } else {
        dispatchAgentMsgApi()
        wxWorkConfigApi()
    }
}



//将企业微信会话存档so放置在系统路径：LD_LIBRARY_PATH指定的路径内，或者将so所在的目录加入到LD_LIBRARY_PATH的路径范围内。
val chatViewerAppModule = AppModule(
    listOf(
        module(createdAtStart = true) {
            single { ChatMsgController() }
            single { ChatMsgService(get()) }
            single(createdAtStart = true) { ChatMsgScheduleTask() }
            single { ChatMsgFetcher() }
            single { OnShutDown(get()) }
        }
    ),
    "chatViewer"
) {
    chatViewerApi()
}




