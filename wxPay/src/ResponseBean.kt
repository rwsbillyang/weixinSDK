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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param code：详细错误码
 * @param message：错误描述，使用易理解的文字表示错误的原因。
 *
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/wei-xin-zhi-fu-api-v3-jie-kou-gui-fan
 * */

interface IResponse {
    val code: String?
    val message: String?
    val detail: ErrorDetail?
}


/**
 * @param field：指示错误参数的位置。当错误参数位于请求body的JSON时，填写指向参数的JSON Pointer。当错误参数位于请求的url或者querystring时，填写参数的变量名。
 * @param value：错误的值
 * @param issue：具体错误原因
 * @param location
 * */
@Serializable
class ErrorDetail(
        val field: String? = null,
        val value: String? = null,
        val issue: String? = null,
        val location: String? = null
)

@Serializable
class ResponseClose(override val code: String? = null,
                    override val message: String? = null,
                    override val detail: ErrorDetail? = null): IResponse

@Serializable
class ResponseOrder(
        @SerialName("prepay_id")
        val prepayId: String? = null,
        override val code: String? = null,
        override val message: String? = null,
        override val detail: ErrorDetail? = null
) : IResponse

/**
 * 二维码链接	code_url	string[1,512]	是	此URL用于生成支付二维码，然后提供给用户扫码支付。
 * 注意：code_url并非固定值，使用时按照URL格式转成二维码即可。
 * 示例值：weixin://wxpay/bizpayurl/up?pr=NwY5Mz9&groupid=00
 * */
@Serializable
class ResponseOrderNative(
        @SerialName("code_url")
        val codeUrl: String? = null,
        override val code: String? = null,
        override val message: String? = null,
        override val detail: ErrorDetail? = null
) : IResponse

/**
 * 支付跳转链接	h5_url	string[1,512]	是	h5_url为拉起微信支付收银台的中间页面，
 * 可通过访问该url来拉起微信客户端，完成支付，h5_url的有效期为5分钟。
 * 示例值：https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wx2016121516420242444321ca0631331346&package=1405458241
 * */
@Serializable
class ResponseOrderH5(
        @SerialName("h5_url")
        val h5Url: String? = null,
        @SerialName("prepay_id")
        val prepayId: String? = null,
        override val code: String? = null,
        override val message: String? = null,
        override val detail: ErrorDetail? = null
) : IResponse

/**
 * @param appId 公众号ID	appid	string[1,32]	是	body 直连商户申请的公众号或移动应用appid。示例值：wxd678efh567hg6787
 * @param mchId 直连商户号	mchid	string[1,32]	是	body 直连商户的商户号，由微信支付生成并下发。示例值：1230000109
 * @param orderId 商户订单号	out_trade_no	string[6,32]	是	body 商户系统内部订单号，只能是数字、大小写字母_-*且在同一个商户号下唯一，详见【商户订单号】。示例值：1217752501201407033233368018\
 * @param

 * @param amount 订单金额	amount	object	是	body 订单金额信息
 * @param payer 支付者	payer	object	是	body 支付者信息
 * @param attach 附加数据	attach	string[1,128]	否	body 附加数据，在查询API和支付通知中原样返回，可作为自定义参数使用 示例值：自定义数据
 * @param detail 优惠功能	detail	object	否	body 优惠功能
 * @param sceneInfo 场景信息	scene_info	object	否	body 支付场景描述
 *
 * 微信支付订单号	transaction_id	string[1,32]	否	微信支付系统生成的订单号。 示例值：1217752501201407033233368018
 * 交易类型	trade_type	string[1,16]	否	交易类型，枚举值：JSAPI：公众号支付 NATIVE：扫码支付APP：APP支付 MICROPAY：付款码支付MWEB：H5支付 FACEPAY：刷脸支付 示例值：MICROPAY
 * 交易状态	trade_state	string[1,32]	是	交易状态，枚举值：SUCCESS：支付成功 REFUND：转入退款 NOTPAY：未支付 CLOSED：已关闭 REVOKED：已撤销（付款码支付） USERPAYING：用户支付中（付款码支付） PAYERROR：支付失败(其他原因，如银行返回失败) 示例值：SUCCESS
 * 交易状态描述	trade_state_desc	string[1,256]	是	交易状态描述  示例值：支付失败，请重新下单支付
 *
 * 付款银行	bank_type	string[1,16]	否	银行类型，采用字符串类型的银行标识。示例值：CMC https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=4_2
 * 附加数据	attach	string[1,128]	否	附加数据，在查询API和支付通知中原样返回，可作为自定义参数使用 示例值：自定义数据
 * 支付完成时间	success_time	string[1,64]	否	支付完成时间，遵循rfc3339标准格式，格式为YYYY-MM-DDTHH:mm:ss+TIMEZONE，YYYY-MM-DD表示年月日，T出现在字符串中，表示time元素的开头，HH:mm:ss表示时分秒，TIMEZONE表示时区（+08:00表示东八区时间，领先UTC 8小时，即北京时间）。例如：2015-05-20T13:29:35+08:00表示，北京时间2015年5月20日 13点29分35秒。 示例值：2018-06-08T10:34:56+08:00
 * 场景信息	scene_info	object	否	支付场景描述
 * 优惠功能	promotion_detail	array	否	优惠功能，享受优惠时返回该字段。
 * https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pay/transactions/chapter3_2.shtml
 * */

class OrderPayDetail(
        @SerialName("appid")
        val appId: String,
        @SerialName("mchid")
        val mchId: String,
        @SerialName("out_trade_no")
        val orderId: String,
        @SerialName("transaction_id")
        val transactionId: String? = null,
        @SerialName("trade_type")
        val tradeType: String? = null,
        @SerialName("trade_state")
        val tradeState: String,
        @SerialName("trade_state_desc")
        val tradeStateDesc: String,
        @SerialName("bank_type")
        val bank: String? = null,
        val attach: String? = null,
        @SerialName("success_time")
        val successTime: String? = null,
        val payer: Payer,
        val amount: ReOrderAmount,
        @SerialName("scene_info")
        val sceneInfo: ReSceneInfo? = null,
        @SerialName("promotion_detail")
        val promotionDetail: Promotion? = null,
        override val code: String? = null,
        override val message: String? = null,
        override val detail: ErrorDetail? = null
): IResponse

/**
 * @param total 总金额	total	int	是	订单总金额，单位为分。示例值：100
 * @param currency 货币类型	currency	string[1,16]	否	CNY：人民币，境内商户号仅支持人民币。示例值：CNY
 * @param paidTotal 用户支付金额	payer_total	int	否	用户支付金额，单位为分。示例值：100
 * @param paidCurrency 用户支付币种	payer_currency	string[1,16]	否	用户支付币种 示例值：CNY
 *  */
@Serializable
class ReOrderAmount(
        val total: Int,
        val currency: String? = null,
        @SerialName("payer_total")
        val paidTotal: Int? = null,
        @SerialName("payer_currency")
        val paidCurrency: String? = null
)

@Serializable
class ReSceneInfo(
        @SerialName("device_id")
        val deviceId: String? = null
)

/**
 * 券ID	coupon_id	string[1,32]	是	券ID 示例值：109519
 * 优惠名称	name	string[1,64]	否	优惠名称 示例值：单品惠-6
 * 优惠范围	scope	string[1,32]	否	GLOBAL：全场代金券  SINGLE：单品优惠 示例值：GLOBAL
 * 优惠类型	type	string[1,32]	否	CASH：充值  NOCASH：预充值  示例值：CASH
 * 优惠券面额	amount	int	是	优惠券面额 示例值：100
 * 活动ID	stock_id	string[1,32]	否	活动ID 示例值：931386
 * 微信出资	wechatpay_contribute	int	否	微信出资，单位为分 示例值：0
 * 商户出资	merchant_contribute	int	否	商户出资，单位为分 示例值：0
 * 其他出资	other_contribute	int	否	其他出资，单位为分 示例值：0
 * 优惠币种	currency	string[1,16]	否	CNY：人民币，境内商户号仅支持人民币。 示例值：CNY
 * detail goods_detail	array	否	单品列表信息
 * */
@Serializable
class Promotion(
        @SerialName("coupon_id")
        val couponId: String,
        val name: String? = null,
        val scope: String? = null,
        val type: String? = null,
        val amount: Int,
        val stockId: String? = null,
        @SerialName("wechatpay_contribute")
        val contributeWechat: Int? = null,
        @SerialName("merchant_contribute")
        val contributeMerchant: Int? = null,
        @SerialName("other_contribute")
        val contributeOther: Int? = null,
        val currency: String? = null,
        @SerialName("goods_detail")
        val detail: List<ReGoodsDetail>? = null
)

/**
 * @param goodsId 商户侧商品编码	goods_id	string[1,32]	是	由半角的大小写字母、数字、中划线、下划线中的一种或几种组成。示例值：商品编码
 * @param quantity 商品数量	quantity	int	是	用户购买的数量 示例值：1
 * @param price 商品单价	unit_price	int	是	商品单价，单位为分 示例值：828800
 * 商品优惠金额	discount_amount	int	是	商品优惠金额 示例值：0
 * 商品备注	goods_remark	string[1,128]	否	商品备注信息 示例值：商品备注信息
 * */
@Serializable
class ReGoodsDetail(
        @SerialName("goods_id")
        val goodsId: String,
        val quantity: Int,
        @SerialName("unit_price")
        val price: Int,
        @SerialName("discount_amount")
        val discount: Int,
        @SerialName("goods_remark")
        val remark: String? = null
)

@Serializable
class CertificateBean(
        @SerialName("serial_no")
        val serialNo: String,
        @SerialName("effective_time")
        val effectiveTime: String,
        @SerialName("expire_time")
        val expireTime: String,
        @SerialName("encrypt_certificate")
        val encryptCertificate: EncryptData,
)

@Serializable
class EncryptData(
        val nonce: String,
        val ciphertext: String,
        @SerialName("associated_data")
        val associatedData: String? = null,
        val algorithm: String = "AEAD_AES_256_GCM"
)

/**
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/jie-kou-wen-dang/ping-tai-zheng-shu
 * */
@Serializable
class ResponseCertificates(
        val data: List<CertificateBean>
)
