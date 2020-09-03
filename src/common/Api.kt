package com.github.rwsbillyang.wxSDK.common

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
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


 val client = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = KotlinxSerializer(apiJson)
    }
}

 val xmlClient = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = JacksonSerializer(jackson = XmlMapper().registerModule(KotlinModule()))
        accept(ContentType.Application.Xml)
    }
}


interface IApi{
    fun url(name: String, requestParams: Map<String, Any?>?, needAccessToken: Boolean = true): String

    fun doGet(name: String, parameters: Map<String, Any?>?):Map<String, Any?> = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.get<Map<String, Any?>>(url(name, parameters))
        }.await()
    }

    /**
     * urlFunc 提供url的函数，如 "$base/corp/get_join_qrcode?access_token=ACCESS_TOKEN&size_type=$sizeType"
     * */
    fun <T> doGet(data: T, urlFunc: () -> String): Map<String, Any?> = runBlocking {
        val url = urlFunc()
        CoroutineScope(Dispatchers.IO).async {
            client.get<Map<String, Any?>>( url)
        }.await()
    }



    fun doPost(name: String, paraBody: Any?, parameters: Map<String, Any?>? = null) = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            if(paraBody != null)
                client.post(url(name, parameters)){ body = paraBody }
            else
                client.post<Map<String, Any?>>(url(name, parameters))
        }.await()
    }


    /**
     * name 和 urlFunc不能同时为空
     * */
    fun <T> doPost(paraBody: T?, name: String?  = null, urlFunc:(() -> String)? = null ) = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val url = urlFunc?.let { it() } ?: url(name!!, null)
            if (paraBody != null)
                client.post<Map<String, Any?>>(url) { body = paraBody }
            else
                client.post<Map<String, Any?>>(url)
        }
    }


    fun doUpload(name: String ,filePath: String, parameters: Map<String, Any?>? = null)= runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            client.post<Map<String, Any?>>(url(name, parameters)){
                val file = File(filePath)
                body = MultiPartFormDataContent(formData{
                    appendInput("media", size = file.length()) {
                        FileInputStream(file).asInput()
                    }
                })
            }
        }.await()
    }
}

