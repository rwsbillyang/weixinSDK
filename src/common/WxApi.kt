package com.github.rwsbillyang.wxSDK.common


abstract class WxApi : Api() {
    /**
     * API前面的公共部分，后面无斜杠，如：https://api.weixin.qq.com/cgi-bin
     * 公众号、企业微信等API在不同的域名下，故base不同
     *
     * 如https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN中前面的是所有API的共同部分
     * */
    abstract val base: String

    /**
     * API中的组，前后都无斜杠，如menu，意味着 各MenuAPI
     * */
    abstract val group: String

    /**
     * 由各个公众号、企业微信中各个不同模块提供
     * */
    abstract fun accessToken(): String?

    /**
     * 拼接API
     * @param name API的名称 如create： 如"https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN"
     * @param requestParams 请求参数
     * @param needAccessToken 是否需要accessToken
     * */
    override fun url(name: String, requestParams: Map<String, String?>?, needAccessToken: Boolean): String {
        val params = requestParams?.entries?.filter { it.value != null }?.joinToString("&") { "${it.key}=${it.value}" }
        return if (needAccessToken)
        {
            if (params.isNullOrBlank())
                "$base/$group/$name?access_token=${accessToken()}"
            else
                "$base/$group/$name?access_token=${accessToken()}&$params"
        }else {
            if (params.isNullOrBlank())
                "$base/$group/$name"
            else
                "$base/$group/$name?$params"
        }

    }
}

//
//
///**
// * 根据提供的参数自动拼凑一个带请求参数的GET请求url
// * */
//inline fun url(base:String, group: String, name: String, accessToken: String, requestParams: Map<String, String?>?): String{
//    val params = requestParams?.entries?.filter { it.value != null }?.joinToString("&") { "${it.key}=${it.value}" }?:""
//    return  "$base/$group/$name?access_token=$accessToken&$params"
//}
//
///**
// * 返回数据类型为T
// * */
//inline fun <reified R> get(base:String, group: String, name: String,  accessToken: String, parameters: Map<String, String?>? = null) = runBlocking {
//    CoroutineScope(Dispatchers.IO).async {
//        client.get<R>(url(base, group,name, accessToken, parameters))
//    }.await()
//}
//
///**
// * 请求数据类型为T， 返回数据类型为R，指定url
// * urlFunc 提供url的函数，如 "$base/corp/get_join_qrcode?access_token=ACCESS_TOKEN&size_type=$sizeType"
// * */
//inline fun <reified T, reified R> post(data: T, url: String) = runBlocking {
//    CoroutineScope(Dispatchers.IO).async {
//        client.post<R>(url){ data?.let { body = it  }}
//    }.await()
//}
//
///**
// * 请求数据类型为T， 返回数据类型为R
// * */
//inline fun <reified T, reified R> post(base:String, group: String, name: String, accessToken: String,
//                                        data: T?, parameters: Map<String, String?>? = null)
//        = runBlocking {
//    CoroutineScope(Dispatchers.IO).async {
//        if(data != null)
//            client.post<R>(url(base, group,name, accessToken, parameters)){ body = data }
//        else
//            client.post<R>(url(base, group,name, accessToken,  parameters))
//    }.await()
//}