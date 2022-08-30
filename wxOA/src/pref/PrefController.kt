/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-11 16:58
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.wxOA.pref

import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.server.BizException
import com.github.rwsbillyang.ktorKit.server.LifeCycle
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.wxOA.EventHandler
import com.github.rwsbillyang.wxOA.MsgHandler
import com.github.rwsbillyang.wxOA.msg.MsgService
import com.github.rwsbillyang.wxSDK.bean.Menu
import com.github.rwsbillyang.wxSDK.bean.MenuType
import com.github.rwsbillyang.wxSDK.msg.InEventType
import com.github.rwsbillyang.wxSDK.msg.MsgType
import com.github.rwsbillyang.wxSDK.officialAccount.MenuApi
import com.github.rwsbillyang.wxSDK.officialAccount.OfficialAccount
import io.ktor.server.application.*
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.ktor.ext.inject
import org.litote.kmongo.eq
import org.slf4j.LoggerFactory


enum class InEventEnum(val value: String){
    SUBSCRIBE( InEventType.SUBSCRIBE),
    UNSUBSCRIBE(InEventType.UNSUBSCRIBE),
    SCAN( InEventType.SCAN),
    LOCATION(InEventType.LOCATION),
    CLICK( InEventType.CLICK),
    VIEW( InEventType.VIEW),
    SCAN_CODE_PUSH( InEventType.SCAN_CODE_PUSH),
    SCAN_CODE_WAIT_MSG(InEventType.SCAN_CODE_WAIT_MSG),
    PIC_SYS_PHOTO(InEventType.PIC_SYS_PHOTO),
    PIC_PHOTO_OR_ALBUM(InEventType.PIC_PHOTO_OR_ALBUM),
    PIC_WEIXIN(InEventType.PIC_WEIXIN),
    LOCATION_SELECT(InEventType.LOCATION_SELECT),
    VIEW_MINI_PROGRAM(InEventType.VIEW_MINI_PROGRAM),
    //MASS_SEND_JOB_FINISH( InEventType.MASS_SEND_JOB_FINISH),
    //TEMPLATE_SEND_JOB_FINISH(InEventType.TEMPLATE_SEND_JOB_FINISH),
    DEFAULT(InEventType.DEFAULT)//官方API中无此类型，为了方便对消息默认处理而添加
}

enum class InMsgEnum(val value: String){
    TEXT(MsgType.TEXT),
    IMAGE ( MsgType.IMAGE),
    VOICE (MsgType.VOICE),
    VIDEO(MsgType.VIDEO),
    SHORT_VIDEO(MsgType.SHORT_VIDEO),
    LOCATION (MsgType.LOCATION),
    LINK ( MsgType.LINK),
    //EVENT ( MsgType.EVENT),
    //MUSIC( MsgType.MUSIC),
    //NEWS (MsgType.NEWS),
    //TRANSFER_TO_CUSTOMER_SERVICE(MsgType.TRANSFER_TO_CUSTOMER_SERVICE),
    DEFAULT(MsgType.DEFAULT)//官方API中无此类型，为了方便对消息默认处理而添加
}

//fun defaultAppId() = PrefController.defaultAppId?: error("not config official account?")

class PrefController(application: Application) : LifeCycle(application), KoinComponent {
    private val log = LoggerFactory.getLogger("PrefController")
    private val service: PrefService by inject()
    private val msgService: MsgService by inject()


    init {
        onStarted {
            log.info("onStarted...")

            val prefService: PrefService by application.inject()
            val myMsgHandler: MsgHandler by application.inject()
            val myEventHandler: EventHandler by application.inject()
            /**
             * 从数据库中装载
             *
             * 本可以支持多个公众号，但变得复杂：比Msg和Event需从微信号转换为appId，
             * 前端的管理后台需要管理一个公众号列表，然后所有设置需要放到某个appId下面
             * 各个model中需要区分是哪个appId的...
             *
             * 为了快速切换公众号，可以在数据库中配置多个公众号，但只有一个是激活的，即PrefOfficialAccount中加了一个enable字段
             *
             * 如果设置了多个公众号，那么查询列表时的最后一个被设置为当前公众号的appId
             *
             * */
            prefService.findOfficialAccounts().forEach {

                //SPA单页应用，目的在于ios中微信wx.config验证时只使用#之前的字符进行验证,否则ios总是验证失败, 数据库中配置
                if(it.oauthWebUrl != null) OfficialAccount.oauthNotifyWebAppUrl = it.oauthWebUrl
                //OfficialAccount.oauthNotifyWebAppUrl = "/#!" + OfficialAccount.oauthNotifyWebAppUrl

                OfficialAccount.config{
                    appId = it._id
                    secret = it.secret
                    encodingAESKey = it.aesKey
                    token = it.token
                    wechatId = it.wechatId
                    wechatName = it.name

                    msgHandler = myMsgHandler
                    eventHandler = myEventHandler

                    //accessToken = TestAccessTokenValue()
                    //ticket = TestJsTicketValue()
                }

                log.info("config done: appId=${it._id}")
            }

        }
    }
    /**
     * 管理后台修改后的保存
     * */
    fun saveOfficialAccount(oa: PrefOfficialAccount): DataBox<PrefOfficialAccount> {
        service.saveOfficialAccount(oa)

        val myMsgHandler: MsgHandler by inject()
        val myEventHandler: EventHandler by inject()
        //update config
        OfficialAccount.config {
            appId = oa._id
            secret = oa.secret
            token = oa.token
            encodingAESKey = oa.aesKey
            wechatId = oa.wechatId
            wechatName = oa.name
            msgHandler = myMsgHandler
            eventHandler = myEventHandler
        }
        log.info("update config done: appId=${oa._id}")


        return DataBox.ok(oa)
    }
    fun findAllOfficialAccount() = DataBox.ok(service.findOfficialAccounts().map {
        OfficialAccountBean(it._id, it.name, it.wechatId,it.enable, it.host, it.oauthWebUrl)
    })

    fun findOfficialAccount(appId: String?): DataBox<PrefOfficialAccount> {
        if(appId == null) return DataBox.ko("no appId")
        return service.findOfficialAccount(appId)?.let { DataBox.ok(it) } ?: DataBox.ko("尚未配置")
    }
    fun delOfficialAccount(appId: String?): DataBox<Long> {
        if(appId == null) return DataBox.ko("no appId")
        return DataBox.ok(service.delOfficialAccount(appId).deletedCount)
    }


    fun findPrefReMsgList(appId: String?,cat: Int?): DataBox<List<PrefReMsgBean>> {
        if(cat == null ||appId == null) throw BizException("invalid parameter")

        val typeList = when(cat) {
            PrefReInMsg.CAT_EVENT -> InEventEnum.values().map { it.value }
            PrefReInMsg.CAT_MSG -> InMsgEnum.values().map { it.value }
            else -> throw BizException("invalid parameter")
        }

        //val count = typeList.size
        val map = mutableMapOf<String, PrefReMsgBean>()
        service.findPrefReMsgList(appId, cat)
                .map { map[it.type] = PrefReMsgBean(it.appId,it.type,it.msgId,it.cat, it._id, msgService.findMyMsg(it.msgId)) }

        val list = typeList.map { map[it]?: PrefReMsgBean(appId,it, null, cat, null, null) }

        return DataBox.ok(list)
    }

    fun findPrefReMsgList(param: PrefReMsgListParams): DataBox<List<PrefReMsgBean>> {
        val filter = param.toFilter()
        //val count = service.countPrefReMsgList(filter)
        val list = service.findPrefReMsgList(param).map {
            PrefReMsgBean(it.appId,it.type,it.msgId,it.cat, it._id, msgService.findMyMsg(it.msgId))
        }
        return DataBox.ok(list)
    }

    fun savePrefReInMsg(param: PrefReInMsg): DataBox<PrefReMsgBean> {
        //val theAppId = param.appId?:defaultAppId()
        if(param.appId == null) return DataBox.ko("invalid paramter: no appId")
       service.savePrefReInMsg(param)
        return DataBox.ok(param.let { PrefReMsgBean(it.appId,it.type,it.msgId,it.cat, it._id, msgService.findMyMsg(it.msgId) )})
    }

    fun delPrefReInMsg(id: String?): DataBox<Int> {
        if(id == null) return DataBox.ko("no id")
        return DataBox.ok(service.delPrefReInMsg(id).deletedCount.toInt())
    }


    fun findPrefMenu(appId: String?): DataBox<List<PrefMenuTree>> {
        if(appId == null) return DataBox.ko("no appId")
        val list = service.findPrefMenus(PrefMenu::appId eq appId)
        return if(list.isNotEmpty()){
            val list2 = list.filter { it.pId != null }
            val list1 = list.filter { it.pId == null }.map {
                val parent = it
                PrefMenuTree(it.name,it._id,it.appId,it.type,it.key,it.url,it.mediaId,
                        it.pagePath,it.miniId, it.pId,
                        list2.filter { it.pId == parent._id }.map {
                            PrefMenuTree(it.name,it._id,it.appId,it.type,it.key,it.url,it.mediaId,
                                    it.pagePath,it.miniId, it.pId ) }
                )
            }
            DataBox.ok(list1)
        }else  DataBox.ok(emptyList())
    }

    fun savePrefMenu(doc: PrefMenu): DataBox<PrefMenu> {
        if(doc.appId == null) return DataBox.ko("invalid paramter: no appId")
        service.savePrefMenu(doc)
        return DataBox.ok(doc)
    }

    fun delPrefMenu(id: String?): DataBox<Int> {
        if(id == null) return DataBox.ko("no id")
        return DataBox.ok(service.delPrefMenu(id.toObjectId()).deletedCount.toInt())
    }

    //从所有子菜单中找到自己的子菜单
    private fun getSubs(subs: List<PrefMenu>, parentId: ObjectId, host: String): List<Menu>? {
        if(subs.isEmpty()) return null
        val list = subs.filter { it.pId == parentId }.map {
            Menu(it.name,it.type, it.key, if(it.url?.startsWith("http") != false) it.url else host+it.url, it.mediaId,
                it.pagePath,it.miniId ) }
        return if(list.isEmpty())  null else list
    }
    fun executeMenuCmd(cmd: String?, appId: String?): DataBox<String> {
        if(appId == null) return DataBox.ko("invalid paramter: no appId")
        if (cmd.isNullOrBlank()) return DataBox.ko("invalid parameter")
        when (cmd) {
            "create" -> {
                val list = service.findPrefMenuByAppId(appId)
                if (list.isEmpty()) {
                    return DataBox.ko("no menu, please create it firstly")
                }
                val host = service.findOfficialAccount(appId)?.host?:""
                val subs = list.filter { it.pId != null }//所有子菜单
                val menus = list.filter { it.pId == null }.map {
                    //必须是parent类型，因为前端编辑时，过去是parent且有sub menu，但后来改为了非parent，这时应作为非parent对待
                    val sub = if(it.type == MenuType.Parent) getSubs(subs, it._id, host) else null
                    Menu(it.name,if(sub!=null) null else it.type, it.key, if(it.url?.startsWith("http") != false) it.url else host+it.url, it.mediaId, it.pagePath,it.miniId,sub)
                }

                val res = MenuApi(appId).create(menus)
                return if (res.isOK()) DataBox.ok(null) else DataBox.ko("${res.errCode}: ${res.errMsg}")
            }
            "del" -> {
                val res = MenuApi(appId).delete()
                return if (res.isOK()) DataBox.ok(null) else DataBox.ko("${res.errCode}: ${res.errMsg}")
            }
            "get" -> {
                val res = MenuApi(appId).detail()
                return if (res.isOK()) DataBox.ok(null) else DataBox.ko("${res.errCode}: ${res.errMsg}")
            }
            else -> {
                log.warn("not support: $cmd")
                return DataBox.ko("not support: $cmd")
            }
        }
    }

}
