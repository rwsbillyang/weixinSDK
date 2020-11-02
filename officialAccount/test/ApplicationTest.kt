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

import com.github.rwsbillyang.wxSDK.aes.SHA1
import com.github.rwsbillyang.wxSDK.aes.SignUtil
import com.github.rwsbillyang.wxSDK.aes.XmlUtil
import com.github.rwsbillyang.wxSDK.officialAccount.OfficialAccount
import io.ktor.http.*
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
    fun testOAUrl(){
        withTestApplication({ apiTest(testing = true) }) {

            val signature = SignUtil.getSignature(OfficialAccount.OA.token,timestamp, nonce)
            val getUrl  = "${OfficialAccount.wxEntryPoint}?signature=$signature&timestamp=$timestamp&nonce=$nonce&echostr=$echoStr"

            handleRequest(HttpMethod.Get,getUrl).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(echoStr, response.content)
            }


            //原始消息文本
            val originalTextMsg = "<xml><ToUserName><![CDATA[$toUser]]></ToUserName><FromUserName><![CDATA[${OfficialAccount.OA.appId}]]></FromUserName><CreateTime>1348831860</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[$content]]></Content><MsgId>${msgId}</MsgId></xml>"
            //将原始文本用timestamp和nonce拼接后，用sha1加密，得到加密消息，再置于post data
            val (xml, msgSignature) = OfficialAccount.OA.wxBizMsgCrypt!!.encryptMsg(originalTextMsg, timestamp, nonce,toUser)

            val postUrl = "${OfficialAccount.wxEntryPoint}?msg_signature=$msgSignature&timestamp=$timestamp&nonce=$nonce&encrypt_type=aes"
            handleRequest(HttpMethod.Post,postUrl){
                setBody(xml)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                if(response.content.isNullOrBlank()){
                    println("in OA post, get empty response")
                }else{
                    if(response.content!!.startsWith("<xml>")){
                        val map = XmlUtil.extract(response.content!!)
                        val reTimeStamp =  map["TimeStamp"]?:""
                        val reNonce = map["Nonce"]?:""
                        val reEcrypt = map["Encrypt"]?:""
                        val signature2 = SHA1.getSHA1(OfficialAccount.OA.token, reTimeStamp, reNonce, reEcrypt)
                        val msg = OfficialAccount.OA.wxBizMsgCrypt!!.decryptWxMsg(signature2,reTimeStamp,reNonce,response.content!!)
                        println("Got OA reply: $msg")
                    }else{
                        println("in OA post, got response: ${response.content}")
                    }
                }

                //assertEquals("content=$${content},msgId=${msgId}", response.content)
            }
        }
    }

}