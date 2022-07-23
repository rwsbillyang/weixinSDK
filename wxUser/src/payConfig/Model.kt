/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-12-14 18:33
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

package com.github.rwsbillyang.wxUser.payConfig

import kotlinx.serialization.Serializable

/**
 * @param appId 公众号或小程序appId
 * @param mchId 商户号
 *
 * @param apiV3Key APIv3密钥，直接在后台设置（需要安装操作证书之后才可设置）
 * 为了保证安全性，微信支付在回调通知和平台证书下载接口中，对关键信息进行了AES-256-GCM加密。
 * API v3密钥是加密时使用的对称密钥。
 * 商户需先在【微信商户平台—>账户设置—>API安全—>设置APIv3密钥】的页面设置该密钥，请求才能通过微信支付的签名校验。密钥的长度为32个字节。
 * APIv3密钥与API密钥是隔离的，设置该密钥时，不会导致API密钥发生变化。APIv3密钥属于敏感信息，请妥善保管不要泄露，如果怀疑信息泄露，请重设密钥。
 *
 * @param serialNo 证书序列号 登陆商户平台【API安全】->【API证书】->【查看证书】，可查看商户API证书序列号。
也可以商户API证书和微信支付平台证书均可以使用第三方的证书解析工具，查看证书内容。或者使用openssl命令行工具查看证书序列号。
openssl x509 -in 1900009191_20180326_cert.pem -noout -serial
serial=1DDExxxxxxxx3A8C
 *
 * @param privateKeyPath apiKeyCertFilePath 商户API私钥  即API证书文件中的内容。在"API安全->API证书"按照指引  商户申请商户API证书时，会生成商户私钥。
 *  按照指引生成并保存到本地的证书文件夹的文件apiclient_key.pem中。
 * 私钥也可以通过工具从商户的p12证书中导出。请妥善保管好你的商户私钥文件

 * */
@Serializable
data class WxPayConfig(
    val appId: String,
    val mchId: String,
    val serialNo: String, //用来按照指定规则对客户端请求参数进行签名，服务器收到请求时会进行签名验证
    val apiV3Key: String, //用于：下载平台证书接口、处理回调通知中报文时，要通过该密钥来解密信息
    val privateKeyPath: String, //用于识别客户端身份 apiclient_key.pem
    val notifyHost: String // 用于支付成功时，拼接通知地址域名，如： https://uufenxiang.niukid.cn 必须在微信支付后台中好此域名
)