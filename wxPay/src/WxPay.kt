/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:33
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.wxSDK.wxPay


import com.github.rwsbillyang.wxSDK.wxPay.auth.*
import com.github.rwsbillyang.wxSDK.wxPay.util.PemUtil
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import org.apache.http.HttpException
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpResponseInterceptor
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpRequestWrapper
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder


object WxPay {
    private var _context: WxPayContext? = null
    val context: WxPayContext
        get() {
            requireNotNull(_context)
            return _context!!
        }

    fun isInited() = _context != null

    /**
     * 非ktor平台可以使用此函数进行配置
     * */
    fun config(block: WxPayConfiguration.() -> Unit) {
        val config = WxPayConfiguration().apply(block)
        _context = WxPayContext(
            config.appId,
            config.mchId,
            config.serialNo,
            config.privateKey,
            config.apiV3Key.toByteArray(),
            //config.useAutoUpdateCertificatesVerifier,
            config.certificate
        )
    }

    internal fun client() = HttpClient(Apache) { wxPayClientConfig() }
}

/**
 * @property appId 公众号或小程序appId
 * @property mchId 商户号
 *
 * @property apiV3Key 为了保证安全性，微信支付在回调通知和平台证书下载接口中，对关键信息进行了AES-256-GCM加密。
 * API v3密钥是加密时使用的对称密钥。
 * 商户需先在【微信商户平台—>账户设置—>API安全—>设置APIv3密钥】的页面设置该密钥，请求才能通过微信支付的签名校验。密钥的长度为32个字节。
 * APIv3密钥与API密钥是隔离的，设置该密钥时，不会导致API密钥发生变化。APIv3密钥属于敏感信息，请妥善保管不要泄露，如果怀疑信息泄露，请重设密钥。
 *
 * @property serialNo 商户签名使用商户私钥，证书序列号包含在请求HTTP头部的Authorization的serial_no
 *
 * @property privateKey 商户API私钥  商户申请商户API证书时，会生成商户私钥，并保存在本地证书文件夹的文件apiclient_key.pem中。
 * 私钥也可以通过工具从商户的p12证书中导出。请妥善保管好你的商户私钥文件
 *
 * @property certificate 平台证书
 * 平台证书是指由微信支付负责申请的，包含微信支付平台标识、公钥信息的证书。商户可以使用平台证书中的公钥进行验签。
 * 微信支付平台证书请调用“获取平台证书接口“获取。不同的商户，对应的微信支付平台证书是不一样的.
 * 平台证书会周期性更换。商户应定时通过API下载新的证书。请参考我们的更新指引，不要依赖人工更新证书。
 * */
class WxPayConfiguration {
    var appId: String = "your_app_id"
    var mchId: String = "your_mch_id" // 商户号
    var apiV3Key: String = "apiV3Key" //微信支付在回调通知和平台证书下载接口中，对关键信息进行了AES-256-GCM加密

    var serialNo: String = "your_serialNo" // 商户证书序列号

    // 你的商户私钥
    var privateKey: String = """
        -----BEGIN PRIVATE KEY-----
        -----END PRIVATE KEY-----
        """.trimIndent()

    //var useAutoUpdateCertificatesVerifier: Boolean = true

    // 你的微信支付平台证书
    var certificate: String? = null
//        """
//        -----BEGIN CERTIFICATE-----
//        -----END CERTIFICATE-----
//        """.trimIndent()
}

class WxPayContext(
    val appId: String,
    val mchId: String,
    val serialNo: String,
    val privateKey: String,
    val apiV3Key: ByteArray,
    //val useAutoUpdateCertificatesVerifier: Boolean = true,
    val certificate: String? = null
) {
    val signer = PrivateKeySigner(
        serialNo,
        PemUtil.loadPrivateKey(privateKey.byteInputStream())
    )

    val credentials = WechatPayCredentials(mchId, signer)

    val verifier =
    //if(useAutoUpdateCertificatesVerifier)
        //不需要传入微信支付证书，将会自动更新
        AutoUpdateCertificatesVerifier(
            //credentials,
            apiV3Key
        )
//    else {
//        requireNotNull(certificate){"please config certificate of wechat platform"}
//        CertificatesVerifier(arrayListOf(PemUtil.loadCertificate(certificate.byteInputStream())))
//    }

    val validator = WechatPayValidator(verifier)

    fun onChanged() {
        WxPayApi.client = WxPay.client()
    }
}

//https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient
fun HttpAsyncClientBuilder.configWechat(ctx: WxPayContext) {
    // 添加认证信息
    val requestInterceptor = HttpRequestInterceptor { request, _ ->
        request.addHeader(
            "Authorization",
            ctx.credentials.schema + " " + ctx.credentials.getToken(request as HttpRequestWrapper)
        )
    }

    // 对成功应答验签
    val responseInterceptor = HttpResponseInterceptor { response, httpContext ->
        val statusLine: StatusLine = response.statusLine
        if (statusLine.statusCode in 200..299) {
            //convertToRepeatableResponseEntity(response)
            if (!ctx.validator.validate(response as CloseableHttpResponse)) {
                throw HttpException("应答的微信支付签名验证失败")
            }
        }
    }

    addInterceptorFirst(requestInterceptor)
    addInterceptorFirst(responseInterceptor)
}

fun HttpClientConfig<ApacheEngineConfig>.wxPayClientConfig() {
    install(HttpTimeout) {}
    install(JsonFeature) {
        serializer = KotlinxSerializer(WxPayApi.apiJson)
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }
    engine {
        customizeClient {
            // Maximum number of socket connections.
            setMaxConnTotal(20480)

            // Maximum number of requests for a specific endpoint route.
            setMaxConnPerRoute(10240)

            val os = System.getProperty("os.name") + "/" + System.getProperty("os.version")
            val ver = System.getProperty("java.version") ?: "Unknown"
            val userAgent = "WechatPay-Apache-HttpAsyncClient/com.github.rwsbillyang.wxSDK.wxPay ($os) Java/$ver"
            setUserAgent(userAgent)

            if (WxPay.isInited())
                configWechat(WxPay.context)
        }
        customizeRequest {
            // this: RequestConfig.Builder from Apache.
        }
    }
}