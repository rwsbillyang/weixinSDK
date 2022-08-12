package com.github.rwsbillyang.wxUser


import com.github.rwsbillyang.ktorKit.*
import com.github.rwsbillyang.wxSDK.wxPay.wxPayNotify

import com.github.rwsbillyang.wxUser.feedback.FeedbackController
import com.github.rwsbillyang.wxUser.feedback.FeedbackService

import com.github.rwsbillyang.wxUser.feedback.feedbackApi
import com.github.rwsbillyang.wxUser.order.AccountOrderController
import com.github.rwsbillyang.wxUser.order.AccountOrderService
import com.github.rwsbillyang.wxUser.order.accountOrderApi
import com.github.rwsbillyang.wxUser.payConfig.PayConfigService
import com.github.rwsbillyang.wxUser.payConfig.WxPayConfigInstallation
import com.github.rwsbillyang.wxUser.product.ProductService
import com.github.rwsbillyang.wxUser.product.productApi
import io.ktor.server.application.*


import org.koin.dsl.module
import org.koin.ktor.ext.inject

val ApplicationCall.userId
    get() = this.request.headers["X-Auth-UserId"]

val ApplicationCall.externalUserId
    get() = this.request.headers["X-Auth-ExternalUserId"]

val ApplicationCall.suiteId
    get() = this.request.headers["X-Auth-SuiteId"]

val ApplicationCall.agentId
    get() = this.request.headers["X-Auth-AgentId"]?.toInt()



/**
 * 包含了订单和产品，对于企业微信建议配置到agent自己的数据库中;
 * 用户反馈功能，建议配置到agent自己的数据库中;
 * 支付配置，可配置agent自己的库中，也可共享的wxWork中（暂不支持）
 * */
val wxUserAppModule = AppModule(
    listOf(
        module {
            single { AccountOrderService(get()) }
            single { AccountOrderController() }
            single { ProductService(get()) }

            single { FeedbackController() }
            single { FeedbackService(get()) }

            single { PayConfigService() }
        }, module(createdAtStart = true) {
            single { WxPayConfigInstallation(get()) }
        },
    ), "user")
{
    accountOrderApi()
    //"/api/sale/wx/payNotify/{appId}" //default  /api/sale/wx/payNotify/
    wxPayNotify { appId, payNotifyBean, orderPayDetail, errType ->
        val orderController: AccountOrderController by inject()
        orderController.onWxPayNotify(appId, payNotifyBean, orderPayDetail, errType)
    }
    productApi()
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