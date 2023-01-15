package com.github.rwsbillyang.wxOA



import com.github.rwsbillyang.ktorKit.server.AppModule
import com.github.rwsbillyang.wxOA.account.RecommendHelper
import com.github.rwsbillyang.wxOA.account.WxOaAccountController
import com.github.rwsbillyang.wxOA.account.WxOaAccountService
import com.github.rwsbillyang.wxOA.account.oaUserApi
import com.github.rwsbillyang.wxOA.account.order.AccountOrderController
import com.github.rwsbillyang.wxOA.account.order.AccountOrderService
import com.github.rwsbillyang.wxOA.account.order.accountOrderApi
import com.github.rwsbillyang.wxOA.account.payConfig.PayConfigService
import com.github.rwsbillyang.wxOA.account.payConfig.WxPayConfigInstallation
import com.github.rwsbillyang.wxOA.account.product.ProductService
import com.github.rwsbillyang.wxOA.account.product.productApi
import com.github.rwsbillyang.wxOA.fan.FanService
import com.github.rwsbillyang.wxOA.fan.fanApi
import com.github.rwsbillyang.wxOA.fan.fanModule
import com.github.rwsbillyang.wxOA.media.mediaApi
import com.github.rwsbillyang.wxOA.media.mediaModule
import com.github.rwsbillyang.wxOA.msg.TemplatePayMsgNotifier
import com.github.rwsbillyang.wxOA.msg.msgApi
import com.github.rwsbillyang.wxOA.msg.msgModule
import com.github.rwsbillyang.wxOA.pref.PrefController
import com.github.rwsbillyang.wxOA.pref.prefApi
import com.github.rwsbillyang.wxOA.pref.prefModule
import com.github.rwsbillyang.wxOA.qrcodeChannel.qrCodeChannelModule
import com.github.rwsbillyang.wxOA.qrcodeChannel.qrcodeChannelApi
import com.github.rwsbillyang.wxOA.stats.statsModule
import com.github.rwsbillyang.wxSDK.wxPay.wxPayNotify
import org.koin.dsl.module
import org.koin.ktor.ext.inject


/**
 * 公众号基础接入模块，包括消息接入、oauth认证、公众号管理配置、关注二维码生成、粉丝（包括列表查看）、公众号进入点击统计等功能
 * */
val wxOaAppModule = AppModule(
    listOf(
        module(createdAtStart = true) {
            single { PrefController(get()) }
            single { ReMsgChooser() }
            single { EventHandler() } //需要时handler时自动注入
            single { MsgHandler() }//需要时handler时自动注入

            single { WxPayConfigInstallation(get()) }
            // single { WxOASofaRpc(get()) }
            // single { ExpireNotifierOA(get()) }
        },
        module {
            single { AccountOrderService(get()) }
            single { AccountOrderController() }
            single { ProductService(get()) }
            single { PayConfigService() }

            single { RecommendHelper() }

            single { WxOaAccountController(get()) }
            single { TemplatePayMsgNotifier() }
            single { WxOaAccountService(get()) }
        },
        fanModule,
        statsModule,
        msgModule,
        prefModule,
        mediaModule,
        qrCodeChannelModule
    ),
    "weixin"
) {
    oaUserApi()
    dispatchMsgApi()//  "/api/wx/oa/app/{appId}"

    accountOrderApi()
    //"/api/sale/wx/payNotify/{appId}" //default  /api/sale/wx/payNotify/
    wxPayNotify { appId, payNotifyBean, orderPayDetail, errType ->
        val orderController: AccountOrderController by inject()
        orderController.onWxPayNotify(appId, payNotifyBean, orderPayDetail, errType)
    }
    productApi()

    oAuthApi( needUserInfoSettingsBlock = {owner, openId ->
        val fanService: FanService by inject()

        fanService.findGuest(openId) == null && fanService.findFan(openId) == null
    }
      /* , onGetOauthAccessToken = { res, appId ->
        val fanService: FanService by inject()
        res.toOauthToken(appId)?.let { fanService.saveOauthToken(it) }
    },
        onGetUserInfo =  { res, appId ->
            val fanService: FanService by inject()
            res.toGuest(appId)?.let { fanService.saveGuest(it) }
        }*/
    )

    
    jsSdkSignature()

    fanApi()
    msgApi()
    prefApi()
    mediaApi()
    qrcodeChannelApi()
}






//
//class WxOASofaRpc(application: Application) : SofaRpc(application) {
//    private val fanRpcServer: FanRpc by inject()
//    private val templateMsgNotifier: TemplateMsgNotifier by inject()
//
//    override fun publicRpcService() {
//        ProviderConfig<IFan>()
//            .setInterfaceId(IFan::class.java.name)
//            .setRef(fanRpcServer)
//            .setServer(serverConfig)
//            .setRegistry(registryConfig)
//            .export()
//
//        ProviderConfig<IWechatNotifier>()
//            .setInterfaceId(IWechatNotifier::class.java.name)
//            .setRef(templateMsgNotifier)
//            .setServer(serverConfig)
//            .setRegistry(registryConfig)
//            .export()
//    }
//
//    override fun injectRpcClient() {
//        val config1 = getConsumerConfig<ISolo>() //查询是否需要获取用户信息设置
//        val config2 = getConsumerConfig<IUser>() //FanRpc.getFanInfoByUId需要用uId换取openId，然后再查fanInfo
//
//        if(config1 != null || config2 != null){
//            //https://start.insert-koin.io/#/getting-started/modules-definitions?id=module-amp-definitions
//            loadKoinModules(module {
//                if(config1 != null) single<ISolo> { config1.refer() }
//                if(config2 != null) single<IUser> { config2.refer() }
//            })
//        }
//    }
//}
//

