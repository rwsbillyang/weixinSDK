/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-05 11:08
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

package com.github.rwsbillyang.wxSDK.work

import com.github.rwsbillyang.wxSDK.IBase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class WxKefuApi(corpId: String) : WorkBaseApi(corpId, null, null) {

    override val group = "kf"
    override var sysAccessTokenKey: String? = SysAgentKey.WxKeFu.name

    /**
     * @param name	是	string	客服名称不多于16个字符
     * @param media_id	是	string	客服头像临时素材。可以调用上传临时素材接口获取。不多于128个字节
     * */
    fun accountCreate(name: String, mediaId: String) = doPostRaw("account/add",
        mapOf("name" to name, "media_id" to mediaId))

    fun accountDel(openKfid: String) = doPostRaw("account/del",
        mapOf("open_kfid" to openKfid))
    fun accountUpdate(openKfid: String,name: String, mediaId: String) = doPostRaw("account/update",
        mapOf("open_kfid" to openKfid,"name" to name, "media_id" to mediaId))

    fun accountList(offset: Int = 0, limit: Int = 100): KfAccountListResponse = doPost(
        "account/list",
        mapOf("offset" to offset.toString(), "limit" to limit.toString())
    )
    /**
     * 获取客服帐号链接 https://developer.work.weixin.qq.com/document/path/94665
     * @param open_kfid	是	string	客服帐号ID
     * @param scene	否	string	场景值，字符串类型，由开发者自定义。
     * 不多于32字节 字符串取值范围(正则表达式)：[0-9a-zA-Z_-]*
     * */
    fun addScene(openKfid: String, scene: String):KfAddSceneResponse = doPost(
        "add_contact_way",
        mapOf("open_kfid" to openKfid,"scene" to scene)
    )

    /**
     * @param cursor	否	string	上一次调用时返回的next_cursor，第一次拉取可以不填。若不填，从3天内最早的消息开始返回 不多于64字节
     * @param token	否	string	回调事件返回的token字段，10分钟内有效；可不填，如果不填接口有严格的频率限制。不多于128字节
     * @param limit	否	uint32	期望请求的数据量，默认值和最大值都为1000。
     * 注意：可能会出现返回条数少于limit的情况，需结合返回的has_more字段判断是否继续请求。
     * @param voice_format	否	uint32	语音消息类型，0-Amr 1-Silk，默认0。可通过该参数控制返回的语音格式，开发者可按需选择自己程序支持的一种格式
     * @param open_kfid	否	string	指定拉取某个客服账号的消息，否则默认返回有权限的客服账号的消息。当客服账号较多，建议按open_kfid来拉取以获取更好的性能。
     * */
    fun syncMsg(token: String?, open_kfid: String?, cursor: String?, limit: Int = 1000, voice_format: Int = 0):WxKfSyncMsgResponse
    = doPost("sync_msg", SyncMsgBody(token, open_kfid,cursor, limit, voice_format))
}

@Serializable
class SyncMsgBody(
    val token: String?,
    val open_kfid: String?,
    val cursor: String?,
    val limit: Int = 1000,
    val voice_format: Int = 0
)
@Serializable
class KfAccountListResponse(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    val account_list: List<KfAccount>? = null
):IBase

@Serializable
class KfAddSceneResponse(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    val url: String? = null
):IBase

@Serializable
class KfAccount(
    val open_kfid: String,
    val name: String,
    val avatar: String,
    val manage_privilege: Boolean
)

/*
 {
    "errcode": 0,
    "errmsg": "ok",
    "next_cursor": "4gw7MepFLfgF2VC5npN",
	"has_more": 1,
    "msg_list": [
        {
            "msgid": "from_msgid_4622416642169452483",
            "open_kfid": "wkAJ2GCAAASSm4_FhToWMFea0xAFfd3Q",
            "external_userid": "wmAJ2GCAAAme1XQRC-NI-q0_ZM9ukoAw",
            "send_time": 1615478585,
            "origin": 3,
			"servicer_userid": "Zhangsan",
            "msgtype": "MSG_TYPE"
        }
    ]
}
 */
@Serializable
class WxKfSyncMsgResponse(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    val next_cursor: String? = null,
    val has_more: Int? = null,
    val msg_list: List<JsonObject>? = null
):IBase

