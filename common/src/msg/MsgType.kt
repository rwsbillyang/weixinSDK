/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-16 12:02
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

package com.github.rwsbillyang.wxSDK.msg

/**
 * 各种消息类型汇总
 * */
object MsgType{
    const val TEXT = "text"
    const val IMAGE = "image"
    const val VOICE = "voice"
    const val MPVIDEO = "mpvideo"
    const val MPNEWS = "mpnews"
    const val WXCARD = "wxcard"

    const val MUSIC = "music"
    const val MENU = "msgmenu"
    const val NEWS = "news"
    const val MINI_PROGRAM = "miniprogrampage"


    const val VIDEO = "video"
    const val SHORT_VIDEO = "shortvideo"
    const val LOCATION = "location"
    const val LINK = "link"
    const val EVENT = "event"

    const val TRANSFER_TO_CUSTOMER_SERVICE = "transfer_customer_service"


    const val DEFAULT = "defaultMsg" //官方API中无此类型，为了方便对消息默认处理而添加
}
/**
 * 主动发的消息，包括群发消息、模板消息、客服消息，不是推送消息后然后系统立即自动回复给用户的消息，它们完全不同
 *
 * @property msgType 消息类型
 * */
interface IMsg {
    val msgType: String
}


object InEventType{
    const val SUBSCRIBE = "subscribe"
    const val UNSUBSCRIBE = "unsubscribe"
    const val SCAN = "SCAN"
    const val LOCATION = "LOCATION"
    const val CLICK = "CLICK"
    const val VIEW = "VIEW"
    const val SCAN_CODE_PUSH = "scancode_push"
    const val SCAN_CODE_WAIT_MSG = "scancode_waitmsg"
    const val PIC_SYS_PHOTO = "pic_sysphoto"
    const val PIC_PHOTO_OR_ALBUM = "pic_photo_or_album"
    const val PIC_WEIXIN = "pic_weixin"
    const val LOCATION_SELECT = "location_select"
    const val VIEW_MINI_PROGRAM = "view_miniprogram"
    const val MASS_SEND_JOB_FINISH = "MASSSENDJOBFINISH"
    const val TEMPLATE_SEND_JOB_FINISH = "TEMPLATESENDJOBFINISH"
    const val DEFAULT = "defaultEvent" //官方API中无此类型，为了方便对消息默认处理而添加
}