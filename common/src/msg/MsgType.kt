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


    const val REVOKE = "revoke" //企业微信中会话存档中的撤回消息类型
    const val AGREE = "agree" //企业微信中会话存档中 同意存档
    const val DISAGREE = "disagree"//企业微信中会话存档中 不同意存档
    const val EMOTION = "emotion"//企业微信
    const val FILE = "file"//企业微信
    const val CARD = "card"//企业微信名片
    const val WEAPP = "weapp" //企业微信 小程序消息
    const val CHAT_RECORD = "chatrecord" //企业微信 会话记录消息
    const val TODO = "todo"  //企业微信 待办消息
    const val VOTE = "vote" //企业微信 投票消息
    const val COLLECT = "collect"//企业微信 投票消息
    const val RED_PACKET = "redpacket" //企业微信 红包消息
    const val MEETING = "meeting"//企业微信 会议邀请消息
    const val DOC = "docmsg" //企业微信  在线文档消息
    const val MARKDOWN = "markdown" //企业微信  MarkDown格式消息
    const val CALENDAR = "calendar"//企业微信 日程消息
    const val MIXED = "mixed" //企业微信 混合消息
    const val MEETING_VOICE_CALL = "meeting_voice_call"//企业微信 音频存档消息
    const val VOIP_DOC_SHARE = "voip_doc_share"//企业微信 音频共享文档消息
    const val EXTERNAL_RED_PACKET = "external_redpacket"//互通红包消息 出现在本企业与外部企业群聊发送的红包、或者本企业与微信单聊、群聊发送的红包消息场景下。

    const val DEFAULT = "defaultMsg" //官方API中无此类型，为了方便对消息默认处理而添加
}

