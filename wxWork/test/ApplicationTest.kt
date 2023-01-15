package com.github.rwsbillyang.wxWork.test

import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.ktorKit.ApiJson.serverSerializeJson
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.util.utcToLocalDateTime
import com.github.rwsbillyang.wxSDK.work.EnterSessionContext
import com.github.rwsbillyang.wxSDK.work.WechatChannels
import com.github.rwsbillyang.wxSDK.work.Work
import com.github.rwsbillyang.wxSDK.work.WxKfSyncMsgResponse
import com.github.rwsbillyang.wxWork.wxkf.WxkfEventHandler
import com.github.rwsbillyang.wxWork.wxkf.WxkfMsg
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.litote.kmongo.coroutine.CoroutineCollection
import java.net.URLDecoder
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            Work.isIsv = false
            testableModule(true)
        }

        val response = client.get("/ok")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK from wxSDK", response.bodyAsText())
    }

   // @Test
    fun testWxkfEvent() = testApplication{
        val dbSource = MongoDataSource("wxAdmin")
        val wxKfMsgCol: CoroutineCollection<WxkfMsg> = dbSource.mongoDb.getCollection()
        try{
            val json = "{\"errcode\":0,\"errmsg\":\"ok\",\"next_cursor\":\"4gw7MepFLfgF2VC5npb\",\"msg_list\":[{\"msgid\":\"4rJQ5Bspe1Ft4FGsbbGiCnSWHHkkX3CQ3DadYwvhXMQC\",\"send_time\":1673578055,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"iIShBMyC1CJbonqJx16yjRtKSHzXHbTSJzMoA1Di-VA\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"4WmUMDx6HgwfSXzkmTBZjgPPDsnNN5B7YMHUhMVv3aWC\",\"send_time\":1673579617,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"6z6ju2dGucXHJ2WO1ggSv5L7OmG1t72VEq3qURQcUGs\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"7d2uv6UpjKHAGoEGbDk431GN5DsTtqy3mLEjFFLGDZ3J\",\"send_time\":1673581267,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"lj8_dNRP-QacJmV3C336mhJBPSGRsNyQVyU-vOg5T8Q\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"MXbuYYJYGqC7TXxWEPWqY4JzF3XKGKkkaQYWMnU8p\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"send_time\":1673582683,\"origin\":3,\"msgtype\":\"text\",\"text\":{\"content\":\"几\"}},{\"msgid\":\"4rFuqvAWXpKUtviTNnu2afUF9gb1bh6myU3hSeee8U5z\",\"send_time\":1673624501,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"DiDoSXaUDrG4UUZtaN1kXqd7l1VXIl05YEylHKEeSjU\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"4SwWz4ZcVW319DYmXyoAEt3BPVgJe4jYzoWcDsNvYs75\",\"send_time\":1673628265,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"c9sGj_FMnp_79VnWmMtZfRuQVN7zvcak_8MvJ9KNrWU\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"4SujPPz61jCizLz18rTvQRkTZc1SDcFdvrHaKPGPBrGR\",\"send_time\":1673677062,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"E4Bon0mQXOL-bEIPCb62sfrKbYBhpz_BJ4jH2vZR5iI\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"7thDb2qEQjnLNAe1MV5TMYNmkGVdwVqfXEJ5TWZauawJ\",\"send_time\":1673677825,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"gCNQdVbdPz-nKuQA20YW3iQ_alkwk8TJgGvIF-7RDZc\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"4KkxQ9BS12bEh6q64RoY3SvTpW7EMS1UyathasBs7upZ\",\"send_time\":1673685738,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"Oox5DXRp-YCDZpwiN3gDbS9OujvCPdlnrBJhvPp7GMc\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"4NwwaEkrws3n2ucUixJB2ReskjHi68ep9ZBk8Puvdtoq\",\"send_time\":1673686303,\"origin\":4,\"msgtype\":\"event\",\"event\":{\"event_type\":\"enter_session\",\"scene\":\"oamenu3\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"welcome_code\":\"AbejttHgwols4555DlGE15Gk7tu9OJyShca4E-2Hk4Y\",\"scene_param\":\"%E5%9C%BA%E6%99%AF3\"}},{\"msgid\":\"5XY51m5tZLvyTVSW2AnyMAe8wV7kFfSgi9kVGoRLU2jn\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"send_time\":1673746631,\"origin\":5,\"servicer_userid\":\"YangZhangGang\",\"msgtype\":\"text\",\"text\":{\"content\":\"hi\"}},{\"msgid\":\"MXbuYYJYGqC7TXxW3yiDZMzkBUKBvXVFXpXNGNYa7\",\"open_kfid\":\"wkfqLUQwAApHj0eaMhYPQOOamS3Wz17w\",\"external_userid\":\"wmfqLUQwAAHniQno9qrSYAQBiwoP6xog\",\"send_time\":1673751175,\"origin\":3,\"msgtype\":\"text\",\"text\":{\"content\":\"hi\"}}],\"has_more\":0}"
            val res: WxKfSyncMsgResponse = ApiJson.clientApiJson.decodeFromString(json)
            if(res.isOK() && res.msg_list != null){
                val list = res.msg_list!!.mapNotNull {
                    val msgId = it["msgid"]?.jsonPrimitive?.content
                    if (msgId == null) {
                        println("msgid is null?")
                        null
                    } else {
                        val msgtype = it["msgtype"]?.jsonPrimitive?.content
                        val content = if (msgtype != null) it[msgtype]?.jsonObject else null
                        val contentStr = content?.let { Json.encodeToString(it) }
                        WxkfMsg(
                            msgId, "appId",
                            it["send_time"]?.jsonPrimitive?.long,
                            it["origin"]?.jsonPrimitive?.int,
                            msgtype,
                            content,
                            contentStr,
                            it["open_kfid"]?.jsonPrimitive?.content ?: "eventKfId",
                            it["external_userid"]?.jsonPrimitive?.content,
                            it["servicer_userid"]?.jsonPrimitive?.content
                        )
                    }
                }

                val json2 = ApiJson.clientApiJson.encodeToString(list)
                println("WxkfMsgList="+ json2)
                wxKfMsgCol.insertMany(list) //Exception: This serializer can be used only with Json format.Expected Encoder to be JsonEncoder, got class com.github.jershell.kbson.BsonEncoder

                val entersMsgs = list.filter { it.msgtype == "event"
                        && WxkfEventHandler.WxkfEnterSession == it.json?.get("event_type")?.jsonPrimitive?.content
                }

                println("entersMsgs.size="+ entersMsgs.size)

                if(entersMsgs.isNotEmpty()){
                    val list3 = entersMsgs.map {
                        val enter = it.json!! //通过校验WxkfEnterSession必然非空
                        val channel = enter.get("wechat_channels")?.jsonObject?.let {
                            WechatChannels(
                                it.get("nickname")?.jsonPrimitive?.content?:"",
                                it.get("scene")?.jsonPrimitive?.int,
                                it.get("shop_nickname")?.jsonPrimitive?.content
                            )
                        }
                        EnterSessionContext(
                            enter.get("scene")?.jsonPrimitive?.content?:"",
                            enter.get("scene_param")?.jsonPrimitive?.content?.let{
                                URLDecoder.decode(it, "UTF-8")
                            },
                            enter.get("open_kfid")?.jsonPrimitive?.content, //客服帐号ID
                            enter.get("external_userid")?.jsonPrimitive?.content, //客户UserID
                            enter.get("welcome_code")?.jsonPrimitive?.content,
                            channel, it.send_time?.utcToLocalDateTime()?: LocalDateTime.now()
                        )
                    }
                    val json3 = serverSerializeJson.encodeToString(list3)
                    println("entersMsgs="+ json3)
                }
            }


        }catch (e: Exception){
            e.printStackTrace()
            assert(false){"Exception: ${e.message}"}
        }
        assert(false){"set failure manually"}
    }
}
