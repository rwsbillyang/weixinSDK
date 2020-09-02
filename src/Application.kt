package com.github.rwsbillyang.wxSDK


import com.github.rwsbillyang.wxSDK.officialAccount.OfficialAccountFeature
import com.github.rwsbillyang.wxSDK.officialAccount.officialAccountApi
import io.ktor.application.*
import io.ktor.routing.*


fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(OfficialAccountFeature) {
        appId = "your_app_id"
        secret = "your_app_secret_key"
        encodingAESKey = "your_encodingAESKey or null "
        token = "your_token"
    }


    routing {
        officialAccountApi()
        //weixinApi("/wx")
    }
}


