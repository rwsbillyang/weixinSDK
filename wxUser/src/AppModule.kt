package com.github.rwsbillyang.wxUser



import com.github.rwsbillyang.ktorKit.server.AppModule
import com.github.rwsbillyang.ktorKit.server.WsSessions
import com.github.rwsbillyang.wxUser.account.AccountControllerWebAdmin
import com.github.rwsbillyang.wxUser.account.AccountService
import com.github.rwsbillyang.wxUser.account.stats.StatsService
import com.github.rwsbillyang.wxUser.account.webUserAdminApi

import com.github.rwsbillyang.wxUser.feedback.FeedbackController
import com.github.rwsbillyang.wxUser.feedback.FeedbackService

import com.github.rwsbillyang.wxUser.feedback.feedbackApi

import org.koin.dsl.module



/**
 * 1.系统级账号，web登录，其它第三方登录可绑定关联成一个统一的账号
 * 2. feedback
 * 3. 登录统计功能
 * */
val wxUserAppModule = AppModule(
    listOf(
        module {
            single { AccountControllerWebAdmin(get(), get()) }
            single { AccountService(get()) }
            single { WsSessions() }
            single { StatsService(get()) }

            single { FeedbackController() }
            single { FeedbackService(get()) }


        }
    ), "user")
{
    webUserAdminApi()
    feedbackApi()
}


//
//
//class UserSofaRpc(application: Application): SofaRpc(application){
//    private val accountRpcServer: AccountRpcServer by inject()
//
//    override fun publicRpcService() {
//        ProviderConfig<IUser>()
//            .setInterfaceId(IUser::class.java.name)
//            .setRef(accountRpcServer)
//            .setServer(serverConfig)
//            .setRegistry(registryConfig)
//            .export()
//    }
//
//    override fun injectRpcClient() {
//        val config1 = getConsumerConfig<IFan>() //登录后 antd admin需要查询用户的profile，否则需要重新登录
//        val config2 = getConsumerConfig<IWechatNotifier>() //推荐奖励作为续费成功通知
//        //https://start.insert-koin.io/#/getting-started/modules-definitions?id=module-amp-definitions
//        if(config1 != null  || config2 != null){
//            loadKoinModules(module {
//                if(config1 != null)single<IFan> { config1.refer() } //注册client
//                if(config2 != null)single<IWechatNotifier> { config2.refer() }
//            })
//        }
//    }
//}