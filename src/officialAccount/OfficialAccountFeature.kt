package com.github.rwsbillyang.wxSDK.officialAccount


import com.github.rwsbillyang.wxSDK.common.aes.SignUtil
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory


class OfficialAccountFeature(config: OAConfiguration) {
    init {
        OA = OAContext(
            config.appId,
            config.secret,
            config.token,
            config.encodingAESKey,
            config.wechatId,
            config.wechatName,
            config.msgHandler,
            config.eventHandler,
            config.accessToken,
            config.ticket
        )
    }

    // Body of the feature
    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
      //  context.call.response.header(name, value)
    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, OAConfiguration, OfficialAccountFeature> {
        // Creates a unique key for the feature.
        override val key = AttributeKey<OfficialAccountFeature>("OfficialAccountFeature")

        // Code to execute when installing the feature.
        override fun install(pipeline: ApplicationCallPipeline, configure: OAConfiguration.() -> Unit): OfficialAccountFeature {

            // 首先创建一个Configuration，然后对其apply：执行install的configure代码块，
            val configuration = OAConfiguration().apply(configure)

            // 然后用上面的Configuration实例对象，创建自己的Feature，
            val feature = OfficialAccountFeature(configuration)

            // Install an interceptor that will be run on each call and call feature instance
//            pipeline.intercept(ApplicationCallPipeline.Setup) {
//                feature.intercept(this)
//            }

            return feature
        }
    }
}



fun Routing.officialAccountApi(path: String = "/weixin/oa") {
    val log = LoggerFactory.getLogger("officialAccountApi")

    route(path) {
        /**
         * 公众号后台中的开发者->配置开发服务器URL，必须调用此函数进行处理GET请求
         *
         * https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
         * https://developers.weixin.qq.com/doc/offiaccount/Getting_Started/Getting_Started_Guide.html
         * 第二步：验证消息的确来自微信服务器
         * 开发者提交信息后，微信服务器将发送GET请求到填写的服务器地址URL上，GET请求携带参数如下表所示：
         *
         * signature	微信加密签名，signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
         * timestamp	时间戳
         * nonce	随机数
         * echostr	随机字符串
         *
         * 开发者通过检验signature对请求进行校验（下面有校验方式）。若确认此次GET请求来自微信服务器，
         * 请原样返回echostr参数内容，则接入生效，成为开发者成功，否则接入失败。
         * */
        get {
            val signature = call.request.queryParameters["signature"]
            val timestamp = call.request.queryParameters["timestamp"]
            val nonce = call.request.queryParameters["nonce"]
            val echostr = call.request.queryParameters["echostr"] ?: ""

            val token = OA.token

            if (StringUtils.isAnyBlank(token, signature, timestamp, nonce)) {
                log.warn("invalid parameters: token=$token, signature=$signature, timestamp=$timestamp, nonce=$nonce")
            } else {
                if (!SignUtil.checkSignature(token, signature!!, timestamp!!, nonce!!)) {
                    log.warn("fail to check signature")
                }
            }
            call.respondText(echostr, ContentType.Text.Plain, HttpStatusCode.OK)
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
            val timeStamp = call.request.queryParameters["timeStamp"]
            val nonce = call.request.queryParameters["nonce"]
            val encryptType = call.request.queryParameters["encrypt_type"]?:"aes"

            val reXml = OA.msgHub.handleXmlMsg(body, msgSignature, timeStamp, nonce, encryptType)

            call.respondText(reXml?:"success", ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }

}

