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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 临时素材管理
 * */
class MediaApi(appId: String) : OABaseApi(appId){
    override val group: String = "media"

    /**
     * 上传临时素材
     * 媒体文件在微信后台保存时间为3天，即3天后media_id失效。
     * */
    fun uploadTmp(file: String, type: MediaType): ResponseUploadMedia = doUpload("upload",file, mapOf("type" to type.value))

    /**
     * 上传图文消息内的图片,返回其URL,可放置图文消息中使用
     *
     * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html
     *
     * 本接口所上传的图片不占用公众号的素材库中图片数量的100000个的限制。图片仅支持jpg/png格式，大小必须在1MB以下。
     * @param file 文件路径
     * @return 上传图片的URL，可用于后续群发中，放置到图文消息中。错误时微信会返回错误码等信息，请根据错误码查询错误信息
     * */
    fun uploadNewsImage(file: String): ResponseUploadNewsImg = doUpload("uploadimg", file)

    /**
     * 下载视频
     * */
    fun downVideo(mediaId: String): ResponseDownVideo = doGet("get", mapOf("media_id" to mediaId))

    /**
     * 返回一个网址自行在浏览器中下载
     * */
    fun downOtherMediaUrl(mediaId: String) = url("get",mapOf("media_id" to mediaId))

    /**
     * 高清语音素材获取接口
     *
     * 公众号可以使用本接口获取从JSSDK的uploadVoice接口上传的临时语音素材，格式为speex，16K采样率。该音频比上文的临时素材获取接口
     * （格式为amr，8K采样率）更加清晰，适合用作语音识别等对音质要求较高的业务。
     * */
    fun downHiVoiceUrl(mediaId: String) = url("get/jssdk",mapOf("media_id" to mediaId))

}
/**
 * @property IMAGE 图片（image）: 10M，支持PNG\JPEG\JPG\GIF格式
 * @property VOICE 语音（voice）：2M，播放长度不超过60s，支持AMR\MP3格式
 * @property VIDEO 视频（video）：10MB，支持MP4格式
 * @property THUMB 缩略图（thumb）：64KB，支持JPG格式
 * */
enum class MediaType(val value: String){
   NEWS("news"), IMAGE("image"), VOICE("voice"), VIDEO("video"), THUMB("thumb")
}

/**
 * @param type	媒体文件类型，分别有图片（image）、语音（voice）、视频（video）和缩略图（thumb，主要用于视频与音乐格式的缩略图）
 * @param mediaId media_id	媒体文件上传后，获取标识
 * @param createAt created_at	媒体文件上传时间戳
 * */
@Serializable
class ResponseUploadMedia(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        val type: String? = null,
        @SerialName("media_id")
        val mediaId: String? = null,
        @SerialName("created_at")
        val createAt: Long? = null
): IBase

/**
 * @param videoUrl 若是视频，则videoUrl为视频网址，其它情况均为默认值null
 * https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/Get_temporary_materials.html
 * */
@Serializable
class ResponseDownVideo(
    @SerialName("errcode")
    override val errCode: Int = 0,
    @SerialName("errmsg")
    override val errMsg: String? = null,
    val videoUrl: String? = null
):IBase


@Serializable
class ResponseUploadNewsImg(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        val url: String? = null
):IBase