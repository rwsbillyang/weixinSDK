/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-22 15:03
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

package com.github.rwsbillyang.wxOA.msg



import com.github.rwsbillyang.wxUser.fakeRpc.PayNotifierType
import kotlinx.serialization.Serializable

/**
 * 预置的配置，不同公众号不同配置
 *
{
"_id": "wx0f92cbee09e231f9",
"cfg":{"ExpireAlarm":"", "PaySuccess":"", "NewClick":"", "Statistics":"", "ActApply": "", "CancelActApply":"", "NewMsg":""，"ActApplyConfirm":""}
}
 * */

@Serializable
data class TemplateMsgConfig(
    val _id: String, // appId
    val templateIdMap: Map<String, String>, //key：通知消息类型， value：模板ID
    val urlMap: Map<String, String>? = null//key：通知消息类型， value：点击后的跳转url,  以http开始的路径
)


/**
 * 预置的模板消息模板库
 * @param id 模板编号
 * @param name 模板名称
 * @param detail 模板正文内容
 * */
class Template(
    val type: PayNotifierType,
    val id: String,
    val name: String,
    val detail: String
)
//
//val SupportedTemplates = listOf(
//    /*
//      亲，您的VIP会员即将过期，请续费，以免影响使用
//                账号名：wx_nick
//                用户ＩＤ：xxxxxxxx
//                服务项目：友客VIP
//                过期时间：2021-10-10 12:32:11
//                点击续费!
//    * */
//    Template(
//        PayNotifierType.ExpireAlarm, "TM00870", "帐号到期提醒",
//        """{{first.DATA}}
//到期时间：{{keynote1.DATA}}
//说明：{{keynote2.DATA}}
//{{remark.DATA}}"""
//    ),
//    /*
//    """尊敬的客户，您已成功完成续费。
//        商品名称：一年VIP
//        支付金额：￥299.00
//        到期时间：2022年12月31日
//        感谢您对友客的支持！"""
//    * */
//    Template(
//        PayNotifierType.PaySuccess, "OPENTM417231652", "续费成功提醒",
//        """{{first.DATA}}
//商品名称：{{keyword1.DATA}}
//支付金额：{{keyword2.DATA}}
//到期时间：{{keyword3.DATA}}
//{{remark.DATA}}""",
//    ),
//
//    /*
//     """你有新的访客到来
//访客姓名：张三丰
//访客目的：《文章标题》
//来访时间：2020年4月17日9:26
//点击查看详情！关闭通知请到 "我->个性开关"中关闭通知
//    * */
//    Template(
//        NotifierType.NewClick, "OPENTM418150125", "访客到访通知",
//        """{{first.DATA}}
//访客姓名：{{keyword1.DATA}}
//访客目的：{{keyword2.DATA}}
//来访时间：{{keyword3.DATA}}
//{{remark.DATA}}"""
//    ),
///*
//* 统计周报
//用户名：店主小王
//统计时间：2015年8月20日-2015年8月25日
//统计数据：总计货款300元，补贴260元，奖惩金0元
//如有任何疑问请联系客服，谢谢
//* */
//    Template(
//        NotifierType.Statistics, "OPENTM207454425", "用户账户统计通知",
//        """{{first.DATA}}
//用户名：{{keyword1.DATA}}
//统计时间：{{keyword2.DATA}}
//统计数据：{{keyword3.DATA}}
//{{remark.DATA}}"""
//    ),
//
//    /*
//   """活动有了新报名
//活动标题：清清网校园大使培训活动
//报名人：黄清清
//报名电话：1858822XXXX
//报名时间：2015年8月8日""" 留言
//* */
//    Template(
//        NotifierType.ActApply, "OPENTM207221836", "新报名通知",
//        """{{first.DATA}}
//活动标题：{{keyword1.DATA}}
//报名人：{{keyword2.DATA}}
//报名电话：{{keyword3.DATA}}
//{{remark.DATA}}"""
//    ),
//    /*
//   """取消报名
//活动主题：广场舞
//退出人：李四
//退出留言：金盘洗手，退出广场界了。
//别伤心，多一个位置留给下一个大妈"""
//* */
//    Template(
//        NotifierType.CancelActApply, "OPENTM204601374", "取消报名申请通知",
//        """{{first.DATA}}
//活动主题：{{keyword1.DATA}}
//退出人：{{keyword2.DATA}}
//退出留言：{{keyword3.DATA}}
//{{remark.DATA}}"""
//    ),
//    /*
//    """您好!有访客访对您发布的信息发起咨询。
//来访用户：张树坤
//访客留言：咨询房产相关事宜。
//回复访客请点击此处。"""
//    * */
//    Template(
//        NotifierType.NewMsg, "TM00846", "访客来咨询通知",
//        """{{first.DATA}}
//来访用户：{{user.DATA}}
//访客留言：{{ask.DATA}}
//{{remark.DATA}}"""
//    ),
//    /*
//   """你已成功报名活动
//活动名称：XXX院士创新教育讲座
//活动时间：2014年12月1日 14:30
//活动地点：教学楼01A203
//感谢你的参与，点击查看活动详情
//咨询电话：4008888888"""
//* */
//    Template( NotifierType.ActApplyConfirm, "OPENTM201292805", "活动报名成功通知",
//        """{{first.DATA}}
//活动名称：{{keyword1.DATA}}
//活动时间：{{keyword2.DATA}}
//活动地点：{{keyword3.DATA}}
//{{remark.DATA}}"""
//    ),
//
//    )
//
//
