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


import com.github.rwsbillyang.wxSDK.wxPay.auth.Signer
import org.apache.commons.lang3.RandomStringUtils

/**
 * 公众号id	appId	string[1,16]	是	请填写merchant_appid对应的值。示例值：wx8888888888888888
 * 时间戳	timeStamp	string[1,32]	是	当前的时间，标准北京时间，时区为东八区，自1970年1月1日 0点0分0秒以来的秒数。注意：部分系统取到的值为毫秒级，需要转换成秒(10位数字)。示例值：1414561699
 * 随机字符串	nonceStr	string[1,32]	是	随机字符串，不长于32位。推荐随机数生成算法。示例值：5K8264ILTKCH16CQ2502SI8ZNMTM67VS
 * 订单详情扩展字符串	package	string[1,128]	是	统一下单接口返回的prepay_id参数值，提交格式如：prepay_id=*** 示例值：prepay_id=wx201410272009395522657a690389285100
 * 签名方式	signType	string[1,32]	是	签名类型，默认为RSA，仅支持RSA。 示例值：RSA
 * 签名	paySign	string[1,256]	是	签名，使用字段appId、timeStamp、nonceStr、package按照签名生成算法计算得出的签名值 示例值：oR9d8PuhnIc+YZ8cBHFCwfgpaK9gd7vaRvkYD7rthRAZ\/X+QBhcCYL21N7cHCTUxbQ+EAt6Uy+lwSN22f5YZvI45MLko8Pfso0jm46v5hqcVwrk6uddkGuT+Cdvu4WBqDzaDjnNa5UK3GfE1Wfl2gHxIIY5lLdUgWFts17D4WuolLLkiFZV+JSHMvH7eaLdT9N5GBovBwu5yYKUR7skR8Fu+LozcSqQixnlEZUfyE55feLOQTUYzLmR9pNtPbPsu6WVhbNHMS3Ss2+AehHvz+n64GDmXxbX++IOBvm2olHu3PsOUGRwhudhVf7UcGcunXt8cqNjKNqZLhLw4jq\/xDg==
 * */
class JsPaySignature(
    val appId: String,
    val timeStamp: String,
    val nonceStr: String,
    val `package`: String,
    var paySign: String? = null,
    val signType: String = "RSA" //need encodeDefaults = true
){
    constructor(prepayId: String): this(
        WxPay.context.appId, (System.currentTimeMillis()/1000).toString(),
        RandomStringUtils.randomAlphanumeric(32), "prepay_id=$prepayId")

    init {
        //签名串一共有四行，每一行为一个参数。行尾以\n（换行符，ASCII编码值为0x0A）结束，包括最后一行。如果参数本身以\n结束，也需要附加一个\n
        //公众号id\n时间戳\n随机字符串\n订单详情扩展字符串\n
        val original = "$appId\n$timeStamp\n$nonceStr\n${`package`}\n"

        val signResult: Signer.SignatureResult = WxPay.context.signer.sign(original.toByteArray())

        paySign = signResult.sign
    }
}