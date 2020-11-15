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


import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

open class ClientWrapper{
    open val apiJson = Json {
        encodeDefaults = true
        useArrayPolymorphism = false
    }


    open val client = HttpClient(Apache) {
        install(HttpTimeout) {}
        install(JsonFeature) {
            serializer = KotlinxSerializer(apiJson)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    inline fun <reified R> doGetByUrl(url: String): R = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.get<R>(url)
        }.await()
    }

    /**
     * 返回R泛型类型结果
     * */
    inline fun <T, reified R> doPostByUrl(url: String, data: T? = null):R = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.post<R>(url) { data?.let{body = data}  }
        }.await()
    }
}
abstract class Api: ClientWrapper(){


    abstract fun url(name: String, requestParams: Map<String, String?>?, needAccessToken: Boolean = true): String

    /**
     * 返回R泛型类型结果
     * */
    inline fun <reified R> doGet(name: String, parameters: Map<String, String?>? = null): R
            = doGetByUrl(url(name, parameters))

    /**
     * 返回Map<String, Any?>类型结果
     * */
    inline fun doGet(crossinline urlFunc: () -> String): Map<String, Any?> = runBlocking {
        val url = urlFunc()
        CoroutineScope(Dispatchers.IO).async {
            client.get<Map<String, Any?>>(url)
        }.await()
    }

    /**
     * 返回R泛型类型结果
     * urlFunc 提供url的函数，如 "$base/corp/get_join_qrcode?access_token=ACCESS_TOKEN&size_type=$sizeType"
     * */
    inline fun <reified R> doGet2(crossinline urlFunc: () -> String): R
            = doGetByUrl(urlFunc())

    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun doGet3(name: String, parameters: Map<String, String?>? = null): Map<String, Any?> = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.get<Map<String, Any?>>(url(name, parameters))
        }.await()
    }



    /**
     * 返回R泛型类型结果
     * */
    inline fun <T, reified R> doPost(name: String, data: T?, parameters: Map<String, String?>? = null):R
            = doPostByUrl(url(name, parameters), data)

    /**
     * 返回R泛型类型结果
     * */
    inline fun <T, reified R> doPost(data: T?, crossinline urlFunc: () -> String):R
            = doPostByUrl(urlFunc(), data)


    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun <T> doPost3(name: String, data: T?, parameters: Map<String, String?>? = null) = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.post<Map<String, Any?>>(url(name, parameters)) { data?.let{body = data}  }
        }.await()
    }
    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun <T> doPost4(data: T?, urlFunc: () -> String):Map<String, Any?> = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val url = urlFunc()
            client.post<Map<String, Any?>>(url) { data?.let{body = data}  }
        }
    }


    inline fun <reified R> doUpload(name: String, filePath: String,
                                    parameters: Map<String, String?>? = null,
                                    formData: Map<String, String>? = null) :R= runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.post<R>(url(name, parameters)) {
                val file = File(filePath)
                body = MultiPartFormDataContent(formData {
                    formData?.forEach{ append(it.key, it.value)}
                    //append("media",filePath, ContentType.Video, file.length())
                    appendInput("media", size = file.length()) {
                        FileInputStream(file).asInput()
                    }
                })
            }
        }.await()
    }

    fun  doUpload3(name: String, filePath: String, parameters: Map<String, String?>? = null,formData: Map<String, String>? = null) = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.post<Map<String, Any?>>(url(name, parameters)) {
                val file = File(filePath)
                body = MultiPartFormDataContent(formData {
                    formData?.forEach{ append(it.key, it.value)}
                    appendInput("media", size = file.length()) {
                        FileInputStream(file).asInput()
                    }
                })
            }
        }.await()
    }




}

