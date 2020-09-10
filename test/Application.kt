package com.github.rwsbillyang.wxSDK.test

import com.github.rwsbillyang.wxSDK.OfficialAccountFeature
import com.github.rwsbillyang.wxSDK.WorkFeature
import com.github.rwsbillyang.wxSDK.common.accessToken.ITimelyRefreshValue
import com.github.rwsbillyang.wxSDK.common.apiJson
import com.github.rwsbillyang.wxSDK.common.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.common.msg.ReTextMsg
import com.github.rwsbillyang.wxSDK.common.msg.WxBaseMsg
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.DefaultOAMsgHandler
import com.github.rwsbillyang.wxSDK.officialAccount.inMsg.OATextMsg
import com.github.rwsbillyang.wxSDK.officialAccountApi
import com.github.rwsbillyang.wxSDK.work.msg.DefaultWorkMsgHandler
import com.github.rwsbillyang.wxSDK.work.msg.WorkBaseMsg
import com.github.rwsbillyang.wxSDK.work.msg.WorkTextMsg
import com.github.rwsbillyang.wxSDK.workApi
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.serialization.*
import org.slf4j.event.Level

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.testableModule(testing: Boolean = false) {
    install(ContentNegotiation) {
        json(
                json = apiJson,
                contentType = ContentType.Application.Json
        )
    }
    install(CallLogging) {
        level = Level.DEBUG
        filter { call -> call.request.path().startsWith("/") }
    }

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
fun Application.OATestableModule(testing: Boolean = false) {

    testableModule(testing)

    install(OfficialAccountFeature) {
        appId = "wxb11529c136998cb6"
        secret = "your_app_secret_key"
        encodingAESKey = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG"
        token = "pamtest"
        msgHandler = TestOAMsgHandler()
        accessToken = TestAccessTokenValue()
        ticket = TestJsTicketValue()
    }


    routing {
        officialAccountApi()
    }
}


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.WorkTestableModule(testing: Boolean = false) {

    testableModule(testing)

    install(WorkFeature) {
        corpId = "wx5823bf96d3bd56c7"
        secret = "your_app_secret_key"
        encodingAESKey = "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C"
        token = "QDG6eK"

        msgHandler = TestWorkMsgHandler()
        accessToken = TestAccessTokenValue()
    }


    routing {
        workApi()
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
        return ReTextMsg("reply the msg: ${msg.content},msgId=${msg.msgId}", msg.base.fromUserName, msg.base.toUserName)
    }

    override fun onDefault(msg: WxBaseMsg): ReBaseMSg? {
        return ReTextMsg("default reply the msg: msgId=${msg.msgId}", msg.base.fromUserName, msg.base.toUserName)
    }
}

class TestWorkMsgHandler: DefaultWorkMsgHandler()
{
    override  fun onWorkTextMsg(msg: WorkTextMsg): ReBaseMSg?{
        return ReTextMsg("reply the msg: content=${msg.content},msgId=${msg.msgId},agentId=${msg.agentId}", msg.base.fromUserName, msg.base.toUserName)
    }

    override fun onDefault(msg: WorkBaseMsg): ReBaseMSg? {
        return ReTextMsg("default reply the msg:: msgId=${msg.msgId},agentId=${msg.agentId}", msg.base.fromUserName, msg.base.toUserName)
    }
}
