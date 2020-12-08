/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:38
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

class MaterialApi(corpId: String, agentId: Int) : WorkBaseApi(corpId, agentId){
    override val group = "media"

    /**
     * 上传临时素材
     *
     * 素材上传得到media_id，该media_id仅三天内有效, media_id在同一企业内应用之间可以共享
     *
     * POST的请求包中，form-data中媒体文件标识，应包含有 filename、filelength、content-type等信息
     * TODO: filename标识文件展示的名称。比如，使用该media_id发消息时，展示的文件名由该字段控制
     *
     * 所有文件size必须大于5个字节
     * 图片（image）：2MB，支持JPG,PNG格式
     * 语音（voice） ：2MB，播放长度不超过60s，仅支持AMR格式
     * 视频（video） ：10MB，支持MP4格式
     * 普通文件（file）：20MB
     * */
    fun upload(type: String, file: String) = doUpload3("upload", file, mapOf("type" to type))


    /**
     * 上传图片
     *
     * 上传图片得到图片URL，该URL永久有效
     * 返回的图片URL，仅能用于图文消息正文中的图片展示；若用于非企业微信域名下的页面，图片将被屏蔽。
     * 每个企业每天最多可上传100张图片,图片文件大小应在 5B ~ 2MB 之间
     * TODO: POST的请求包中，form-data中媒体文件标识，应包含有filename、content-type等信息
     * */
    fun uploadImg(file: String) = doUpload3("uploadimg", file)

    /**
     * TODO: 本接口支持通过在http header里指定Range来分块下载。
     * 在文件很大，可能下载超时的情况下，推荐使用分块下载。
     * */
    fun getMaterial(mediaId: String) = doGet3("get", mapOf("media_id" to mediaId))

    /**
     * 获取高清语音素材
     * 可以使用本接口获取从JSSDK的uploadVoice接口上传的临时语音素材，格式为speex，16K采样率。该音频比上文的临时素材获取接口（格式为amr，
     * 8K采样率）更加清晰，适合用作语音识别等对音质要求较高的业务。
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90255
     * */
    fun getHDVoice(mediaId: String) = doGet3("get/jssdk", mapOf("media_id" to mediaId))
}