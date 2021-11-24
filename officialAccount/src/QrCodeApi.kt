/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:37
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

package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.IBase
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import java.net.URLEncoder

/**
 * 1、临时二维码，是有过期时间的，最长可以设置为在二维码生成后的30天（即2592000秒）后过期，但能够生成较多数量。
 * 临时二维码主要用于帐号绑定等不要求二维码永久保存的业务场景
 * 2、永久二维码，是无过期时间的，但数量较少（目前为最多10万个）。永久二维码主要用于适用于帐号绑定、用户来源统计等场景。
 *
 * 用户扫描带场景值二维码时，可能推送以下两种事件：
 * 如果用户还未关注公众号，则用户可以关注公众号，关注后微信会将带场景值关注事件推送给开发者。
 * 如果用户已经关注公众号，在用户扫描后会自动进入会话，微信也会将带场景值扫描事件推送给开发者。
 *
 * */
class QrCodeApi(appId: String) : OABaseApi(appId) {
    override val group: String = "qrcode"

    /**
     *  获取带参数的二维码的过程包括两步，首先创建二维码ticket，然后凭借ticket到指定URL换取二维码。
     * */
    fun createTmp(id: Int, expire: Long = 2592000L): ResponseCreate =
        doPost("create", QrCodeInfo(ActionName.QR_SCENE, Scene(id), expire))

    fun createTmp(str: String, expire: Long = 2592000L): ResponseCreate =
        doPost("create", QrCodeInfo(ActionName.QR_STR_SCENE, Scene(str = str), expire))

    fun create(id: Int, expire: Long = 2592000L): ResponseCreate =
        doPost("create", QrCodeInfo(ActionName.QR_LIMIT_SCENE, Scene(id), expire))

    fun create(str: String, expire: Long = 2592000L): ResponseCreate =
        doPost("create", QrCodeInfo(ActionName.QR_LIMIT_STR_SCENE, Scene(str = str), expire))

    /**
     * 通过ticket换取二维码
     *
     * ticket正确情况下，http 返回码是200，是一张图片，可以直接展示或者下载
     * TICKET已进行UrlEncode
     * */
    fun qrCodeUrl(ticket: String) =
        URLEncoder.encode("https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=$ticket", "utf-8")


    /**
     * 将一条长链接转成短链接。
     * 主要使用场景： 开发者用于生成二维码的原链接（商品、支付二维码等）太长导致扫码速度和成功率下降，
     * 将原长链接通过此接口转成短链接再生成二维码将大大提升扫码速度和成功率。
     * */
    fun shortUrl(url: String): String? = doPost4(mapOf("action" to "long2short", "long_url" to url)) {
        "https://api.weixin.qq.com/cgi-bin/shorturl?access_token=${accessToken()}"
    }["short_url"]?.toString()
}

/**
 * 二维码类型
 * QR_SCENE为临时的整型参数值
 * QR_STR_SCENE为临时的字符串参数值
 * QR_LIMIT_SCENE为永久的整型参数值
 * QR_LIMIT_STR_SCENE为永久的字符串参数值
 * */
@Serializable
enum class ActionName {
    QR_SCENE, QR_STR_SCENE, QR_LIMIT_SCENE, QR_LIMIT_STR_SCENE
}

/**
 * @param id scene_id	场景值ID，临时二维码时为32位非0整型，永久二维码时最大值为100000（目前参数只支持1--100000）
 * @param str scene_str	场景值ID（字符串形式的ID），字符串类型，长度限制为1到64
 * */
@Serializable(with = SceneSerializer::class)
class Scene(
    @SerialName("scene_id")
    val id: Int? = null,
    @SerialName("scene_str")
    val str: String? = null
)

object SceneSerializer : KSerializer<Scene> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Scene") {
            element<String>("scene_id", isOptional = true)
            element<String?>("scene_str", isOptional = true)
        }

    override fun serialize(encoder: Encoder, value: Scene) =
        encoder.encodeStructure(descriptor) {
            value.id?.let { encodeIntElement(descriptor, 0, it) }
            value.str?.let { encodeStringElement(descriptor, 1, it) }
        }

    override fun deserialize(decoder: Decoder): Scene {
        TODO("Not implement")
    }
}

/**
 * @param expire expire_seconds	该二维码有效时间，以秒为单位。 最大不超过2592000（即30天），此字段如果不填，则默认有效期为30秒。
 * */
@Serializable
class QrCodeInfo(
    @SerialName("action_name")
    val actionName: ActionName,
    val scene: Scene,
    @SerialName("expire_seconds")
    val expire: Long // = 2592000L
)

/**
 * @param ticket    获取的二维码ticket，凭借此ticket可以在有效时间内换取二维码。
 * @param expire expire_seconds	该二维码有效时间，以秒为单位。 最大不超过2592000（即30天）。
 * @param url    二维码图片解析后的地址，开发者可根据该地址自行生成需要的二维码图片
 * */
@Serializable
class ResponseCreate(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    val ticket: String? = null,
    @SerialName("expire_seconds")
    val expire: Long? = null,
    val url: String? = null
) : IBase
