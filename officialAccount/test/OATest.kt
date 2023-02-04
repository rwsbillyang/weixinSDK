/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 16:21
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


import com.github.rwsbillyang.wxSDK.security.AesException
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.Assert
import org.junit.Test
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import kotlin.test.assertEquals


class OATest {
    private val encodingAesKey = "O3MmgDFDYHCDOi9LPdbs57cmKkYQuywUXgnSs1Vb37Q"
    private val token = "pamtest"
    private val timestamp = "1409304348"
    private val nonce = "xxxxxx"
    private val appId = "wxb11529c136998cb6"
    private val replyMsg = "<xml><ToUserName><![CDATA[oia2TjjewbmiOUlr6X-1crbLOvLw]]></ToUserName><FromUserName><![CDATA[gh_7f083739789a]]></FromUserName><CreateTime>1407743423</CreateTime><MsgType><![CDATA[video]]></MsgType><Video><MediaId><![CDATA[eYJ1MbwPRJtOvIEabaxHs7TX2D-HV71s79GUxqdUkjm6Gs2Ed1KF3ulAOA9H1xG0]]></MediaId><Title><![CDATA[testCallBackReplyVideo]]></Title><Description><![CDATA[testCallBackReplyVideo]]></Description></Video></xml>";
    private val xmlFormat =
        "<xml><ToUserName><![CDATA[toUser]]></ToUserName><Encrypt><![CDATA[%1%s]]></Encrypt></xml>"

    private val wxBizMsgCrypt = WXBizMsgCrypt(token, encodingAesKey)

//
//    @Test
//    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
//    fun testNormal() {
//        try {
//            val (xml, msgSignature)  = wxBizMsgCrypt.encryptMsg(appId, replyMsg,  timestamp, nonce,"toUser")
//
////            val dbf = DocumentBuilderFactory.newInstance()
////            val db = dbf.newDocumentBuilder()
////            val sr = StringReader(afterEncrpt)
////            val `is` = InputSource(sr)
////            val document = db.parse(`is`)
////            val root = document.documentElement
//
//            //val encrypt = root.getElementsByTagName("Encrypt").item(0).textContent
//            //val fromXML = "<xml><ToUserName><![CDATA[toUser]]></ToUserName><Encrypt><![CDATA[%1$encrypt]]></Encrypt></xml>"
//
//            //val msgSignature = root.getElementsByTagName("MsgSignature").item(0).textContent
//            // 第三方收到公众号平台发送的消息
//            val data = XmlUtil.extract(xml)["Encrypt"]?:""
//            val afterDecrpt = wxBizMsgCrypt.decryptWxMsg(appId, msgSignature, timestamp, nonce, data,"aes")
//
//            //println("afterDecrpt=$afterDecrpt")
//            Assert.assertEquals(afterDecrpt, afterDecrpt)
//        } catch (e: AesException) {
//            Assert.fail("正常流程，怎么就抛出异常了？？？？？？")
//        }
//    }


    /**
     * 测试encodingAesKey的合法性
     * */
    @Test
    fun testIllegalAesKey() {
        try {
            WXBizMsgCrypt(token, "abcde")
        } catch (e: AesException) {
            Assert.assertEquals(AesException.IllegalAesKey, e.code)
            return
        }
        Assert.fail("错误流程不抛出异常？？？")
    }

    /**
     * 测试encodingAesKey的合法性
     * */
   @Test
    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    fun testValidateSignatureError() {
        try {
            val afterEncrpt = wxBizMsgCrypt.encryptMsg(appId, replyMsg,timestamp, nonce,"toUser").first
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val sr = StringReader(afterEncrpt)
            val `is` = InputSource(sr)
            val document = db.parse(`is`)
            val root = document.documentElement
            val nodelist1 = root.getElementsByTagName("Encrypt")
            val encrypt = nodelist1.item(0).textContent
            val fromXML = String.format(xmlFormat, encrypt)
            wxBizMsgCrypt.decryptWxMsg(appId, "12345", timestamp, nonce, fromXML) // 这里签名错误
        } catch (e: AesException) {
            Assert.assertEquals(AesException.ValidateSignatureError, e.code)
            return
        }
        Assert.fail("错误流程不抛出异常？？？")
    }


    private fun url(appId:String, needUserInfo: Int, separator: String,owner: String?)
            = if(owner== null)"/notify/${appId}/${separator}/${needUserInfo}"
    else "/notify/${appId}/${separator}/${needUserInfo}/${owner}"

    private fun result(appId:String?, needUserInfo: Int?, separator: String?,owner: String?) = if(owner == null) "${appId}/${needUserInfo}/${separator}/${owner}" else
        "${appId}/${needUserInfo}/${separator}"
    private fun Application.notifyTestableModule() {
        routing {
            get("/notify/{appId}/{separator}/{needUserInfo}/{owner?}") {
                val appId = call.parameters["appId"]
                val needUserInfo = call.parameters["needUserInfo"]?.toInt()
                val separator = call.parameters["separator"]
                val owner = call.parameters["owner"]

                val result = if(owner == null) "${appId}/${needUserInfo}/${separator}/${owner}" else
                    "${appId}/${needUserInfo}/${separator}"

                call.respondText(result(appId, needUserInfo, separator, owner), contentType = ContentType.Text.Plain)
            }
        }
    }

   // @Test
    fun testNotifyPath1() = testApplication{
        application {
            notifyTestableModule()
        }
        val appId = "TestAppId"
        val needUserInfo = 0
        val separator = "#!" //路径不支持，提示404 not found
        val owner = null

        val url = url(appId, needUserInfo, separator, owner)
        println("url=$url")
        val response = client.get(url)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(result(appId, needUserInfo, separator, owner), response.bodyAsText())
    }

    //@Test
    fun testNotifyPath2() = testApplication{
        application {
            notifyTestableModule()
        }
        val appId = "TestAppId"
        val needUserInfo = 0
        val separator = "#"//路径不支持，提示404 not found
        val owner = null

        val response = client.get(url(appId, needUserInfo, separator, owner))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(result(appId, needUserInfo, separator, owner), response.bodyAsText())
    }
    @Test
    fun testNotifyPath3() = testApplication{
        application {
            notifyTestableModule()
        }
        val appId = "TestAppId"
        val needUserInfo = 0
        val separator = "0"
        val owner = null

        val response = client.get(url(appId, needUserInfo, separator, owner))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(result(appId, needUserInfo, separator, owner), response.bodyAsText())
    }
    //@Test
    fun testNotifyPath4() = testApplication{
        application {
            notifyTestableModule()
        }
        val appId = "TestAppId"
        val needUserInfo = 0
        val separator = ""
        val owner = null

        val response = client.get(url(appId, needUserInfo, separator, owner))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(result(appId, needUserInfo, separator, owner), response.bodyAsText())
    }

    @Test
    fun testNotifyPath5() = testApplication{
        application {
            notifyTestableModule()
        }
        val appId = "TestAppId"
        val needUserInfo = 0
        val separator = "0"
        val owner = "testowner"

        val response = client.get(url(appId, needUserInfo, separator, owner))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(result(appId, needUserInfo, separator, owner), response.bodyAsText())
    }
}
