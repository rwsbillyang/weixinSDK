/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 11:40
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

package com.github.rwsbillyang.wxWork.test

import com.github.rwsbillyang.wxSDK.accessToken.ITimelyRefreshValue
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.ReTextMsg
import com.github.rwsbillyang.wxSDK.work.*
import com.github.rwsbillyang.wxSDK.work.inMsg.DefaultWorkEventHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.DefaultWorkMsgHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.WorkBaseMsg
import com.github.rwsbillyang.wxSDK.work.inMsg.WorkTextMsg
import com.github.rwsbillyang.wxWork.dispatchAgentMsgApi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object TestConstatns {
    val KeyContact = SysAgentKey.Contact.name
    val KeyCustomer = SysAgentKey.ExternalContact.name
    val KeyChatArchive = SysAgentKey.ChatArchive.name
    val KeyWxkf = SysAgentKey.WxKeFu.name


    const val CorpId = "wx5823bf96d3bd56c7"
}



@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.testableModule(testing: Boolean = false) {
    class AuthenticationException : RuntimeException()
    class AuthorizationException : RuntimeException()

    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden)
        }
    }

    routing {
        get("/ok") {
            call.respondText("OK from wxSDK", contentType = ContentType.Text.Plain)
        }
    }
}




@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.WorkTestableModule(testing: Boolean = false) {
    testableModule(testing)

    Work.isIsv = false

    val msgHandler = TestWorkMsgHandler()
    val eventHandler = DefaultWorkEventHandler()
    WorkMulti.config(
        TestConstatns.CorpId,
        TestConstatns.KeyWxkf,
        "the_secret",
        "QDG6eK",
        "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C",
        customMsgHandler = msgHandler,
        customEventHandler = eventHandler,
        customAccessToken = TestAccessTokenValue()
    )
    WorkMulti.config(
        TestConstatns.CorpId,
        TestConstatns.KeyChatArchive,
        "the_secret",
        "QDG6eK",
        "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C",
        customMsgHandler = msgHandler,
        customEventHandler = eventHandler,
        customAccessToken = TestAccessTokenValue()
    )
    WorkMulti.config(
        TestConstatns.CorpId,
        TestConstatns.KeyContact,
        "the_secret",
        "QDG6eK",
        "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C",
        customMsgHandler = msgHandler,
        customEventHandler = eventHandler,
        customAccessToken = TestAccessTokenValue()
    )
    WorkMulti.config(
        TestConstatns.CorpId,
        TestConstatns.KeyCustomer,
        "the_secret",
        "QDG6eK",
        "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C",
        customMsgHandler = msgHandler,
        customEventHandler = eventHandler,
        customAccessToken = TestAccessTokenValue()
    )

    routing {
        dispatchAgentMsgApi()
    }
}

class TestAccessTokenValue : ITimelyRefreshValue {
    override fun get(): String {
        return "testAccessToken-${System.currentTimeMillis()}"
    }
}

class TestJsTicketValue : ITimelyRefreshValue {
    override fun get(): String {
        return "testTicket-${System.currentTimeMillis()}"
    }
}


class TestWorkMsgHandler : DefaultWorkMsgHandler() {
    override fun onTextMsg(appId: String ,agentId: String?, msg: WorkTextMsg): ReBaseMSg? {
        return ReTextMsg(
            "TestWorkMsgHandler reply the msg: content=${msg.content},msgId=${msg.msgId},agentId=${msg.agentId}",
            msg.base.fromUserName,
            msg.base.toUserName
        )
    }

    override fun onDefault(appId: String ,agentId: String?,msg: WorkBaseMsg): ReBaseMSg? {
        return ReTextMsg(
            "TestWorkMsgHandler default reply the msg: msgId=${msg.msgId},agentId=${msg.agentId}",
            msg.base.fromUserName,
            msg.base.toUserName
        )
    }
}
