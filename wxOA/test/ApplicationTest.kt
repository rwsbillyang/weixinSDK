package com.github.rwsbillyang.wxOA.test

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
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
}
