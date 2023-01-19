package com.github.rwsbillyang.wxOA.test

import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.wxOA.msg.MyMsg
import com.github.rwsbillyang.wxSDK.msg.TextContent
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.*


class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            testableModule()
        }

        val response = client.get("/ok")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK from wxSDK", response.bodyAsText())
    }

    @Test
    fun testSealClassDeserialize(){
        val json = "{\"appId\":\"wxc94c93e0938a84bb\",\"msg\":{\"_class\":\"text\",\"content\":\"xxxxx\"},\"name\":\"test\"}"
        val msg = ApiJson.serverSerializeJson.decodeFromString<MyMsg>(json)
        println("decode->encode: " + ApiJson.serverSerializeJson.encodeToString(msg))
        assertTrue("msg.msg is TextContent"){msg.msg is TextContent }
    }
}
