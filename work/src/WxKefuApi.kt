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
}
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