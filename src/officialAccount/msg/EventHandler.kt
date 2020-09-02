package com.github.rwsbillyang.wxSDK.officialAccount.msg

import com.github.rwsbillyang.wxSDK.common.msg.IEventHandler
import com.github.rwsbillyang.wxSDK.common.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.common.msg.WxBaseEvent

/**
 * 微信推送过来的事件的处理接口
 *
 * */
interface IOAEventHandler: IEventHandler{
    /**
     * 关注事件
     *
     * 用户在关注与取消关注公众号时，微信会把这个事件推送到开发者填写的URL。
     * 方便开发者给用户下发欢迎消息或者做帐号的解绑。为保护用户数据隐私，开发者收到用户取消关注事件时需要删除该用户的所有信息。微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。
     * 关于重试的消息排重，推荐使用FromUserName + CreateTime 排重。
     * 假如服务器无法保证在五秒内处理并回复，可以直接回复空串，微信服务器不会对此作任何处理，并且不会发起重试。
     * */
    fun onWxSubscribeEvent(e: WxSubscribeEvent): ReBaseMSg?

    /**
     * 取消关注事件
     *
     * 用户在关注与取消关注公众号时，微信会把这个事件推送到开发者填写的URL。
     * 方便开发者给用户下发欢迎消息或者做帐号的解绑。为保护用户数据隐私，开发者收到用户取消关注事件时需要删除该用户的所有信息。微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。
     * 关于重试的消息排重，推荐使用FromUserName + CreateTime 排重。
     * 假如服务器无法保证在五秒内处理并回复，可以直接回复空串，微信服务器不会对此作任何处理，并且不会发起重试。
     * */
    fun onWxUnsubscribeEvent(e: WxUnsubscribeEvent): ReBaseMSg?

    /**
     * 用户未关注时，扫码关注后的事件推送
     * */
    fun onWxScanSubscribeEvent(e: WxScanSubscribeEvent): ReBaseMSg?

    /**
     * 用户已关注时，扫码关注后的事件推送
     *
     * */
    fun onWxScanEvent(e: WxScanEvent): ReBaseMSg?

    /**
     * 上报地理位置事件
     *
     * 用户同意上报地理位置后，每次进入公众号会话时，都会在进入时上报地理位置，或在进入会话后每5秒上报一次地理位置，
     * 公众号可以在公众平台网站中修改以上设置。上报地理位置时，微信会将上报地理位置事件推送到开发者填写的URL。
     * */
    fun onWxLocationEvent(e: WxLocationEvent): ReBaseMSg?

    /**
     * 点击菜单拉取消息时的事件推送
     *
     * 用户点击自定义菜单后，微信会把点击事件推送给开发者，请注意，点击菜单弹出子菜单，不会产生上报。
     * */
    fun onWxMenuClickEvent(e: WxMenuClickEvent): ReBaseMSg?

    /**
     * 点击菜单跳转链接时的事件推送
     * */
    fun onWxMenuViewEvent(e: WxMenuViewEvent): ReBaseMSg?

    /**
     * scancode_push：扫码推事件的事件推送
     *
     * 仅支持微信iPhone5.4.1以上版本，和Android5.4以上版本的微信用户
     * 旧版本微信用户点击后将没有回应，开发者也不能正常接收到事件推送。
     * */
    fun onWxMenuScanCodePushEvent(e: WxMenuScanCodePushEvent): ReBaseMSg?

    /**
     * scancode_waitmsg：扫码推事件且弹出“消息接收中”提示框的事件推送
     *
     * 仅支持微信iPhone5.4.1以上版本，和Android5.4以上版本的微信用户
     * 旧版本微信用户点击后将没有回应，开发者也不能正常接收到事件推送。
     * */
    fun onWxMenuScanCodeWaitEvent(e: WxMenuScanCodeWaitEvent): ReBaseMSg?

    /**
     * pic_sysphoto：弹出系统拍照发图的事件推送
     *
     * 仅支持微信iPhone5.4.1以上版本，和Android5.4以上版本的微信用户
     * 旧版本微信用户点击后将没有回应，开发者也不能正常接收到事件推送。
     * */
    fun onWxMenuPhotoEvent(e: WxMenuPhotoEvent): ReBaseMSg?

    /**
     * pic_photo_or_album：弹出拍照或者相册发图的事件推送
     *
     * 仅支持微信iPhone5.4.1以上版本，和Android5.4以上版本的微信用户
     * 旧版本微信用户点击后将没有回应，开发者也不能正常接收到事件推送。
     * */
    fun onWxMenuPhotoOrAlbumEvent(e: WxMenuPhotoOrAlbumEvent): ReBaseMSg?

    /**
     * pic_weixin：弹出微信相册发图器的事件推送
     *
     * 仅支持微信iPhone5.4.1以上版本，和Android5.4以上版本的微信用户
     * 旧版本微信用户点击后将没有回应，开发者也不能正常接收到事件推送。
     * */
    fun onWxMenuWxAlbumEvent(e: WxMenuWxAlbumEvent): ReBaseMSg?

    /**
     * location_select：弹出地理位置选择器的事件推送
     *
     * 仅支持微信iPhone5.4.1以上版本，和Android5.4以上版本的微信用户
     * 旧版本微信用户点击后将没有回应，开发者也不能正常接收到事件推送。
     * */
    fun onWxMenuLocationEvent(e: WxMenuLocationEvent): ReBaseMSg?

    /**
     * 点击菜单跳转小程序的事件推送
     *
     * 仅支持微信iPhone5.4.1以上版本，和Android5.4以上版本的微信用户
     * 旧版本微信用户点击后将没有回应，开发者也不能正常接收到事件推送。
     * */
    fun onWxMenuMiniEvent(e: WxMenuMiniEvent): ReBaseMSg?

    /**
     * 群发结果通知推送
     *
     * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html
     *
     * 在公众平台网站上，为订阅号提供了每天一条的群发权限，为服务号提供每月（自然月）4条的群发权限。而对于某些具备开发能力的公众号运营者，
     * 可以通过高级群发接口，实现更灵活的群发能力。
     *
     * 由于群发任务提交后，群发任务可能在一定时间后才完成，因此，群发接口调用时，仅会给出群发任务是否提交成功的提示，若群发任务提交成功，
     * 则在群发任务结束时，会向开发者在公众平台填写的开发者URL（callback URL）推送事件。需要注意，由于群发任务彻底完成需要较长时间，
     * 将会在群发任务即将完成的时候，就推送群发结果，此时的推送人数数据将会与实际情形存在一定误差
     *
     *
     * 群发图文消息的过程如下：
     * 首先，预先将图文消息中需要用到的图片，使用上传图文消息内图片接口，上传成功并获得图片 URL；
     * 上传图文消息素材，需要用到图片时，请使用上一步获取的图片 URL；
     * 使用对用户标签的群发，或对 OpenID 列表的群发，将图文消息群发出去，群发时微信会进行原创校验，并返回群发操作结果；
     * 在上述过程中，如果需要，还可以预览图文消息、查询群发状态，或删除已群发的消息等。
     *
     * 群发图片、文本等其他消息类型的过程如下：
     * 如果是群发文本消息，则直接根据下面的接口说明进行群发即可；
     * 如果是群发图片、视频等消息，则需要预先通过素材管理接口准备好 mediaID。
     * 关于群发时使用is_to_all为true使其进入公众号在微信客户端的历史消息列表：
     *
     * 使用is_to_all为true且成功群发，会使得此次群发进入历史消息列表。
     * 为防止异常，认证订阅号在一天内，只能使用is_to_all为true进行群发一次，或者在公众平台官网群发（不管本次群发是对全体还是对某个分组）一次。
     * 以避免一天内有2条群发进入历史消息列表。
     * 类似地，服务号在一个月内，使用is_to_all为true群发的次数，加上公众平台官网群发（不管本次群发是对全体还是对某个分组）的次数，最多只能是4次。
     * 设置is_to_all为false时是可以多次群发的，但每个用户只会收到最多4条，且这些群发不会进入历史消息列表。
     * 另外，请开发者注意，本接口中所有使用到media_id的地方，现在都可以使用素材管理中的永久素材media_id了。请但注意，使用同一个素材群发出去的
     * 链接是一样的，这意味着，删除某一次群发，会导致整个链接失效。
     * */
    fun onWxWxMassSendFinishEvent(e: WxMassSendFinishEvent): ReBaseMSg?

    /**
     * 在模版消息发送任务完成后，微信服务器会将是否送达成功作为通知，发送到开发者中心中填写的服务器配置地址中。
     *
     * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Template_Message_Interface.html
     *
     * @param status success 送达成功;
     * failed:user block: 送达由于用户拒收（用户设置拒绝接收公众号消息）;
     * failed: system failed: 发送状态为发送失败（非用户拒绝）
     * */
    fun onWxTemplateSendJobFinish(e: WxTemplateSendJobFinish): ReBaseMSg?

}

class DefaultOAEventHandler: IOAEventHandler {
    override fun onWxSubscribeEvent(e: WxSubscribeEvent) = onDefault(e)

    override fun onWxUnsubscribeEvent(e: WxUnsubscribeEvent) = onDefault(e)

    override fun onWxScanSubscribeEvent(e: WxScanSubscribeEvent) = onDefault(e)

    override fun onWxScanEvent(e: WxScanEvent) = onDefault(e)

    override fun onWxLocationEvent(e: WxLocationEvent) = onDefault(e)

    override fun onWxMenuClickEvent(e: WxMenuClickEvent) = onDefault(e)

    override fun onWxMenuViewEvent(e: WxMenuViewEvent) = onDefault(e)

    override fun onWxMenuScanCodePushEvent(e: WxMenuScanCodePushEvent) = onDefault(e)

    override fun onWxMenuScanCodeWaitEvent(e: WxMenuScanCodeWaitEvent) = onDefault(e)

    override fun onWxMenuPhotoEvent(e: WxMenuPhotoEvent) = onDefault(e)

    override fun onWxMenuPhotoOrAlbumEvent(e: WxMenuPhotoOrAlbumEvent) = onDefault(e)

    override fun onWxMenuWxAlbumEvent(e: WxMenuWxAlbumEvent) = onDefault(e)

    override fun onWxMenuLocationEvent(e: WxMenuLocationEvent) = onDefault(e)

    override fun onWxMenuMiniEvent(e: WxMenuMiniEvent) = onDefault(e)

    override fun onWxWxMassSendFinishEvent(e: WxMassSendFinishEvent) = onDefault(e)

    override fun onWxTemplateSendJobFinish(e: WxTemplateSendJobFinish) = onDefault(e)

    override fun onDefault(e: WxBaseEvent): ReBaseMSg? {
        return null
    }

}