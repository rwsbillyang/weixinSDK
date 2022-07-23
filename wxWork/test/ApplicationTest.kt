package com.github.rwsbillyang.wxWork.test

import com.github.rwsbillyang.ktorKit.testModule
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxWork.wxWorkModule
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({
            Work.isMulti = false
            Work.isIsv = false
            testModule(wxWorkModule)
        }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }
}
