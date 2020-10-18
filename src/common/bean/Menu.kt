package com.github.rwsbillyang.wxSDK.common.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * 菜单的响应动作类型
 * https://work.weixin.qq.com/api/doc/90000/90135/90231
 *
 * click	点击推事件	成员点击click类型按钮后，企业微信服务器会通过消息接口推送消息类型为event的结构给开发者，并且带上按钮中开发者填写的key值，开发者可以通过自定义的key值与成员进行交互；
 * view	跳转URL	成员点击view类型按钮后，企业微信客户端将会打开开发者在按钮中填写的网页URL，可与网页授权获取成员基本信息接口结合，获得成员基本信息。
 * scancode_push	扫码推事件	成员点击按钮后，企业微信客户端将调起扫一扫工具，完成扫码操作后显示扫描结果（如果是URL，将进入URL），且会将扫码的结果传给开发者，开发者可用于下发消息。
 * scancode_waitmsg	扫码推事件 且弹出“消息接收中”提示框	成员点击按钮后，企业微信客户端将调起扫一扫工具，完成扫码操作后，将扫码的结果传给开发者，同时收起扫一扫工具，然后弹出“消息接收中”提示框，随后可能会收到开发者下发的消息。
 * pic_sysphoto	弹出系统拍照发图	弹出系统拍照发图 成员点击按钮后，企业微信客户端将调起系统相机，完成拍照操作后，会将拍摄的相片发送给开发者，并推送事件给开发者，同时收起系统相机，随后可能会收到开发者下发的消息。
 * pic_photo_or_album	弹出拍照或者相册发图	成员点击按钮后，企业微信客户端将弹出选择器供成员选择“拍照”或者“从手机相册选择”。成员选择后即走其他两种流程。
 * pic_weixin	弹出企业微信相册发图器	成员点击按钮后，企业微信客户端将调起企业微信相册，完成选择操作后，将选择的相片发送给开发者的服务器，并推送事件给开发者，同时收起相册，随后可能会收到开发者下发的消息。
 * location_select	弹出地理位置选择器	成员点击按钮后，企业微信客户端将调起地理位置选择工具，完成选择操作后，将选择的地理位置发送给开发者的服务器，同时收起位置选择工具，随后可能会收到开发者下发的消息。
 * view_miniprogram	跳转到小程序	成员点击按钮后，企业微信客户端将会打开开发者在按钮中配置的小程序
 * media_id：下发消息（除文本消息）用户点击media_id类型按钮后，微信服务器会将开发者填写的永久素材id对应的素材下发给用户，永久素材类型可以是图片、音频、视频、图文消息。请注意：永久素材id必须是在“素材管理/新增永久素材”接口上传后获得的合法id。
 * view_limited：跳转图文消息URL 用户点击view_limited类型按钮后，微信客户端将打开开发者在按钮中填写的永久素材id对应的图文消息URL，永久素材类型只支持图文消息。请注意：永久素材id必须是在“素材管理/新增永久素材”接口上传后获得的合法id。
 * */
@Serializable
enum class MenuType(val value: String) {
    @SerialName("click")
    Click("click"),
    @SerialName("view")
    View("view"),
    @SerialName("scancode_push")
    ScanCodePush("scancode_push"),
    @SerialName("scancode_waitmsg")
    ScanCodeWaitMsg("scancode_waitmsg"),
    @SerialName("pic_sysphoto")
    PicSysPhoto("pic_sysphoto"),
    @SerialName("pic_photo_or_album")
    PicPhotoOrAlbum("pic_photo_or_album"),
    @SerialName("pic_weixin")
    PicWeiXin("pic_weixin"),
    @SerialName("location_select")
    LocationSelect("location_select"),
    @SerialName("view_miniprogram")
    ViewMiniProgram("view_miniprogram"),
    @SerialName("media_id") MediaId("media_id"),
    @SerialName("view_limited") ViewLimited("view_limited"),
    @SerialName("parent")
    Parent("parent") //自定义的类型，用于前端将菜单类型标识为一级菜单
}

/**
 * @param type	是	菜单的响应动作类型
 * @param name	是	菜单的名字。不能为空，主菜单不能超过16字节，子菜单不能超过40字节。
 * @param key	click等点击类型必须	菜单KEY值，用于消息接口推送，不超过128字节
 * @param url	view类型必须	网页链接，成员点击菜单可打开链接，不超过1024字节。为了提高安全性，建议使用https的url
 * @param pagepath	view_miniprogram类型必须	小程序的页面路径
 * @param appid	view_miniprogram类型必须	小程序的appid（仅认证公众号可配置）
 * @param sub_button	否	二级菜单数组，个数应为1~5个
 * */
@Serializable
data class Menu(val name: String,
                val type: MenuType?,
                val key: String? = null,
                val url: String? = null,
                @SerialName("media_id")
                val mediaId: String? = null,
                @SerialName("pagepath")
                val pagePath: String? = null,
                @SerialName("appid")
                val appId: String? = null,
                @SerialName("sub_button")
                val subButtons: List<Menu>? = null
) {
    init {
        when (type) {
            MenuType.Click -> requireNotNull(key) { "key  click等点击类型必须" }
            MenuType.View -> requireNotNull(url) { "view类型必须 网页链接" }
            MenuType.ViewMiniProgram -> {
                requireNotNull(pagePath) { "pagepath view_miniprogram类型必须" }
                requireNotNull(appId) { "appid\tview_miniprogram类型必须\t小程序的appid（仅认证公众号可配置）" }
            }
            MenuType.MediaId,MenuType.ViewLimited -> requireNotNull(mediaId) { "media_id\tmedia_id类型和view_limited类型必须\t调用新增永久素材接口返回的合法media_id" }
            null -> require(!subButtons.isNullOrEmpty() && subButtons.size < 6) { "二级菜单数组，个数应为1~5个" }
            else -> {
            }
        }
    }
}

@Serializable
class Menus(@SerialName("button") val menus: List<Menu>)
