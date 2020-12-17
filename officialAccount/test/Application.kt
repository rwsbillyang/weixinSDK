/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 16:19
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

package com.github.rwsbillyang.wxSDK.officialAccount.test


import com.github.rwsbillyang.wxSDK.accessToken.ITimelyRefreshValue
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.ReTextMsg
import com.github.rwsbillyang.wxSDK.msg.WxBaseEvent
import com.github.rwsbillyang.wxSDK.msg.WxBaseMsg
import com.github.rwsbillyang.wxSDK.officialAccount.OfficialAccount
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.DefaultOAEventHandler
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.DefaultOAMsgHandler
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.OATextMsg
import com.github.rwsbillyang.wxSDK.officialAccount.officialAccountMsgApi

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.features.*
import io.ktor.http.*

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val AppIdForTest = "wx2ea341a3b3871d23"

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.testableModule(testing: Boolean = false) {
    class AuthenticationException : RuntimeException()
    class AuthorizationException : RuntimeException()

    routing {
        get("/") {
            call.respondText("OK from wxSDK", contentType = ContentType.Text.Plain)
        }

        install(StatusPages) {
            exception<AuthenticationException> { cause ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> { cause ->
                call.respond(HttpStatusCode.Forbidden)
            }

        }
    }
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.apiTest(testing: Boolean = false){
    testableModule(testing)
    OfficialAccount.config {
        appId = AppIdForTest
        secret = "89d147ef8e83c4cd097e96992127f0bc"
        encodingAESKey = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG"
        token = "com.github.rwsbillyang.wxSDK.test.testToken"
        wechatId = "gh_b2f9163a8000"
        wechatName = "测试号"
        msgHandler = TestOAMsgHandler()
        eventHandler = OAEventTestHandler()

        accessToken = TestAccessTokenValue()
        ticket = TestJsTicketValue()
    }
    routing {
        officialAccountMsgApi()
    }
}




class TestAccessTokenValue: ITimelyRefreshValue {
    override fun get(): String {
        return "testAccessToken-${System.currentTimeMillis()}"
    }
}
class TestJsTicketValue: ITimelyRefreshValue {
    override fun get(): String {
        return "testTicket-${System.currentTimeMillis()}"
    }
}

class TestOAMsgHandler: DefaultOAMsgHandler()
{
    override fun onOATextMsg(msg: OATextMsg): ReBaseMSg?{
        return ReTextMsg("TestOAMsgHandler reply the msg: ${msg.content},msgId=${msg.msgId}", msg.base.fromUserName, msg.base.toUserName)
    }

    override fun onDefault(msg: WxBaseMsg): ReBaseMSg? {
        return ReTextMsg("TestOAMsgHandler default reply the msg: msgId=${msg.msgId}", msg.base.fromUserName, msg.base.toUserName)
    }
}
class OAEventTestHandler: DefaultOAEventHandler(){
    override fun onDefault(e: WxBaseEvent): ReBaseMSg? {
        println("got event: ${e.event}")
        return null

    }
}


