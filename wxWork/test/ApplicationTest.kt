package com.github.rwsbillyang.wxWork.test

import com.github.rwsbillyang.wxSDK.work.Work
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            Work.isMulti = false
            Work.isIsv = false
            testableModule(true)
        }

        val response = client.get("/ok")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK from wxSDK", response.bodyAsText())
    }

}
