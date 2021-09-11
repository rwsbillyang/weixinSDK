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
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.security.XmlUtil
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.WorkMulti
import com.github.rwsbillyang.wxSDK.work.WorkSingle


import io.ktor.http.*
import io.ktor.server.testing.*


import org.junit.Test
import kotlin.test.*


class ApplicationTest {
    private val timestamp = System.currentTimeMillis().toString()
    private val nonce: String = WXBizMsgCrypt.getRandomStr(6)
    private val echoStr = "123456"
    private val msgId = "1234567890123456"
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
    fun testWorkUrlGet() {
        withTestApplication({ WorkTestableModule(testing = true) }) {
            val agentName = TestConstatns.KeyBase

            val ctx = if (Work.isMulti)
                WorkMulti.ApiContextMap[TestConstatns.CorpId]!!.agentMap[agentName]!!
            else WorkSingle.agentContext

            //加密第二个参数
            val encryptEcho = ctx.wxBizMsgCrypt!!.encrypt(TestConstatns.CorpId,echoStr)

            //对加密后的内容进行签名
            val signature = SHA1.getSHA1(ctx.token!!, timestamp, nonce, encryptEcho)

            val url = "${Work.msgNotifyUri}/${TestConstatns.CorpId}/${agentName}?msg_signature=$signature&timestamp=$timestamp&nonce=$nonce&echostr=$encryptEcho"
            println("getUrl=$url")

            handleRequest(HttpMethod.Get, url).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                println("Tx: token=${ctx.token}, timestamp=$timestamp, nonce=$nonce, encryptEcho=$encryptEcho")
                assertEquals(echoStr, response.content)
            }
        }
    }


    @Test
    fun testWorkUrlPost() {
        withTestApplication({ WorkTestableModule(testing = true) }) {
            val agentName = TestConstatns.KeyBase
            val ctx = if (Work.isMulti)
                WorkMulti.ApiContextMap[TestConstatns.CorpId]!!.agentMap[agentName]!!
            else WorkSingle.agentContext


            //原始消息文本
            val originalTextMsg =
                "<xml><ToUserName><![CDATA[$toUser]]></ToUserName><FromUserName><![CDATA[${TestConstatns.CorpId}]]></FromUserName><CreateTime>1348831860</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[$content]]></Content><MsgId>$msgId</MsgId><AgentID>128</AgentID></xml>"

            //将原始文本用timestamp和nonce拼接后，用sha1加密，得到加密消息，再置于post data中的Encrypt的标签中
            val (xml, msgSignature) = ctx.wxBizMsgCrypt!!.encryptMsg(
                TestConstatns.CorpId,
                originalTextMsg,
                timestamp,
                nonce,
                toUser,
                agentName
            )

            //println("post Tx: msgSignature=$msgSignature, token=${ctx.token},timestamp=$timestamp,nonce=$nonce, encryptText=$xml")
            val postUrl = "${Work.msgNotifyUri}/${TestConstatns.CorpId}/${agentName}?msg_signature=$msgSignature&timestamp=$timestamp&nonce=$nonce"
            println("postUrl=$postUrl")
            handleRequest(HttpMethod.Post, postUrl) {
                setBody(xml)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content
                if (content.isNullOrBlank()) {
                    println("in wx work post, get empty response")
                } else {
                    if (content.startsWith("<xml>")) {
                        val map = XmlUtil.extract(content)
                        val reTimeStamp = map["TimeStamp"] ?: ""
                        val reNonce = map["Nonce"] ?: ""
                        val reEcrypt = map["Encrypt"] ?: ""
                        val signature2 = SHA1.getSHA1(ctx.token!!, reTimeStamp, reNonce, reEcrypt)
                        val msg = ctx.wxBizMsgCrypt!!.decryptWxMsg(TestConstatns.CorpId, signature2, reTimeStamp, reNonce, content)
                        println("Got wx work reply: $msg")
                    } else {
                        println("in wx work post, got response: $content")
                    }
                }
            }

        }
    }

}