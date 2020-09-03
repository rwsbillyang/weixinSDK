package com.github.rwsbillyang.wxSDK.common

import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking


interface WxApi: IApi{
    /**
     * API前面的公共部分，后面无斜杠，如：https://api.weixin.qq.com/cgi-bin
     * 公众号、企业微信等API在不同的域名下，故base不同
     *
     * 如https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN中前面的是所有API的共同部分
     * */
    val base: String
    /**
     * API中的组，前后都无斜杠，如menu，意味着 各MenuAPI
     * */
    val group: String

    /**
     * 由各个公众号、企业微信中各个不同模块提供
     * */
    fun accessToken(): String?

    /**
     * 拼接API
     * @param name API的名称 如create： 如"https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN"
     * @param requestParams 请求参数
     * @param needAccessToken 是否需要accessToken
     * */
    override fun url(name: String, requestParams: Map<String, Any?>?, needAccessToken: Boolean): String{
        val params = requestParams?.entries?.filter { it.value != null }?.joinToString("&") { "${it.key}=${it.value}" }?:""
        return  "$base/$group/$name?access_token=${accessToken()}&$params"
    }
}



/**
 * 根据提供的参数自动拼凑一个带请求参数的GET请求url
 * */
fun url(base:String, group: String, name: String, requestParams: Map<String, Any?>?): String{
    val params = requestParams?.entries?.filter { it.value != null }?.joinToString("&") { "${it.key}=${it.value}" }?:""
    return  "$base/$group/$name?access_token=ACCESS_TOKEN&$params"
}

/**
 * 返回数据类型为T
 * */
inline fun <reified T> get(base:String, group: String, name: String, parameters: Map<String, Any?>?) = runBlocking {
    CoroutineScope(Dispatchers.IO).async {
        client.get<T>(url(base, group,name, parameters))
    }.await()
}

/**
 * 请求数据类型为T1， 返回数据类型为T2，指定url
 * urlFunc 提供url的函数，如 "$base/corp/get_join_qrcode?access_token=ACCESS_TOKEN&size_type=$sizeType"
 * */
inline fun <reified T1, reified T2> get(data: T1, url: String) = runBlocking {
    CoroutineScope(Dispatchers.IO).async {
        client.get<T2>(url)
    }.await()
}

/**
 * 请求数据类型为T1， 返回数据类型为T2
 * */
inline fun <reified T1, reified T2> post(base:String, group: String, name: String, paraBody: T1?, parameters: Map<String, Any?>? = null) = runBlocking {
    CoroutineScope(Dispatchers.IO).async {
        if(paraBody != null)
            client.post(url(base, group,name, parameters)){ body = paraBody }
        else
            client.post<T2>(url(base, group,name, parameters))
    }.await()
}