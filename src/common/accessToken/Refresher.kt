package com.github.rwsbillyang.wxSDK.common.accessToken


import com.github.rwsbillyang.wxSDK.common.*
import io.ktor.client.request.get
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 刷新器，刷新后得到某个值
 * */
interface IRefresher{
    /**
     * 刷新后返回新值
     * */
    fun refresh(): String
}
/**
 * 提供远程url地址
 * */
interface IUrlProvider
{
    fun url(): String
}
/**
 *  默认的的请求刷新器，如刷新获取 accessToken，ticket等
 *
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html
 *  https://work.weixin.qq.com/api/doc/90000/90135/91039
 *
 *  @param urlProvider 提供刷新的url
 *  @param key 提取结果的key 如： access_token，ticket 等
 * */
open class Refresher(private val urlProvider: IUrlProvider, private val key: String): IRefresher
{
    companion object {
        private val log: Logger = LoggerFactory.getLogger("Refresher")
    }
    val wrapper = ClientWrapper()

    /**
     * 向远程发出请求，获取最新值然后返回
     */
    override  fun refresh(): String {
        val url = urlProvider.url()
        log.debug("to refresh for key=$key...,url=$url")

        return runBlocking {
            val text: String = wrapper.client.get<HttpResponse>(url).readText()
            log.debug("got text: $text")
            val jsonElement = wrapper.apiJson.parseToJsonElement(text)
            if(jsonElement is JsonObject){
                val valueElement = jsonElement[key]
                if(valueElement == null || valueElement is JsonNull)
                {
                    log.error("fail refresh key=$key, because: $text")
                    throw WxException("fail refresh key=$key")
                }else{
                    val value = valueElement.jsonPrimitive.content
                    log.debug("got value=$value for key=$key")
                    value
                }
            }else{
                throw WxException("fail refresh key=$key, not a jsonObject: $text")
            }
        }
    }
}


/**
 *  请求刷新accessToken，如用于公众号、企业微信等获取accessToken
 * */
class AccessTokenRefresher(urlProvider: IUrlProvider): Refresher(urlProvider,"access_token")

/**
 * 请求刷新Ticket，如用于公众号获取jsTicket
 * */
class TicketRefresher(urlProvider: IUrlProvider): Refresher(urlProvider,"ticket")
