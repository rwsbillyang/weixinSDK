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
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream


open class KtorHttpClient {
    companion object {
        val apiJson = Json {
            encodeDefaults = false //变为false 会影响某些非空默认值，如模板消息颜色
            useArrayPolymorphism = false
            ignoreUnknownKeys = true
        }

        var logLevel = LogLevel.INFO //使用者可直接配置

        val DefaultClient = HttpClient(CIO) {
            install(HttpTimeout) {}
            install(JsonFeature) {
                serializer = KotlinxSerializer(apiJson)
                accept(ContentType.Application.Json)
                //OAuthApi.getAccessToken 返回的是：Content-Type: text/plain
                accept(ContentType.Text.Any) //https://github.com/ktorio/ktor/issues/772
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = logLevel
            }
            defaultRequest { // this: HttpRequestBuilder ->
                contentType(ContentType.Application.Json)
            }

            //https://ktor.io/docs/http-client-engines.html#jvm-and-android
            engine {
                maxConnectionsCount = 20480

                endpoint {
                    /**
                     * Maximum number of requests for a specific endpoint route.
                     */
                    maxConnectionsPerRoute = 10240

                    /**
                     * Max size of scheduled requests per connection(pipeline queue size).
                     */
                    pipelineMaxSize = 20

                    /**
                     * Max number of milliseconds to keep idle connection alive.
                     */
                    keepAliveTime = 5000

                    /**
                     * Number of milliseconds to wait trying to connect to the server.
                     */
                    connectTimeout = 8000

                    /**
                     * Maximum number of attempts for retrying a connection.
                     */
                    connectAttempts = 3
                }
            }
        }
    }

    /**
     * 多数情况下使用默认的client即可,亦可定制自己的client
     * */
    open val client = DefaultClient

    inline fun <reified R> doGetByUrl(url: String): R = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.get(url)
        }
    }

    /**
     * 返回R泛型类型结果
     * */
    inline fun <T, reified R> doPostByUrl(url: String, data: T? = null): R = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.post(url) {
                data?.let { body = data }
            }
        }
    }
}

abstract class Api : KtorHttpClient() {
    abstract fun url(name: String, requestParams: Map<String, String?>?, needAccessToken: Boolean = true): String

    /**
     * 返回R泛型类型结果
     * */
    inline fun <reified R> doGet(name: String, parameters: Map<String, String?>? = null): R =
        doGetByUrl(url(name, parameters))

    /**
     * 返回Map<String, Any?>类型结果
     * */
    inline fun doGet(crossinline urlFunc: () -> String): Map<String, Any?> = runBlocking {
        val url = urlFunc()
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.get(url)
        }
    }

    /**
     * 返回R泛型类型结果
     * urlFunc 提供url的函数，如 "$base/corp/get_join_qrcode?access_token=ACCESS_TOKEN&size_type=$sizeType"
     * */
    inline fun <reified R> doGet2(crossinline urlFunc: () -> String): R = doGetByUrl(urlFunc())

    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun doGet3(name: String, parameters: Map<String, String?>? = null): Map<String, Any?> = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.get(url(name, parameters))
        }
    }


    /**
     * 返回R泛型类型结果
     * */
    inline fun <T, reified R> doPost(name: String, data: T?, parameters: Map<String, String?>? = null): R =
        doPostByUrl(url(name, parameters), data)

    /**
     * 返回R泛型类型结果
     * */
    inline fun <T, reified R> doPost(data: T?, crossinline urlFunc: () -> String): R = doPostByUrl(urlFunc(), data)


    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun <T> doPost3(name: String, data: T?, parameters: Map<String, String?>? = null) = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.post<Map<String, Any?>>(url(name, parameters)) {
                data?.let { body = data }
            }
        }
    }

    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun <T> doPost4(data: T?, urlFunc: () -> String): Map<String, Any?> = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val url = urlFunc()
            client.post(url) {
                data?.let { body = data }
            }
        }
    }


    inline fun <reified R> doUpload(
        name: String, filePath: String,
        parameters: Map<String, String?>? = null,
        formData: Map<String, String>? = null
    ): R = runBlocking {
        //append("media",filePath, ContentType.Video, file.length())
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.post(url(name, parameters)) {
                val file = File(filePath)
                body = MultiPartFormDataContent(formData {
                    formData?.forEach { append(it.key, it.value) }
                    //append("media",filePath, ContentType.Video, file.length())
                    appendInput("media", size = file.length()) {
                        FileInputStream(file).asInput()
                    }
                })
            }
        }
    }

    fun doUpload3(
        name: String,
        filePath: String,
        parameters: Map<String, String?>? = null,
        formData: Map<String, String>? = null
    ) = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.post<Map<String, Any?>>(url(name, parameters)) {
                val file = File(filePath)
                body = MultiPartFormDataContent(formData {
                    formData?.forEach { append(it.key, it.value) }
                    appendInput("media", size = file.length()) {
                        FileInputStream(file).asInput()
                    }
                })
            }
        }
    }


    /**
     * 根据url下载文件，保存到filepath中
     *
     * @param url
     * @param filepath such as: ./qrcode/${appId}
     * @param filename: such as: abc.jpg
     * @return
     */
    fun download(url: String, filepath: String,filename: String) = runBlocking {
        val response: HttpResponse = client.get(url)
        if (response.status.isSuccess()) {
            val content = ByteArrayContent(response.readBytes())
            val path = File(filepath)
            if(!path.exists()){
                path.mkdirs()
            }
            val file = File("$filepath/$filename")
            file.writeBytes(content.bytes())
            true
        }else{
            println("fail to download from $url, status=${response.status.value}")
            false
        }
    }

}

