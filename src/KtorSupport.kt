package com.github.rwsbillyang.wxSDK

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.rwsbillyang.wxSDK.common.aes.AesException
import com.github.rwsbillyang.wxSDK.common.aes.SignUtil
import com.github.rwsbillyang.wxSDK.officialAccount.*
import com.github.rwsbillyang.wxSDK.work._WORK
import com.github.rwsbillyang.wxSDK.work.WorkConfiguration
import com.github.rwsbillyang.wxSDK.work.WorkContext
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.async
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


class OfficialAccountFeature {
    // Body of the feature
//    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
//        //  context.call.response.header(name, value)
//    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, OAConfiguration, OfficialAccountFeature> {
        // Creates a unique key for the feature.
        override val key = AttributeKey<OfficialAccountFeature>("OfficialAccountFeature")

        // Code to execute when installing the feature.
        override fun install(pipeline: ApplicationCallPipeline, configure: OAConfiguration.() -> Unit): OfficialAccountFeature {
            OfficialAccount.config(configure)
            
            // 首先创建一个Configuration，然后对其apply：执行install的configure代码块，
            //val configuration = OAConfiguration().apply(configure)

            // Install an interceptor that will be run on each call and call feature instance
//            pipeline.intercept(ApplicationCallPipeline.Setup) {
//                feature.intercept(this)
//            }

            return OfficialAccountFeature()
        }
    }
}



fun Routing.officialAccountApi(path: String = OfficialAccount._OA.callbackPath) {
    val log = LoggerFactory.getLogger("officialAccountApi")

    route(path) {
        /**
         * 公众号后台中的开发者->配置开发服务器URL，必须调用此函数进行处理GET请求
         *
         * https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
         * https://developers.weixin.qq.com/doc/offiaccount/Getting_Started/Getting_Started_Guide.html
         *
         * 第二步：验证消息的确来自微信服务器
         * 开发者提交信息后，微信服务器将发送GET请求到填写的服务器地址URL上，GET请求携带参数如下表所示：
         *
         * signature	微信加密签名，signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
         * timestamp	时间戳
         * nonce	随机数
         * echostr	随机字符串
         *
         * 开发者通过检验signature对请求进行校验（下面有校验方式）。若确认此次GET请求来自微信服务器，请原样返回echostr参
         * 数内容，则接入生效，成为开发者成功，否则接入失败。加密/校验流程如下：
         * 1）将token、timestamp、nonce三个参数进行字典序排序
         * 2）将三个参数字符串拼接成一个字符串进行sha1加密
         * 3）开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
         * */
        get {
            val signature = call.request.queryParameters["signature"]
            val timestamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val echostr = call.request.queryParameters["echostr"]

            val token = OfficialAccount._OA.token

            if (StringUtils.isAnyBlank(token, signature, timestamp, nonce,echostr)) {
                log.warn("invalid parameters: token=$token, signature=$signature, timestamp=$timestamp, nonce=$nonce,echostr=$echostr")
            } else {
                if (!SignUtil.checkSignature(token, signature!!, timestamp!!, nonce!!)) {
                    log.warn("fail to check signature")
                }
            }
            call.respondText(echostr?:"", ContentType.Text.Plain, HttpStatusCode.OK)
        }

        /**
         * 公众号后台中的开发者->配置开发服务器URL，必须调用此函数进行处理POST请求
         *
         * 当普通微信用户向公众账号发消息时，微信服务器将POST消息的XML数据包到开发者填写的URL上。
         *
         * 假如服务器无法保证在五秒内处理并回复，可以直接回复空串，微信服务器不会对此作任何处理，并且不会发起重试。
         *  1） 被动回复消息，即发送被动响应消息，不同于客服消息接口
         *  2） 它其实并不是一种接口，而是对微信服务器发过来消息的一次回复
         *  3） 收到粉丝消息后不想或者不能5秒内回复时，需回复“success”字符串（下文详细介绍）
         *  4） 客服接口在满足一定条件下随时调用
         *
         *   假如服务器无法保证在五秒内处理回复，则必须回复“success”（推荐方式）或者“”（空串），否则微信后台会发起三次重试。
         *   三次重试后，依旧没有及时回复任何内容，系统自动在粉丝会话界面出现错误提示“该公众号暂时无法提供服务，请稍后再试”。
         *
         *
         *   启用加解密功能（即选择兼容模式或安全模式）后，公众平台服务器在向公众账号服务器配置地址（可在“开发者中心”修改）推送消息时，
         *   URL将新增加两个参数（加密类型和消息体签名），并以此来体现新功能。加密算法采用AES。
         *   1、在接收已授权公众号消息和事件的 URL 中，增加 2 个参数：encrypt_type（加密类型，为 aes）和 msg_signature（消息体签名，
         *   用于验证消息体的正确性）（此前已有 2 个参数，为时间戳 timestamp，随机数 nonce）
         *   2、postdata 中的 XML 体，将使用第三方平台申请时的接收消息的加密 symmetric_key（也称为 EncodingAESKey）来进行加密。
         *
         *   开发者安全模式（推荐）：公众平台发送消息体的内容只含有密文，公众账号回复的消息体也为密文。但开发者通过客服接口等API调用形式向用户发送消息，则不受影响。
         * */
        post {
            val body: String = call.receiveText()

            val msgSignature = call.request.queryParameters["msg_signature"]
            val timeStamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val encryptType = call.request.queryParameters["encrypt_type"]?:"aes"

            val reXml = OfficialAccount._OA.msgHub.handleXmlMsg(body, msgSignature, timeStamp, nonce, encryptType)

            if(reXml.isNullOrBlank())
                call.respondText("success", ContentType.Text.Plain, HttpStatusCode.OK)
            else
                call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)

        }
    }
}

fun Routing.oAuthApi(
        oauthInfoPath: String = "/api/wx/oauth/info",
        notifyPath: String = "/api/wx/oauth/notify",
        notifyWebAppUrl: String = "/wx/auth",
        needUserInfo: ((String, String) -> Int)? = null,
        onGetOauthAccessToken: ((ResponseOauthAccessToken)-> Unit)? = null,
        onGetUserInfo: ((info: ResponseUserInfo) -> Unit)? = null
) {
    val log = LoggerFactory.getLogger("oAuthApi")
    val stateCache = Caffeine.newBuilder()
            .maximumSize(Long.MAX_VALUE)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(0, TimeUnit.SECONDS)
            .build<String, Boolean>()
    /**
     * 前端webapp请求该api获取appid，state等信息，然后重定向到腾讯的授权页面，用户授权之后将重定向到下面的notify
     * @param userInfo 0或1分别表示是否需要获取用户信息，优先使用前端提供的参数, 没有提供的话使用needUserInfo(host, uri)进行判断（用于从某个用户设置中获取），再没有的话则默认为0
     * @param host 跳转host，如："https：//www.example.com"
     * */
    get(oauthInfoPath){
        val userInfo = (call.request.queryParameters["userInfo"]?.toInt()?:(needUserInfo?.let { it(call.request.host(), call.request.uri) })?:0) == 1
        val host = call.request.queryParameters["host"]?:call.request.host()

        val oAuthInfo = OAuthApi.prepareOAuthInfo(host + notifyPath, userInfo)
        stateCache.put(oAuthInfo.state, userInfo)
        call.respond(oAuthInfo)
    }
    /**
     * 腾讯在用户授权之后，将调用下面的api通知code，并附带上原state。
     *
     * 第二步：通过code换取网页授权access_token，然后必要的话获取用户信息。
     * 然后将一些登录信息通知到前端（调用前端提供的url）
     *
     * 用户同意授权后, 如果用户同意授权，页面将跳转至此处的redirect_uri/?code=CODE&state=STATE。
     * code作为换取access_token的票据，每次用户授权带上的code将不一样，code只能使用一次，5分钟未被使用自动过期。
     *
     * */
    get(notifyPath){
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        if(code.isNullOrBlank() || state.isNullOrBlank()){
            //notify webapp fail
            call.respondRedirect("$notifyWebAppUrl?state=$state&code=KO&msg=nullCodeOrState", permanent = false)
        }else{
            val res = OAuthApi.getAccessToken(code)
            if(res.isOK()  && res.openId != null){

                onGetOauthAccessToken?.let { async { it.invoke(res) } }

                var url = "$notifyWebAppUrl?state=$state&code=OK&openId=${res.openId}"
                val needUserInfo = stateCache.getIfPresent(state)?:false
                stateCache.invalidate(state)
                if(needUserInfo && res.accessToken != null){
                    val resUserInfo = OAuthApi.getUserInfo(res.accessToken, res.openId)
                    if(resUserInfo.isOK())
                    {
                        if(resUserInfo.unionId != null){
                            url += "&unionId=${resUserInfo.unionId}"
                        }
                        //async save fan user info
                        onGetUserInfo?.let { async { it.invoke(resUserInfo)} }
                    }else{
                        log.warn("fail getUserInfo: $resUserInfo")
                    }
                }
                //notify webapp OK
                call.respondRedirect(url, permanent = false)
            }
        }
    }
}


class WorkFeature(config: WorkConfiguration) {
    init {
        _WORK = WorkContext(
                config.corpId,
                config.secret,
                config.token,
                config.encodingAESKey,
                config.wechatId,
                config.wechatName,
                config.callbackPath,
                config.msgHandler,
                config.eventHandler,
                config.accessToken
                //config.ticket
        )
    }

    // Body of the feature
    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        //  context.call.response.header(name, value)
    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, WorkConfiguration, WorkFeature> {
        // Creates a unique key for the feature.
        override val key = AttributeKey<WorkFeature>("WorkFeature")

        // Code to execute when installing the feature.
        override fun install(pipeline: ApplicationCallPipeline, configure: WorkConfiguration.() -> Unit): WorkFeature {

            // 首先创建一个Configuration，然后对其apply：执行install的configure代码块，
            val configuration = WorkConfiguration().apply(configure)

            // 然后用上面的Configuration实例对象，创建自己的Feature，
            val feature = WorkFeature(configuration)

            // Install an interceptor that will be run on each call and call feature instance
//            pipeline.intercept(ApplicationCallPipeline.Setup) {
//                feature.intercept(this)
//            }

            return feature
        }
    }
}



fun Routing.workApi(path: String = _WORK.callbackPath) {
    val log = LoggerFactory.getLogger("workApi")

    route(path) {
        /**
         *
         *
         * https://work.weixin.qq.com/api/doc/90000/90135/90238
         *
         * 为了能够让自建应用和企业微信进行双向通信，企业可以在应用的管理后台开启接收消息模式。
         * 开启接收消息模式的企业，需要提供可用的接收消息服务器URL（建议使用https）。
         * 开启接收消息模式后，用户在应用里发送的消息会推送给企业后台。此外，还可配置地理位置上报等事件消息，当事件触发时企业微信会把相应的数据推送到企业的后台。
         * 企业后台接收到消息后，可在回复该消息请求的响应包里带上新消息，企业微信会将该被动回复消息推送给用户。
         *
         * 设置接收消息的参数
         * 在企业的管理端后台，进入需要设置接收消息的目标应用，点击“接收消息”的“设置API接收”按钮，进入配置页面。

         * 要求填写应用的URL、Token、EncodingAESKey三个参数

         * URL是企业后台接收企业微信推送请求的访问协议和地址，支持http或https协议（为了提高安全性，建议使用https）。
         * Token可由企业任意填写，用于生成签名。
         * EncodingAESKey用于消息体的加密。
         *
         * 验证URL有效性
         * 当点击“保存”提交以上信息时，企业微信会发送一条验证消息到填写的URL，发送方法为GET。
         * 企业的接收消息服务器接收到验证请求后，需要作出正确的响应才能通过URL验证。
         *
         * 企业在获取请求时需要做Urldecode处理，否则可能会验证不成功
         * 你可以访问接口调试工具进行调试，依次选择 建立连接 > 接收消息。
         *
         * 假设接收消息地址设置为：http://api.3dept.com/，企业微信将向该地址发送如下验证请求：
         *
         * 请求方式：GET
         * 请求地址：http://api.3dept.com/?msg_signature=ASDFQWEXZCVAQFASDFASDFSS&timestamp=13500001234&nonce=123412323&echostr=ENCRYPT_STR
         * 参数说明
         * 参数	必须	说明
         * msg_signature	是	企业微信加密签名，msg_signature结合了企业填写的token、请求中的timestamp、nonce参数、加密的消息体
         * timestamp	是	时间戳
         * nonce	是	随机数
         * echostr	是	加密的字符串。需要解密得到消息内容明文，解密后有random、msg_len、msg、receiveid四个字段，其中msg即为消息内容明文
         * 企业后台收到请求后，需要做如下操作：
         *
         * 对收到的请求做Urldecode处理
         * 通过参数msg_signature对请求进行校验，确认调用者的合法性。
         * 解密echostr参数得到消息内容(即msg字段)
         * 在1秒内响应GET请求，响应内容为上一步得到的明文消息内容(不能加引号，不能带bom头，不能带换行符)
         * 以上2~3步骤可以直接使用验证URL函数一步到位。
         * 之后接入验证生效，接收消息开启成功。
         * */
        get {
            val signature = call.request.queryParameters["msg_signature"]
            val timestamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val echostr = call.request.queryParameters["echostr"]

            val token = _WORK.token

            if (StringUtils.isAnyBlank(token, signature, timestamp, nonce,echostr)) {
                log.warn("invalid parameters: token=$token, signature=$signature, timestamp=$timestamp, nonce=$nonce, echostr=$echostr")
                call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
            } else {
                try{
                    val str = _WORK.wxBizMsgCrypt.verifyUrl(signature!!,timestamp!!,nonce!!,echostr!!)
                    call.respondText(str, ContentType.Text.Plain, HttpStatusCode.OK)
                }catch (e: AesException){
                    log.warn("AesException: ${e.message}")
                    call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
                }
            }
        }

        /**
         * 开启接收消息模式后，企业微信会将消息发送给企业填写的URL，企业后台需要做正确的响应。
         *
         * https://work.weixin.qq.com/api/doc/90000/90135/90238
         *
         * 接收消息协议的说明
         * 企业微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。如果企业在调试中，发现成员无法收到被动回复的消息，可以检查是否消息处理超时。
         * 当接收成功后，http头部返回200表示接收ok，其他错误码企业微信后台会一律当做失败并发起重试。
         * 关于重试的消息排重，有msgid的消息推荐使用msgid排重。事件类型消息推荐使用FromUserName + CreateTime排重。
         * 假如企业无法保证在五秒内处理并回复，或者不想回复任何内容，可以直接返回200（即以空串为返回包）。企业后续可以使用主动发消息接口进行异步回复。
         *
         * 接收消息请求的说明
         * 假设企业的接收消息的URL设置为http://api.3dept.com。
         * 请求方式：POST
         * 请求地址 ：http://api.3dept.com/?msg_signature=ASDFQWEXZCVAQFASDFASDFSS&timestamp=13500001234&nonce=123412323
         * 接收数据格式 ：
         * <xml>
         * <ToUserName><![CDATA[toUser]]></ToUserName>
         * <AgentID><![CDATA[toAgentID]]></AgentID>
         * <Encrypt><![CDATA[msg_encrypt]]></Encrypt>
         * </xml>
         * */
        post {
            val body: String = call.receiveText()

            val msgSignature = call.request.queryParameters["msg_signature"]
            val timeStamp = call.request.queryParameters["timeStamp"]
            val nonce = call.request.queryParameters["nonce"]
            val encryptType = call.request.queryParameters["encrypt_type"]?:"aes"

            val reXml = _WORK.msgHub.handleXmlMsg(body, msgSignature, timeStamp, nonce, encryptType)

            if(reXml.isNullOrBlank())
                call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
            else
                call.respondText(reXml, ContentType.Text.Xml, HttpStatusCode.OK)
        }
    }

}

