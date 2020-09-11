package com.github.rwsbillyang.wxSDK.test

import com.github.rwsbillyang.wxSDK.common.aes.SHA1
import com.github.rwsbillyang.wxSDK.common.aes.SignUtil
import com.github.rwsbillyang.wxSDK.common.aes.XmlUtil
import com.github.rwsbillyang.wxSDK.officialAccount._OA
import com.github.rwsbillyang.wxSDK.work._WORK
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

            val signature = SignUtil.getSignature(_OA.token,timestamp, nonce)
            val getUrl  = "${_OA.callbackPath}?signature=$signature&timestamp=$timestamp&nonce=$nonce&echostr=$echoStr"

            handleRequest(HttpMethod.Get,getUrl).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(echoStr, response.content)
            }


            //原始消息文本
            val originalTextMsg = "<xml><ToUserName><![CDATA[$toUser]]></ToUserName><FromUserName><![CDATA[${_OA.appId}]]></FromUserName><CreateTime>1348831860</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[$content]]></Content><MsgId>${msgId}</MsgId></xml>"
            //将原始文本用timestamp和nonce拼接后，用sha1加密，得到加密消息，再置于post data
            val (xml, msgSignature) = _OA.wxBizMsgCrypt!!.encryptMsg(originalTextMsg, timestamp, nonce,toUser)

            val postUrl = "${_OA.callbackPath}?msg_signature=$msgSignature&timestamp=$timestamp&nonce=$nonce&encrypt_type=aes"
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
                        val signature2 = SHA1.getSHA1(_OA.token, reTimeStamp, reNonce, reEcrypt)
                        val msg = _OA.wxBizMsgCrypt!!.decryptWxMsg(signature2,reTimeStamp,reNonce,response.content!!)
                        println("Got OA reply: $msg")
                    }else{
                        println("in OA post, got response: ${response.content}")
                    }
                }

                //assertEquals("content=$${content},msgId=${msgId}", response.content)
            }
        }
    }

   @Test
    fun testWorkUrl(){
        withTestApplication({ WorkTestableModule(testing = true) }) {
            //TODO: how to get the encryptEchoStr from "1616140317555161061"
            val encryptEcho = "P9nAzCzyDtyTWESHep1vC5X9xho/qYX3Zpb4yKa9SKld1DsH3Iyt3tP3zNdtp+4RPcs8TgAE7OaBO+FZXvnaqQ=="
            //val encryptEcho = _WORK.wxBizMsgCrypt.encrypt(sVerifyEchoStr)
            val signature =  SHA1.getSHA1(_WORK.token, timestamp, nonce, encryptEcho)
            val getUrl  = "${_WORK.callbackPath}?msg_signature=$signature&timestamp=$timestamp&nonce=$nonce&echostr=$encryptEcho"

//            handleRequest(HttpMethod.Get, getUrl).apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                println("getUrl=$getUrl")
//                assertEquals(echoStr, response.content)
//            }


            //原始消息文本
            val originalTextMsg = "<xml><ToUserName><![CDATA[$toUser]]></ToUserName><FromUserName><![CDATA[${_WORK.corpId}]]></FromUserName><CreateTime>1348831860</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[$content]]></Content><MsgId>$msgId</MsgId><AgentID>128</AgentID></xml>"

            //将原始文本用timestamp和nonce拼接后，用sha1加密，得到加密消息，再置于post data中的Encrypt的标签中
            val (xml, msgSignature) = _WORK.wxBizMsgCrypt.encryptMsg(originalTextMsg,timestamp, nonce, toUser,128)


            val postUrl = "${_WORK.callbackPath}?msg_signature=$msgSignature&timestamp=$timestamp&nonce=$nonce"
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
                        val signature2 = SHA1.getSHA1(_WORK.token, reTimeStamp, reNonce, reEcrypt)
                        val msg = _WORK.wxBizMsgCrypt.decryptWxMsg(signature2,reTimeStamp,reNonce,response.content!!)
                        println("Got wx work reply: $msg")
                    }else{
                        println("in wx work post, got response: ${response.content}")
                    }
                }
            }


        }
    }

}