/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 11:56
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
package com.github.rwsbillyang.wxOA.test

import com.github.rwsbillyang.wxSDK.officialAccount.CustomerServiceApi

import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class ApiTest {
    @Test
    fun testCustomerServiceApi() {
        withTestApplication({ apiTest(testing = true) }) {
            val res = CustomerServiceApi(AppIdForTest).getAccountList()
            println(Json.encodeToString(res))
            //assert(res.isOK())
        }
    }
}