package com.github.rwsbillyang.wxSDK.work


import com.github.rwsbillyang.wxSDK.common.ResponseCallbackIps
import com.github.rwsbillyang.wxSDK.common.WxApi
import com.github.rwsbillyang.wxSDK.common.client
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


abstract class WorkBaseApi: WxApi {
    override val base = "https://qyapi.weixin.qq.com/cgi-bin"
    override fun accessToken() = _WORK.accessToken.get()

    /**
     * 企业微信在回调企业指定的URL时，是通过特定的IP发送出去的。如果企业需要做防火墙配置，那么可以通过这个接口获取到所有相关的IP段。
     * IP段有变更可能，当IP段变更时，新旧IP段会同时保留一段时间。建议企业每天定时拉取IP段，更新防火墙设置，避免因IP段变更导致网络不通。
     *
     * 若调用失败，会返回errcode及errmsg（判断是否调用失败，根据errcode存在并且值非0）
     * */
    suspend fun getCallbackIp(): ResponseCallbackIps
            = client.get("$base/getcallbackip?access_token=${accessToken()}")
}


@Serializable
class QyBaseResponse(
    @SerialName("errcode")
    val errCode: Int?,
    @SerialName("errmsg")
    val errMsg: String?
){
    fun isOK() = (errCode != null && errCode == 0)
}