/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 16:20
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

package com.github.rwsbillyang.wxSDK.officialAccount.test

import com.github.rwsbillyang.wxSDK.security.SHA1
import com.github.rwsbillyang.wxSDK.security.SignUtil
import com.github.rwsbillyang.wxSDK.security.XmlUtil
import com.github.rwsbillyang.wxSDK.officialAccount.OfficialAccount
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.server.testing.*
import org.apache.commons.lang3.RandomStringUtils


import org.junit.Test
import kotlin.test.*


class ApplicationTest {
    private val timestamp = System.currentTimeMillis().toString()
    private val nonce: String = RandomStringUtils.randomAlphanumeric(6)
    private val echoStr = "123456"
    private val msgId= "1234567890123456"
    private val content = "this is a test content"
    private val toUser = "zhangsan"

    @Test
    fun testRoot() {
        withTestApplication({ testableModule(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK from wxSDK", response.content)
            }
        }
    }

    @Test
    fun testOAUrlGet(){
        withTestApplication({ apiTest(testing = true) }) {

            val signature = SignUtil.getSignature(OfficialAccount.ApiContextMap[AppIdForTest]?.token!!,timestamp, nonce)
            val getUrl  = "${OfficialAccount.msgUri}/$AppIdForTest?signature=$signature&timestamp=$timestamp&nonce=$nonce&echostr=$echoStr"

            handleRequest(HttpMethod.Get,getUrl).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(echoStr, response.content)
            }
        }
    }

    @Test
    fun testOAUrlPost(){
        withTestApplication({ apiTest(testing = true) }) {

            //原始消息文本
            val originalTextMsg = "<xml><ToUserName><![CDATA[$toUser]]></ToUserName><FromUserName><![CDATA[${OfficialAccount.ApiContextMap[AppIdForTest]?.appId}]]></FromUserName><CreateTime>1348831860</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[$content]]></Content><MsgId>${msgId}</MsgId></xml>"
            //将原始文本用timestamp和nonce拼接后，用sha1加密，得到加密消息，再置于post data
            val (xml, msgSignature) = OfficialAccount.ApiContextMap[AppIdForTest]?.wxBizMsgCrypt!!.encryptMsg(AppIdForTest, originalTextMsg, timestamp, nonce,toUser)
            println("post: msgSignature=$msgSignature, xml=$xml")

            val postUrl = "${OfficialAccount.msgUri}/$AppIdForTest?msg_signature=$msgSignature&timestamp=$timestamp&nonce=$nonce&encrypt_type=aes"
            handleRequest(HttpMethod.Post,postUrl){
                setBody(xml)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

}