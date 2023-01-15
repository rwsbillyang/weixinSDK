/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-13 17:27
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