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

package com.github.rwsbillyang.wxSDK.work.test


import com.github.rwsbillyang.wxSDK.accessToken.ITimelyRefreshValue
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.msg.ReTextMsg
import com.github.rwsbillyang.wxSDK.work.*
import com.github.rwsbillyang.wxSDK.work.inMsg.DefaultWorkMsgHandler
import com.github.rwsbillyang.wxSDK.work.inMsg.WorkBaseMsg
import com.github.rwsbillyang.wxSDK.work.inMsg.WorkTextMsg

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.features.*
import io.ktor.http.*

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object TestConstatns {
    const val KeyBase = 1000
    const val KeyContact = KeyBase + 1
    const val KeyCustomer = KeyBase + 2
    const val KeyChatArchive = KeyBase + 3

    const val KeyAgentMgt = KeyBase + 4
    const val KeyCalendar = KeyBase + 5
    const val KeyInvoice = KeyBase + 6
    const val KeyMaterial = KeyBase + 7
    const val KeyOA = KeyBase + 8

    const val CorpId = "wx5823bf96d3bd56c7"
}



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
fun Application.WorkTestableModule(testing: Boolean = false) {

    testableModule(testing)

    WorkMulti.config(
        TestConstatns.CorpId,
        TestConstatns.KeyBase,
        "the_secret",
        "QDG6eK",
        "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C",
        customAccessToken = TestAccessTokenValue()
    )
    WorkMulti.config(
        TestConstatns.CorpId,
        TestConstatns.KeyChatArchive,
        "the_secret",
        "QDG6eK",
        "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C",
        customAccessToken = TestAccessTokenValue()
    )
    WorkMulti.config(
        TestConstatns.CorpId,
        TestConstatns.KeyContact,
        "the_secret",
        "QDG6eK",
        "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C",
        customAccessToken = TestAccessTokenValue()
    )
    WorkMulti.config(
        TestConstatns.CorpId,
        TestConstatns.KeyCustomer,
        "the_secret",
        "QDG6eK",
        "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C",
        customAccessToken = TestAccessTokenValue()
    )

    routing {
        dispatchAgentMsgApi(TestConstatns.CorpId, TestConstatns.KeyBase)
        dispatchAgentMsgApi(TestConstatns.CorpId, TestConstatns.KeyChatArchive)
        dispatchAgentMsgApi(TestConstatns.CorpId, TestConstatns.KeyContact)
        dispatchAgentMsgApi(TestConstatns.CorpId, TestConstatns.KeyCustomer)
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
    override fun onTextMsg(msg: WorkTextMsg): ReBaseMSg? {
        return ReTextMsg(
            "TestWorkMsgHandler reply the msg: content=${msg.content},msgId=${msg.msgId},agentId=${msg.agentId}",
            msg.base.fromUserName,
            msg.base.toUserName
        )
    }

    override fun onDefault(msg: WorkBaseMsg): ReBaseMSg? {
        return ReTextMsg(
            "TestWorkMsgHandler default reply the msg: msgId=${msg.msgId},agentId=${msg.agentId}",
            msg.base.fromUserName,
            msg.base.toUserName
        )
    }
}
