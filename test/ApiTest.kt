package com.github.rwsbillyang.wxSDK.test


import com.github.rwsbillyang.wxSDK.officialAccount.CustomerServiceApi
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class ApiTest {
    @Test
    fun testCustomerServiceApi() {
        withTestApplication({ apiTest(testing = true) }) {
            val res = CustomerServiceApi.getAccountList()
            println(Json.encodeToString(res))
            assert(res.isOK())
        }
    }
}