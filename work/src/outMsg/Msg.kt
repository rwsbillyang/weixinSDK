/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-09-22 12:26
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

package com.github.rwsbillyang.wxSDK.work.outMsg

import com.github.rwsbillyang.wxSDK.msg.*

import kotlinx.serialization.Serializable


//应用给可见范围内的成员发送消息
//https://work.weixin.qq.com/api/doc/90000/90135/90236


interface IOutWxWorkMsg

@Serializable
class WxWorkTextMsg(
    val touser: String? = null, //指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。 特殊情况：指定为”@all”，则向该企业应用的全部成员发送
    val toparty: String? = null, //指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val totag: String? = null, //指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val agentid: Int, //企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
    val text: TextContent,
    val msgtype: String = MsgType.TEXT,
    val safe: Int = 0, //表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
    val enable_id_trans: Int = 0, //是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
    val enable_duplicate_check: Int = 0,//是否开启重复消息检查，0表示否，1表示是，默认0
    val duplicate_check_interval: Int = 1800, //是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
):IOutWxWorkMsg {
    //其中text参数的content字段可以支持换行、以及A标签，即可打开自定义的网页（可参考以上示例代码）(注意：换行符请用转义过的\n)
    constructor(content: String, agentId: String, touser: String?) :
            this(touser, null, null,  agentId.toInt(), TextContent(content))
}

@Serializable
class TextCard(val title: String, val description: String, val url: String)

@Serializable
class WxWorkTextCardMsg(
    val touser: String? = null, //指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。 特殊情况：指定为”@all”，则向该企业应用的全部成员发送
    val toparty: String? = null, //指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val totag: String? = null, //指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val agentid: Int, //企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
    val textcard: TextCard,
    val msgtype: String = "textcard",//配置成了缺省值不编码，故不能缺省
    val safe: Int = 0, //表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
    val enable_id_trans: Int = 0, //是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
    val enable_duplicate_check: Int = 0,//是否开启重复消息检查，0表示否，1表示是，默认0
    val duplicate_check_interval: Int = 1800, //是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
):IOutWxWorkMsg  {
    constructor(title: String, description: String, url: String, agentId: String, touser: String?) :
            this(touser, null, null, agentId.toInt(), TextCard(title, description, url))
}

@Serializable
class WxWorkImageMsg(
    val touser: String? = null, //指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。 特殊情况：指定为”@all”，则向该企业应用的全部成员发送
    val toparty: String? = null, //指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val totag: String? = null, //指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val agentid: Int, //企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
    val image: ImageContent,
    val msgtype: String = MsgType.IMAGE,
    val safe: Int = 0, //表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
    val enable_id_trans: Int = 0, //是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
    val enable_duplicate_check: Int = 0,//是否开启重复消息检查，0表示否，1表示是，默认0
    val duplicate_check_interval: Int = 1800, //是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
) : IOutWxWorkMsg {
    //图片媒体文件id，可以调用上传临时素材接口获取
    constructor(mediaId: String, agentId: String, touser: String?) :
            this(touser, null, null, agentId.toInt(), ImageContent(mediaId))
}

@Serializable
class WxWorkVoiceMsg(
    val touser: String? = null, //指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。 特殊情况：指定为”@all”，则向该企业应用的全部成员发送
    val toparty: String? = null, //指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val totag: String? = null, //指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数

    val agentid: Int, //企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
    val voice: VoiceContent,
    val msgtype: String = MsgType.VOICE,
    val safe: Int = 0, //表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
    val enable_id_trans: Int = 0, //是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
    val enable_duplicate_check: Int = 0,//是否开启重复消息检查，0表示否，1表示是，默认0
    val duplicate_check_interval: Int = 1800, //是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
):IOutWxWorkMsg  {
    //语音文件id，可以调用上传临时素材接口获取
    constructor(mediaId: String, agentId: String, touser: String?) :
            this(touser, null, null,  agentId.toInt(), VoiceContent(mediaId))
}

@Serializable
class WxWorkVideoMsg(
    val touser: String? = null, //指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。 特殊情况：指定为”@all”，则向该企业应用的全部成员发送
    val toparty: String? = null, //指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val totag: String? = null, //指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val agentid: Int, //企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
    val video: VideoContent,
    val msgtype: String = MsgType.VIDEO,
    val safe: Int = 0, //表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
    val enable_id_trans: Int = 0, //是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
    val enable_duplicate_check: Int = 0,//是否开启重复消息检查，0表示否，1表示是，默认0
    val duplicate_check_interval: Int = 1800, //是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
):IOutWxWorkMsg  {

    constructor(mediaId: String, agentId: String, touser: String?, title: String? = null, description: String? = null) :
            this(touser, null, null,  agentId.toInt(), VideoContent(mediaId, title, description))
}

@Serializable
class WxWorkNewsMsg(
    val touser: String? = null, //指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。 特殊情况：指定为”@all”，则向该企业应用的全部成员发送
    val toparty: String? = null, //指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val totag: String? = null, //指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val agentid: Int, //企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
    val news: NewsContent,
    val msgtype: String = MsgType.NEWS,
    val safe: Int = 0, //表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
    val enable_id_trans: Int = 0, //是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
    val enable_duplicate_check: Int = 0,//是否开启重复消息检查，0表示否，1表示是，默认0
    val duplicate_check_interval: Int = 1800, //是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
):IOutWxWorkMsg  {
    constructor(articles: List<ArticleItem>, agentId: String, touser: String) :
            this(touser, null, null,  agentId.toInt(), NewsContent(articles))
}


@Serializable
class WxWorkMpNewsMsg(
    val touser: String? = null, //指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。 特殊情况：指定为”@all”，则向该企业应用的全部成员发送
    val toparty: String? = null, //指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val totag: String? = null, //指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为”@all”时忽略本参数
    val agentid: Int, //企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
    val mpnews: MpNewsContent,
    val msgtype: String = MsgType.NEWS,
    val safe: Int = 0, //表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
    val enable_id_trans: Int = 0, //是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
    val enable_duplicate_check: Int = 0,//是否开启重复消息检查，0表示否，1表示是，默认0
    val duplicate_check_interval: Int = 1800, //是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
):IOutWxWorkMsg  {
    constructor(mediaId: String, agentId: String, touser: String) :
            this(touser, null, null, agentId.toInt(), MpNewsContent(mediaId))
}


//其它消息类型：TODO