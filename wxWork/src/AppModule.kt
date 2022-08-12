package com.github.rwsbillyang.wxWork


import com.github.rwsbillyang.ktorKit.AppModule
import com.github.rwsbillyang.ktorKit.installModule
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.IWorkMsgHandler
import com.github.rwsbillyang.wxUser.account.AccountServiceBase
import com.github.rwsbillyang.wxUser.account.stats.StatsService
import com.github.rwsbillyang.wxUser.fakeRpc.IPayWechatNotifier
import com.github.rwsbillyang.wxWork.account.*
import com.github.rwsbillyang.wxWork.agent.agentApi
import com.github.rwsbillyang.wxWork.agent.agentModule
import com.github.rwsbillyang.wxWork.config.ConfigController
import com.github.rwsbillyang.wxWork.config.ConfigService
import com.github.rwsbillyang.wxWork.config.wxWorkConfigApi
import com.github.rwsbillyang.wxWork.contacts.contactApi
import com.github.rwsbillyang.wxWork.contacts.contactModule
import com.github.rwsbillyang.wxWork.fakeRpc.FanRpcWork
import com.github.rwsbillyang.wxWork.isv.IsvCorpService
import com.github.rwsbillyang.wxWork.isv.OnStartConfigWxWork3rd
import com.github.rwsbillyang.wxWork.isv.UserDetail3rd
import com.github.rwsbillyang.wxWork.isv.isvModule
import com.github.rwsbillyang.wxWork.msg.PayMsgNotifier
import io.ktor.server.application.*
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject


/**
 * 企业微信基础接入模块: 企业联系人、agent应用等基础模块,接入企业微信，支持收发消息和事件、oauth认证登录,js签名等功能
 *
 * 配置信息可以动态修改更新，但route api不支持动态卸载
 *
 * */
val wxWorkModule = AppModule(
    listOf(contactModule, agentModule,
        module {
            single<IWorkEventHandler> { WxWorkEventHandler() }
            single<IWorkMsgHandler> { WxWorkMsgHandler() }
        }),
    if (Work.isIsv) "WxWorkIsv" else "WxWork"
) {
    agentApi()
    contactApi()
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
    }
}

/**
 * 企业微信应用配置模块: 配置数据需要在应用自己的库中，否则启动时加载了别的agent的配置
 *
 * 在启动时，会使用OnStartConfigWxWork配置数据库中所有enabled的agent配置，
 * 而agent信息不存在时，则会同步agent信息，并同步其可见联系人详情，以及可见联系人的外部联系详情
 *
 * 单独拆出来，目的在于各应用可使用自己的数据库，避免加载其它应用的配置
 * */
val wxWorkConfigModule = AppModule(
    if (Work.isIsv) {
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
                single { OnStartConfigWxWork(get()) }
                //single { WxWorkSofaRpc(get()) }
            })
    },
    if (Work.isIsv) "WxWorkIsv" else "WxWork"
) {
    if (!Work.isIsv) {
        wxWorkConfigApi()
    }
}

/**
 * 企业微信应用账号模块： 包括账号注册、登录、绑定uId、查询账号level等，更新账户ext、账户支付通知、推荐奖励、查询用户基本信息等功能
 *
 * 注意：需要配置好WechatNotifier中的跳转链接
 * 各企业微信应用agent可共享account，也可各有各的数据库
 *
 *  为什么不采用上面的方式，因account AppModule在此处定义，而service所使用的dbname却在依赖库wxUser中使用
 *  不便于重新指定
 * */
fun Application.installWxWorkAccountAppModule(
    dbName: String = if (Work.isIsv) "WxWorkIsv" else "WxWork",
) {
    AccountServiceBase.AccountDbName = dbName
    val list = listOf(
        module(createdAtStart = true) {
            single { ExpireNotifierWork(get()) }
        },
        module {
            single { FanRpcWork() }
            single { PayMsgNotifier() } bind IPayWechatNotifier::class //OrderController中支付成功后需要通知
            single { StatsService(get()) }
            single { RecommendHelper() }
            single { AccountControllerWork(get()) }
            //RawDataController中点击通知需要查询有效期从而是否展示访客名字, statsWorker中也类似, OrderController中支付成功后需要更新账户信息
            single { AccountServiceWxWork(get()) } bind AccountServiceBase::class
        })

    val appModule = AppModule(list, dbName) {
        workAccountApi()
    }

    installModule(appModule)
}






