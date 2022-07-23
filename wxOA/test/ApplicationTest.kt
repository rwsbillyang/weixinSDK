package com.github.rwsbillyang.wxOA.test

import com.github.rwsbillyang.ktorKit.testModule
import com.github.rwsbillyang.wxOA.wxOaAppModule
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*


class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ testModule(wxOaAppModule) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }
}
