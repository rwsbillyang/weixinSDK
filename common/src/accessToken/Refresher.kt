package com.github.rwsbillyang.wxSDK.accessToken



import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.ktorKit.client.DefaultClient
import com.github.rwsbillyang.wxSDK.WxException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
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
 *  默认的的请求刷新器，如刷新获取 accessToken，ticket等
 *
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html
 *  https://work.weixin.qq.com/api/doc/90000/90135/91039
 *  https://work.weixin.qq.com/api/doc/90001/90142/90593
 *
 *  @param key 默认解析的值 如：access_token，ticket 等
 *  @param url 提供刷新的url，拥有比urlBlock更高的优先级，不提供时使用urlBlock获取url
 *  @param urlBlock 返回url的函数
 * */
open class Refresher(
    private val key: String,
    private val url: String? = null,
    private val urlBlock: (() -> String)? = null): IRefresher
{
    companion object {
        private val log: Logger = LoggerFactory.getLogger("Refresher")
    }

    init {
        require(url != null || urlBlock != null){
            "one of url or urlBlock should not null"
        }
    }

    /**
     * 向远程发出请求，获取最新值然后返回
     */
    override  fun refresh(): String {
        return runBlocking {
            val text: String = getResponse().bodyAsText()

           val str = ApiJson.clientApiJson
                .parseToJsonElement(text)
                .jsonObject[key]?.jsonPrimitive?.content
            if(str == null){
                log.warn("fail refresh key=$key, not a jsonObject: $text");
                throw WxException("fail refresh key=$key, not a jsonObject: $text")
            }else str
        }
    }
    open fun url() = url?: urlBlock?.invoke()?: throw Throwable("not provide url")
    open suspend fun getResponse() = DefaultClient.get(url())
}


/**
 * post请求所用data，使用实时数据
 * @param key 用于从请求结果中解析提取它的值
 * @param block 用于构建请求体，如：{ setBody(body)}
 * @param url 请求url
 * @param urlBlock 构造url的函数，与url二者选其一
 * */
open class PostRefresher(
    key: String,
    //private val postDataBlock: ()->T?,
    private val block: HttpRequestBuilder.() -> Unit = {},
    url: String? = null,
    urlBlock: (() -> String)? = null):Refresher(key,url, urlBlock)
{
    override suspend fun getResponse() = DefaultClient.post(url(), block)
    //{ setBody(postDataBlock())}
}

/**
 *  请求刷新accessToken，如用于公众号、企业微信等获取accessToken
 * */
class AccessTokenRefresher(url: String): Refresher("access_token",url)

/**
 * 请求刷新Ticket，如用于公众号获取jsTicket
 * */
class TicketRefresher(url: () -> String): Refresher("ticket", null, url)
