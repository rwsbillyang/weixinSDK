package com.github.rwsbillyang.wxSDK.common



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
