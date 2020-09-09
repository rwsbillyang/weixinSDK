package com.github.rwsbillyang.wxSDK.common


import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream


val apiJson = Json {
    encodeDefaults = true
    useArrayPolymorphism = false
    //serialModule = serializersModuleOf(mapOf())
}


val client = HttpClient(Apache) {
    install(HttpTimeout) {}
    install(JsonFeature) {
        serializer = KotlinxSerializer(apiJson)
    }
}


// val xmlClient = HttpClient(CIO) {
//    install(JsonFeature) {
//        serializer = JacksonSerializer(jackson = XmlMapper().registerModule(KotlinModule()))
//        accept(ContentType.Application.Xml)
//    }
//}


abstract class Api {
    abstract fun url(name: String, requestParams: Map<String, String?>?, needAccessToken: Boolean = true): String

    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun doGet(name: String, parameters: Map<String, String?>? = null): Map<String, Any?> = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.get<Map<String, Any?>>(url(name, parameters))
        }.await()
    }

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
     * */
    inline fun <reified R> doGet2(name: String, parameters: Map<String, String?>? = null): R = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.get<R>(url(name, parameters))
        }.await()
    }
    /**
     * 返回R泛型类型结果
     * urlFunc 提供url的函数，如 "$base/corp/get_join_qrcode?access_token=ACCESS_TOKEN&size_type=$sizeType"
     * */
    inline fun <reified R> doGet2(crossinline urlFunc: () -> String): R = runBlocking {
        val url = urlFunc()
        CoroutineScope(Dispatchers.IO).async {
            client.get<R>(url)
        }.await()
    }



    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun <T> doPost(name: String, data: T?, parameters: Map<String, String?>? = null) = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.post<Map<String, Any?>>(url(name, parameters)) { data?.let{body = data}  }
        }.await()
    }
    /**
     * 返回Map<String, Any?>类型结果
     * */
    fun <T> doPost(data: T?, urlFunc: () -> String):Map<String, Any?> = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val url = urlFunc()
            client.post<Map<String, Any?>>(url) { data?.let{body = data}  }
        }
    }

    /**
     * 返回R泛型类型结果
     * */
    inline fun <T, reified R> doPost2(name: String, data: T?, parameters: Map<String, String?>? = null):R = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.post<R>(url(name, parameters)) { data?.let{body = data}  }
        }.await()
    }

    /**
     * 返回R泛型类型结果
     * */
    inline fun <T, reified R> doPost3(data: T?, crossinline urlFunc: () -> String):R = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.post<R>(urlFunc()) { data?.let{body = data}  }
        }
    }


    fun  doUpload(name: String, filePath: String, parameters: Map<String, String?>? = null,formData: Map<String, String>? = null) = runBlocking {
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



    inline fun <reified R> doUpload2(name: String, filePath: String,
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
}

