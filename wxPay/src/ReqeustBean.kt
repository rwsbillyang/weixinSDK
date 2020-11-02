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
 * @param appId 公众号ID	appid	string[1,32]	是	body 直连商户申请的公众号或移动应用appid。示例值：wxd678efh567hg6787
 * @param mchId 直连商户号	mchid	string[1,32]	是	body 直连商户的商户号，由微信支付生成并下发。示例值：1230000109
 * @param description 商品描述	description	string[1,127]	是	body 商品描述 示例值：Image形象店-深圳腾大-QQ公仔
 * @param orderId 商户订单号	out_trade_no	string[6,32]	是	body 商户系统内部订单号，只能是数字、大小写字母_-*且在同一个商户号下唯一，详见【商户订单号】。示例值：1217752501201407033233368018
 * @param notifyUrl 通知地址	notify_url	string[1,256]	是	body 通知URL必须为直接可访问的URL，不允许携带查询串 格式：URL 示例值：https://www.weixin.qq.com/wxpay/pay.php
 * @param amount 订单金额	amount	object	是	body 订单金额信息
 * @param payer 支付者	payer	object	是	body 支付者信息
 * @param attach 附加数据	attach	string[1,128]	否	body 附加数据，在查询API和支付通知中原样返回，可作为自定义参数使用 示例值：自定义数据
 * @param expire 交易结束时间	time_expire	string[1,64]	否	body 订单失效时间，遵循rfc3339标准格式，格式为YYYY-MM-DDTHH:mm:ss+TIMEZONE，YYYY-MM-DD表示年月日，T出现在字符串中，表示time元素的开头，HH:mm:ss表示时分秒，TIMEZONE表示时区（+08:00表示东八区时间，领先UTC 8小时，即北京时间）。例如：2015-05-20T13:29:35+08:00表示，北京时间2015年5月20日 13点29分35秒。示例值：2018-06-08T10:34:56+08:00
 * @param goodsTags 订单优惠标记	goods_tag	string[1,32]	否	body 订单优惠标记 示例值：WXG
 * @param detail 优惠功能	detail	object	否	body 优惠功能
 * @param sceneInfo 场景信息	scene_info	object	否	body 支付场景描述

 *
 * https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/pay/transactions/chapter3_2.shtml
 * */
@Serializable
class Transaction(
    @SerialName("appid")
        val appId: String,
    @SerialName("mchid")
        val mchId: String,
    val description: String,
    @SerialName("out_trade_no")
        val orderId: String,
    @SerialName("notify_url")
        val notifyUrl: String,
    val amount: OrderAmount,
    val payer: Payer,
    @SerialName("scene_info")
        val sceneInfo: SceneInfo? = null,
    val attach: String? = null,
    @SerialName("time_expire")
        val expire: String? = null,
    @SerialName("goods_tag")
        val goodsTags: String? = null,
    val detail: OrderDetail? = null
)
{
    constructor(orderId: String, description: String, total: Int, openId: String,
                ip: String, notifyUrl: String,
                appId: String = WxPay.context.appId,
                mchId: String = WxPay.context.mchId)
            : this(appId, mchId, description, orderId, notifyUrl, OrderAmount(total), Payer(openId), SceneInfo(ip))
}

/**
 * @param total 总金额	total	int	是	订单总金额，单位为分。示例值：100
 * @param currency 货币类型	currency	string[1,16]	否	CNY：人民币，境内商户号仅支持人民币。示例值：CNY
 * */
@Serializable
class OrderAmount(
        val total: Int,
        val currency: String? = "CNY"
)

/**
 * @param openId 用户标识	openid	string[1,128]	是	用户在直连商户appid下的唯一标识。示例值：oUpF8uMuAJO_M2pxb1Q9zNjWeS6o
 * */
@Serializable
class Payer(
        @SerialName("openid")
        val openId: String
)

/**
 * @param costPrice 订单原价	cost_price	int	否
 * 1、商户侧一张小票订单可能被分多次支付，订单原价用于记录整张小票的交易金额。
 * 2、当订单原价与支付金额不相等，则不享受优惠。
 * 3、该字段主要用于防止同一张小票分多次支付，以享受多次优惠的情况，正常支付订单不必上传此参数。示例值：608800
 *
 * @param invoiceId 商品小票ID	invoice_id	string[1,32]	否	商家小票ID 示例值：微信123
 * @param goodsList 单品列表	goods_detail	array	否	单品列表信息 条目个数限制：【1，undefined】
 * */
@Serializable
class OrderDetail(
        @SerialName("cost_price")
        val costPrice: Int,
        @SerialName("invoice_id")
        val invoiceId: String? = null,
        @SerialName("goods_detail")
        val goodsList: List<GoodsDetail>? = null
)

/**
 * @param goodsId 商户侧商品编码	merchant_goods_id	string[1,32]	是	由半角的大小写字母、数字、中划线、下划线中的一种或几种组成。示例值：商品编码
 * @param quantity 商品数量	quantity	int	是	用户购买的数量 示例值：1
 * @param price 商品单价	unit_price	int	是	商品单价，单位为分 示例值：828800
 * @param wechatGoodsId 微信侧商品编码	wechatpay_goods_id	string[1,32]	否	微信支付定义的统一商品编号（没有可不传）示例值：1001
 * @param name 商品名称	goods_name	string[1,256]	否	商品的实际名称 示例值：iPhoneX 256G
 * */
@Serializable
class GoodsDetail(
        @SerialName("merchant_goods_id")
        val goodsId: String,
        val quantity: Int,
        @SerialName("unit_price")
        val price: Int,
        @SerialName("wechatpay_goods_id")
        val wechatGoodsId: String? = null,
        @SerialName("goods_name")
        val name: String? = null
)

/**
 * @param ip 用户终端IP	payer_client_ip	string[1,45]	是	调用微信支付API的机器IP，支持IPv4和IPv6两种格式的IP地址。示例值：14.23.150.211
 * @param deviceId 商户端设备号	device_id	string[1,32]	否	商户端设备号（门店号或收银设备ID）。示例值：013467007045764
 * @param storeInfo 商户门店信息	store_info	object	否	商户门店信息
 * @param h5Info H5场景信息	h5_info	object	是	H5场景信息
 * */
@Serializable
class SceneInfo(
    @SerialName("payer_client_ip")
        val ip: String,
    @SerialName("device_id")
        val deviceId: String? = null,
    @SerialName("store_info")
        val storeInfo: StoreInfo? = null,
    @SerialName("h5_info")
        val h5Info: H5Info? = null
)

/**
 *
 * @param name 门店名称	name	string[1,256]	是	商户侧门店名称 示例值：腾讯大厦分店
 * @param code 地区编码	area_code	string[1,32]	是	地区编码，详细请见省市区编号对照表。 示例值：440305
 * @param address 详细地址	address	string[1,512]	是	详细的商户门店地址 示例值：广东省深圳市南山区科技中一道10000号
 * @param id 门店编号	id	string[1,32]	否	商户侧门店编号 示例值：0001
 * */
@Serializable
class StoreInfo(
        val name: String,
        @SerialName("area_code")
        val code: String,
        val address: String,
        val id: String? = null
)

/**
 * @param type 场景类型	type	string[1,32]	是	场景类型 示例值：iOS, Android, Wap
 * @param appName 应用名称	app_name	string[1,64]	否	应用名称  示例值：王者荣耀
 * @param appUrl 网站URL	app_url	string[1,128]	否	网站URL 示例值：https://pay.qq.com
 * @param bundleId iOS平台BundleID	bundle_id	string[1,128]	否	iOS平台BundleID 示例值：com.tencent.wzryiOS
 * @param packageName Android平台PackageName	package_name	string[1,128]	否	Android平台PackageName 示例值：com.tencent.tmgp.sgame
 * */
@Serializable
class H5Info(
        val type: String,
        @SerialName("app_name")
        val appName: String,
        @SerialName("app_url")
        val appUrl: String? = null,
        @SerialName("bundle_id")
        val bundleId: String? = null,
        @SerialName("package_name")
        val packageName: String? = null
) {
    companion object {
        const val TYPE_IOS = "iOS"
        const val TYPE_ANDROID = "Android"
        const val TYPE_WAP = "Wap"
    }
}