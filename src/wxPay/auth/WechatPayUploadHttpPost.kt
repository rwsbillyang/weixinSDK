/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 13:36
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

package com.github.rwsbillyang.wxSDK.wxPay.auth


import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import java.io.InputStream
import java.net.URI
import java.net.URLConnection

/**
 *
 * https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient
 * */
class WechatPayUploadHttpPost private constructor(uri: URI, val meta: String) : HttpPost(uri) {

    class Builder(private val uri: URI?) {
        private var fileName: String? = null
        private var fileSha256: String? = null
        private var fileInputStream: InputStream? = null
        private var fileContentType: ContentType? = null
        fun withImage(fileName: String?, fileSha256: String?, inputStream: InputStream?): Builder {
            this.fileName = fileName
            this.fileSha256 = fileSha256
            fileInputStream = inputStream
            val mimeType = URLConnection.guessContentTypeFromName(fileName)
            if (mimeType == null) {
                // guess this is a video uploading
                fileContentType = ContentType.APPLICATION_OCTET_STREAM
            } else {
                fileContentType = ContentType.create(mimeType)
            }
            return this
        }

        fun build(): WechatPayUploadHttpPost {
            require(!(fileName == null || fileSha256 == null || fileInputStream == null)) { "缺少待上传图片文件信息" }
            requireNotNull(uri) { "缺少上传图片接口URL" }
            val meta = String.format("{\"filename\":\"%s\",\"sha256\":\"%s\"}", fileName, fileSha256)
            val request = WechatPayUploadHttpPost(uri, meta)
            val entityBuilder: MultipartEntityBuilder = MultipartEntityBuilder.create()
            entityBuilder.setMode(HttpMultipartMode.RFC6532)
                .addBinaryBody("file", fileInputStream, fileContentType, fileName)
                .addTextBody("meta", meta, ContentType.APPLICATION_JSON)
            request.entity = entityBuilder.build()
            request.addHeader("Accept", ContentType.APPLICATION_JSON.toString())
            return request
        }
    }
}