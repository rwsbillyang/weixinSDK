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
import com.github.rwsbillyang.wxSDK.security.PemUtil

import java.io.FileInputStream


object WxPay {
    val ApiContextMap = hashMapOf<String, WxPayContext>()
    /**
     * 配置参数
     * */
    fun config(block: WxPayConfiguration.() -> Unit) {
        val config = WxPayConfiguration().apply(block)

         val ctx = WxPayContext(
                config.appId,
                config.mchId,
                config.serialNo,
                config.apiKeyCertFilePath,
                config.apiV3Key,
                config.notifyHost
        )
        ApiContextMap[config.appId] = ctx
        ctx.verifier.startDownload()
    }
    var payNotifyUrlPrefix: String = "/api/sale/wx/payNotify/"

    fun payNotifyPath(appId: String): String {
        val ctx = ApiContextMap[appId]
        requireNotNull(ctx){"config wxPayConfig for appId=$appId ?"}
        return ctx.notifyHost + payNotifyUrlPrefix + appId
    }
}

/**
 * @property appId 公众号或小程序appId
 * @property mchId 商户号

 * @property apiV3Key APIv3密钥， 用于：下载平台证书接口、处理回调通知中报文时，要通过该密钥来解密信息直接在后台设置（需要安装操作证书之后才可设置）
 * 为了保证安全性，微信支付在回调通知和平台证书下载接口中，对关键信息进行了AES-256-GCM加密。
 * API v3密钥是加密时使用的对称密钥。
 * 商户需先在【微信商户平台—>账户设置—>API安全—>设置APIv3密钥】的页面设置该密钥，请求才能通过微信支付的签名校验。密钥的长度为32个字节。
 * APIv3密钥与API密钥是隔离的，设置该密钥时，不会导致API密钥发生变化。APIv3密钥属于敏感信息，请妥善保管不要泄露，如果怀疑信息泄露，请重设密钥。
 *
 * @property serialNo 证书序列号 登陆商户平台【API安全】->【API证书】->【查看证书】，可查看商户API证书序列号。
 * 也可以商户API证书和微信支付平台证书均可以使用第三方的证书解析工具，查看证书内容。或者使用openssl命令行工具查看证书序列号。
 * openssl x509 -in 1900009191_20180326_cert.pem -noout -serial
 * serial=1DDExxxxxxxxxxxxxxxxxxxxA8C
 *
 * @property apiKeyCertFilePath 商户API私钥  用于识别客户端身份， 即API证书文件中的内容。在"API安全->API证书"按照指引  商户申请商户API证书时，会生成商户私钥。
 *  按照指引生成并保存到本地的证书文件夹的文件apiclient_key.pem中。
 * 私钥也可以通过工具从商户的p12证书中导出。请妥善保管好你的商户私钥文件
 *

 * 平台证书是指由微信支付负责申请的，包含微信支付平台标识、公钥信息的证书。平台证书会周期性更换。
 * 商户应定时通过API下载新的证书。请参考我们的更新指引，不要依赖人工更新证书。
 * 商户可以使用平台证书中的公钥进行验签。微信支付平台证书请调用“获取平台证书接口“获取。不同的商户，对应的微信支付平台证书是不一样的.
 *
 * */
class WxPayConfiguration {
    var appId: String = "your_app_id"
    var mchId: String = "your_mch_id" // 商户号
    var serialNo: String = "your_serialNo" // API密钥 后台中设置
    var apiKeyCertFilePath: String = "apiclient_key.pem file path" //API证书， 按照指引，由工具生成的文件路径
    var apiV3Key: String = "apiV3Key" // APIv3密钥 后台中设置
    var notifyHost: String = "https://www.your_site.com" // 用于支付成功时，拼接通知地址域名，如： https://uufenxiang.niukid.cn 必须在微信支付后台中好此域名
}



class WxPayContext(
    val appId: String,
    val mchId: String,
    val serialNo: String,
    val privateKeyFilePath: String, //apiclient_key.pem
    val apiV3Key: String,
    val notifyHost: String
) {
    val signer = PrivateKeySigner(serialNo,PemUtil.loadPrivateKey(FileInputStream(privateKeyFilePath)))
    val credentials = WechatPayCredentials(mchId, signer)
    val verifier =  AutoUpdateCertificatesVerifier(appId, apiV3Key.toByteArray())
    val validator = WechatPayValidator(verifier)
}


