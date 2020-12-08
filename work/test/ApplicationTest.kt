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

package com.github.rwsbillyang.wxSDK.work.test

import com.github.rwsbillyang.wxSDK.security.SHA1
import com.github.rwsbillyang.wxSDK.security.XmlUtil
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.WorkBaseApi


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
    fun testWorkUrl(){
        withTestApplication({ WorkTestableModule(testing = true) }) {
            //TODO: how to get the encryptEchoStr from "1616140317555161061"
            val encryptEcho = "P9nAzCzyDtyTWESHep1vC5X9xho/qYX3Zpb4yKa9SKld1DsH3Iyt3tP3zNdtp+4RPcs8TgAE7OaBO+FZXvnaqQ=="
            //val encryptEcho = _WORK.wxBizMsgCrypt.encrypt(sVerifyEchoStr)
            val agentName = WorkBaseApi.KeyBase
            val ctx = Work.ApiContextMap[corpId]!!.agentMap[agentName]!!

            val signature =  SHA1.getSHA1(ctx.token!!, timestamp, nonce, encryptEcho)
            val getUrl  = "${ctx.msgNotifyUri}?msg_signature=$signature&timestamp=$timestamp&nonce=$nonce&echostr=$encryptEcho"

//            handleRequest(HttpMethod.Get, getUrl).apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                println("getUrl=$getUrl")
//                assertEquals(echoStr, response.content)
//            }


            //原始消息文本
            val originalTextMsg = "<xml><ToUserName><![CDATA[$toUser]]></ToUserName><FromUserName><![CDATA[${corpId}]]></FromUserName><CreateTime>1348831860</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[$content]]></Content><MsgId>$msgId</MsgId><AgentID>128</AgentID></xml>"

            //将原始文本用timestamp和nonce拼接后，用sha1加密，得到加密消息，再置于post data中的Encrypt的标签中
            val (xml, msgSignature) = ctx.wxBizMsgCrypt!!.encryptMsg(originalTextMsg,timestamp, nonce, toUser,128)


            val postUrl = "${ctx.msgNotifyUri}?msg_signature=$msgSignature&timestamp=$timestamp&nonce=$nonce"
            handleRequest(HttpMethod.Post,postUrl){
                setBody(xml)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                if(response.content.isNullOrBlank()){
                    println("in wx work post, get empty response")
                }else{
                    if(response.content!!.startsWith("<xml>")){
                        val map = XmlUtil.extract(response.content!!)
                        val reTimeStamp =  map["TimeStamp"]?:""
                        val reNonce = map["Nonce"]?:""
                        val reEcrypt = map["Encrypt"]?:""
                        val signature2 = SHA1.getSHA1(ctx.token!!, reTimeStamp, reNonce, reEcrypt)
                        val msg = ctx.wxBizMsgCrypt!!.decryptWxMsg(signature2,reTimeStamp,reNonce,response.content!!)
                        println("Got wx work reply: $msg")
                    }else{
                        println("in wx work post, got response: ${response.content}")
                    }
                }
            }


        }
    }

}