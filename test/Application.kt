package com.github.rwsbillyang.wxSDK.test

import com.github.rwsbillyang.wxSDK.OfficialAccountFeature
import com.github.rwsbillyang.wxSDK.WorkFeature
import com.github.rwsbillyang.wxSDK.officialAccountApi
import com.github.rwsbillyang.wxSDK.workApi
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.features.*
import io.ktor.http.*

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.testableModule(testing: Boolean = false) {
    install(ContentNegotiation) {
    }

    install(OfficialAccountFeature) {
        appId = "your_app_id"
        secret = "your_app_secret_key"
        encodingAESKey = "your_encodingAESKey or null "
        token = "your_token"
    }
    install(WorkFeature) {
        corpId = "your_app_id"
        secret = "your_app_secret_key"
        encodingAESKey = "your_encodingAESKey"
        token = "your_token"
    }


    class AuthenticationException : RuntimeException()
    class AuthorizationException : RuntimeException()

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        officialAccountApi()
        workApi()

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


