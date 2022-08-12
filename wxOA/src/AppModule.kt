package com.github.rwsbillyang.wxOA



import com.github.rwsbillyang.ktorKit.AppModule
import com.github.rwsbillyang.ktorKit.installModule
import com.github.rwsbillyang.wxOA.account.*
import com.github.rwsbillyang.wxOA.fakeRpc.FanRpcOA
import com.github.rwsbillyang.wxOA.fan.*
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
import com.github.rwsbillyang.wxSDK.officialAccount.OfficialAccount
import com.github.rwsbillyang.wxUser.account.*
import com.github.rwsbillyang.wxUser.account.stats.StatsService
import com.github.rwsbillyang.wxUser.fakeRpc.IPayWechatNotifier
import io.ktor.server.application.*
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject


/**
 * 公众号基础接入模块，包括消息接入、oauth认证、公众号管理配置、关注二维码生成、粉丝（包括列表查看）、公众号进入点击统计等功能
 * */
val wxOaAppModule = AppModule(
    listOf(
        module(createdAtStart = true) {
            single { PrefController(get()) }
            single { EventHandler() } //需要时handler时自动注入
            single { MsgHandler() }//需要时handler时自动注入
           // single { WxOASofaRpc(get()) }
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
    dispatchMsgApi()//  "/api/wx/oa/app/{appId}"

    oAuthApi( needUserInfo = {owner, openId ->
        val accountService: AccountServiceOA by inject()
        val fanService: FanService by inject()

        val needUserInfo = owner?.let { accountService.findById(it) }?.needUserInfo?: OfficialAccount.defaultGetUserInfo
        val hasInfo = fanService.findGuest(openId) != null || fanService.findFan(openId) != null
        needUserInfo && !hasInfo
    },
        onGetOauthAccessToken = { res, appId ->
        val fanService: FanService by inject()
        res.toOauthToken()?.let { fanService.saveOauthToken(it) }
    })
    { res, appId ->
        val fanService: FanService by inject()
        res.toGuest(appId)?.let { fanService.saveGuest(it) }
    }
    
    jsSdkSignature()

    fanApi()
    msgApi()
    prefApi()
    mediaApi()
    qrcodeChannelApi()
}


/**
 * 账号功能： 公众号注册、登录、查询账号的level、推荐奖励、fanInfo等信息、支付通知模板消息、
 *
 * @param dbName 数据库名称，用于将账号模块放入到业务应用自己的数据库中
 * @param standalone 单独使用公众号功能时为true，与企业微信同时使用时为false。
 * 为true时，整个运行app中，AccountControllerOA和AccountServiceOA以及其父类型，都使用此时注入的OA实例；
 * 为false时，父类实例另有其它子类实例绑定，如企业微信子类实例，此处只管自己类型的额外的实例注入。
 *
 * */
fun Application.installOaAccountAppModule(dbName: String, standalone: Boolean = true){
    AccountServiceBase.AccountDbName = dbName

    val list = listOf(
        module(createdAtStart = true) {
            single { ExpireNotifierOA(get()) }
        },
        module {
            single { RecommendHelper() }
            single { FanRpcOA() }

            single { AccountControllerOA(get()) }
            if(standalone){
                single { StatsService(get()) }
                //OrderController中支付成功后需要通知
                single { TemplatePayMsgNotifier() } bind IPayWechatNotifier::class
                //RawDataController中点击通知需要查询有效期从而是否展示访客名字, statsWorker中也类似, OrderController中支付成功后需要更新账户信息
                single { AccountServiceOA(get()) } bind AccountServiceBase::class
            }else{
                single { TemplatePayMsgNotifier() }
                single { AccountServiceOA(get()) }
            }
        })

    val appModule = AppModule(list, dbName){
        oaUserApi()
    }

    installModule(appModule)
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

