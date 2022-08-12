/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 15:09
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

package com.github.rwsbillyang.wxSDK

import com.github.rwsbillyang.ktorKit.apiJson.KHttpClient



abstract class Api: KHttpClient()  {

    abstract fun url(name: String, requestParams: Map<String, String?>?=null, needAccessToken: Boolean = true): String

    /**
     * 返回R泛型类型结果
     * */
    inline fun <reified R> doGet(name: String, parameters: Map<String, String?>? = null): R =
        get(url(name, parameters))

    /**
     * 返回HttpResponse，需自行解析
     * */
    fun doGetRaw(name: String, parameters: Map<String, String?>? = null) =
        getByRaw(url(name, parameters))


    /**
     * 返回R泛型类型结果
     * */
    inline fun <reified T,  reified R> doPost(name: String, data: T?, parameters: Map<String, String?>? = null):R =
        post(url(name, parameters), data)

    /**
     * 返回HttpResponse，需自行解析
     * */
    inline fun <reified T> doPostRaw(name: String, data: T?, parameters: Map<String, String?>? = null) =
        postByRaw(url(name, parameters), data)

    inline fun <reified R>  doUpload(
        name: String, filePath: String,
        parameters: Map<String, String?>? = null,
        formData: Map<String, String>? = null
    ):R = upload(url(name, parameters),filePath,formData)

   fun doUploadRaw(
        name: String, filePath: String,
        parameters: Map<String, String?>? = null,
        formData: Map<String, String>? = null
    ) = uploadByRaw(url(name, parameters),filePath,formData)

}

