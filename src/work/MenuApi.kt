package com.github.rwsbillyang.wxSDK.work

import com.github.rwsbillyang.wxSDK.common.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * 菜单的响应动作类型
 * https://work.weixin.qq.com/api/doc/90000/90135/90231
 *
 * click	点击推事件	成员点击click类型按钮后，企业微信服务器会通过消息接口推送消息类型为event 的结构给开发者（参考消息接口指南），并且带上按钮中开发者填写的key值，开发者可以通过自定义的key值与成员进行交互；
 * view	跳转URL	成员点击view类型按钮后，企业微信客户端将会打开开发者在按钮中填写的网页URL，可与网页授权获取成员基本信息接口结合，获得成员基本信息。
 * scancode_push	扫码推事件	成员点击按钮后，企业微信客户端将调起扫一扫工具，完成扫码操作后显示扫描结果（如果是URL，将进入URL），且会将扫码的结果传给开发者，开发者可用于下发消息。
 * scancode_waitmsg	扫码推事件 且弹出“消息接收中”提示框	成员点击按钮后，企业微信客户端将调起扫一扫工具，完成扫码操作后，将扫码的结果传给开发者，同时收起扫一扫工具，然后弹出“消息接收中”提示框，随后可能会收到开发者下发的消息。
 * pic_sysphoto	弹出系统拍照发图	弹出系统拍照发图 成员点击按钮后，企业微信客户端将调起系统相机，完成拍照操作后，会将拍摄的相片发送给开发者，并推送事件给开发者，同时收起系统相机，随后可能会收到开发者下发的消息。
 * pic_photo_or_album	弹出拍照或者相册发图	成员点击按钮后，企业微信客户端将弹出选择器供成员选择“拍照”或者“从手机相册选择”。成员选择后即走其他两种流程。
 * pic_weixin	弹出企业微信相册发图器	成员点击按钮后，企业微信客户端将调起企业微信相册，完成选择操作后，将选择的相片发送给开发者的服务器，并推送事件给开发者，同时收起相册，随后可能会收到开发者下发的消息。
 * location_select	弹出地理位置选择器	成员点击按钮后，企业微信客户端将调起地理位置选择工具，完成选择操作后，将选择的地理位置发送给开发者的服务器，同时收起位置选择工具，随后可能会收到开发者下发的消息。
 * view_miniprogram	跳转到小程序	成员点击按钮后，企业微信客户端将会打开开发者在按钮中配置的小程序
 * */

enum class MenuType{
    @SerialName("click") Click,
    @SerialName("view") View,
    @SerialName("scancode_push") ScanCodePush,
    @SerialName("scancode_waitmsg") ScanCodeWaitMsg,
    @SerialName("pic_sysphoto") PicSysPhoto,
    @SerialName("pic_photo_or_album") PicPhotoOrAlbum,
    @SerialName("pic_weixin") PicWeiXin,
    @SerialName("location_select") LocationSelect,
    @SerialName("view_miniprogram") ViewMiniProgram,
    @SerialName("Unknown") Unknown
}

/**
 * type	是	菜单的响应动作类型
 * name	是	菜单的名字。不能为空，主菜单不能超过16字节，子菜单不能超过40字节。
 * key	click等点击类型必须	菜单KEY值，用于消息接口推送，不超过128字节
 * url	view类型必须	网页链接，成员点击菜单可打开链接，不超过1024字节。为了提高安全性，建议使用https的url
 * pagepath	view_miniprogram类型必须	小程序的页面路径
 * appid	view_miniprogram类型必须	小程序的appid（仅与企业绑定的小程序可配置）
 * sub_button	否	二级菜单数组，个数应为1~5个
 * */
@Serializable
data class Menu(val name: String,
                val type: MenuType,
                val key: String? = null,
                val url: String? = null,
                @SerialName("pagepath")
                val pagePath: String? = null,
                @SerialName("appid")
                val appId: String? = null,
                @SerialName("sub_button")
                val subButtons: List<Menu>? = null
)

class MenuApi: WorkBaseApi(){
    override val group = "menu"
    companion object{
        const val CREATE = "create"
        const val DETAIL = "get"
        const val DELETE = "delete"
    }

    /**
     * 创建菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90231
     * */
    fun create(agentId: String, body: List<Menu>) = doPost(CREATE, body, mapOf("agentid" to agentId))

    /**
     * 获取菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90232
     * */
    fun detail(agentId: String): List<Menu> = get(base, group, DETAIL,  mapOf("agentid" to agentId))

    /**
     * 删除菜单
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90233
     * */
    fun delete(agentId: String) = doGet(DELETE,  mapOf("agentid" to agentId))
}