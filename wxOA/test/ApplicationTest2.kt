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


import com.github.rwsbillyang.ktorKit.ApiJson

import com.github.rwsbillyang.wxSDK.officialAccount.OAuthApi
import com.github.rwsbillyang.wxSDK.officialAccount.OfficialAccount
import com.github.rwsbillyang.wxSDK.officialAccount.ResponseOauthAccessToken
import com.github.rwsbillyang.wxSDK.security.SignUtil
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import org.junit.Test
import kotlin.test.assertEquals


class ApplicationTest2 {
    private val timestamp = System.currentTimeMillis().toString()
    private val nonce: String = WXBizMsgCrypt.getRandomStr(6)
    private val echoStr = "123456"
    private val msgId = "1234567890123456"
    private val content = "this is a test content"
    private val toUser = "zhangsan"

    @Test
    fun testRoot() = testApplication {
        application {
            testableModule()
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testOAUrlGet() = testApplication {
        application {
            apiTest()
        }
        configTestOA()

        val signature = SignUtil.getSignature(OfficialAccount.ApiContextMap[AppIdForTest]!!.token, timestamp, nonce)
        val getUrl =
            "${OfficialAccount.msgUri}/$AppIdForTest?signature=$signature&timestamp=$timestamp&nonce=$nonce&echostr=$echoStr"

        val response = client.get(getUrl)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(echoStr, response.bodyAsText())
    }

    @Test
    fun testOAUrlPost() = testApplication {
        application {
            apiTest()
        }
        configTestOA()

        //原始消息文本
        val originalTextMsg =
            "<xml><ToUserName><![CDATA[$toUser]]></ToUserName><FromUserName><![CDATA[${AppIdForTest}]]></FromUserName><CreateTime>1348831860</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[$content]]></Content><MsgId>${msgId}</MsgId></xml>"
        //将原始文本用timestamp和nonce拼接后，用sha1加密，得到加密消息，再置于post data

        val (xml, msgSignature) = OfficialAccount.ApiContextMap[AppIdForTest]?.wxBizMsgCrypt!!.encryptMsg(
            AppIdForTest,
            originalTextMsg,
            timestamp,
            nonce,
            toUser
        )

        println("post: msgSignature=$msgSignature, xml=$xml")

        val postUrl =
            "${OfficialAccount.msgUri}/$AppIdForTest?msg_signature=$msgSignature&timestamp=$timestamp&nonce=$nonce&encrypt_type=aes"

        val response = client.get(postUrl) {
            setBody(xml)
        }
        assertEquals(HttpStatusCode.OK, response.status)

    }

    @Test
    fun testJsonSerialize() = testApplication {
        val box1 = ResponseOauthAccessToken(accessToken = "This_is_accessToken", expire = 7200)

        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json(ApiJson.serverSerializeJson)
            }
            routing {
                get("/") {
                    call.respond(box1)
                }
            }
        }
        val client = createClient {
            //this@testApplication.
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                //json(ApiJson.clientApiJson, ContentType.Application.Json)
                json(ApiJson.clientApiJson)
            }
        }


        val box2: ResponseOauthAccessToken = client.get("/").body()
        assertEquals(box1.accessToken, box2.accessToken)
        assertEquals(box1.expire, box2.expire)
    }

    //@Test
    fun testResponseOauthAccessToken() = testApplication {
        application {
            apiTest()
        }
        configTestOA()

        val oauthAi = OAuthApi(AppIdForTest)
        val res = oauthAi.getAccessToken("code")
        //val url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code"
        //val res: ResponseOauthAccessToken = oauthAi.doGetByUrl(url)
        //val res: ResponseOauthAccessToken = DefaultClient.get(url).body()
        println("testResponseOauthAccessToken: res.errCode=${ApiJson.serializeJson.encodeToString(res)}")
        assertEquals(true, res.isOK())
    }

    //@Test
    fun testResponseOauthAccessToken2() = testApplication {
        println("====================enter  testResponseOauthAccessToken2====================")
        application {
            apiTest()
        }
        configTestOA()

        val res = client.get("/api/wx/oa/oauth/notify1/$AppIdForTest?code=CODE&state=STATE")

        //println("testResponseOauthAccessToken: res.errCode=${res.errCode},res.errMsg=${res.errMsg}")
        assertEquals(HttpStatusCode.OK, res.status)
        println("====================end  testResponseOauthAccessToken2====================")
    }
}