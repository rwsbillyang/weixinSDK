/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-09 20:56
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

package com.github.rwsbillyang.wxWork.isv


import com.github.rwsbillyang.wxSDK.work.isv.AuthCorpInfo
import com.github.rwsbillyang.wxSDK.work.isv.AuthInfo
import com.github.rwsbillyang.wxSDK.work.isv.DealerCorpInfo
import com.github.rwsbillyang.wxSDK.work.isv.RegisterCodeInfo
import kotlinx.serialization.Serializable


/**
 * 企业授权时保存的信息
 * @param _id suiteId/corpId
 * @param suiteId
 * @param corpId
 * @param status 授权成功则为1，被取消授权-1，系统管理员关闭则为0
 * @param time 获取到accessToken的时间
 * @param permanentCode 永久授权码
 * @param dealerInfo 来自于授权成功时获取到的企业信息
 * @param corpInfo 来自于授权成功时获取到的企业信息
 * @param authInfo 来自于授权成功时获取到的企业信息
 * @param registerInfo 来自于授权成功时获取到的企业信息
 * */
@Serializable
data class CorpInfo(
    val _id: String, //suiteId/corpId
    val suiteId: String,
    val corpId: String,
    val status: Int,
    val time: Long? = null, //获取到accessToken的时间
    val permanentCode: String? = null,
    val dealerInfo: DealerCorpInfo? = null,
    val corpInfo: AuthCorpInfo? = null,
    val authInfo : AuthInfo? = null,
    val registerInfo: RegisterCodeInfo?=null
){
    companion object{
        const val STATUS_ENABLED = 1
        const val STATUS_DISABLED = 0
        const val STATUS_CANCELED = -1
        fun id(suiteId: String, corpId: String) = "${suiteId}/$corpId"
    }
}

/**
 * 第三方进行用户oauth身份认证时，能获取到的用户信息，参见：https://work.weixin.qq.com/api/doc/90001/90143/91122
 * */
@Serializable
data class UserDetail3rd(
    val corpId: String?,
    val userId: String?,
    val name : String?,
    val gender: Int?,
    val avatar: String?,
    val qrCode: String?,
    var _id: String? = null, //corpId/userId
)
/**
 * 第三方应用的配置信息
 * ticket由腾讯推送过来
 *
 * 需手工添加到数据库中，或程序中指定
 * */
@Serializable
data class SuiteConfig(
    val _id: String, //suiteId
    val status: Int,
    val secret: String,
    val token: String,
    val encodingAESKey: String? = null,
    val enableJsSdk: Boolean,
    val privateKeyFilePath: String? = null
){
    companion object{
        const val STATUS_ENABLED = 1
        const val STATUS_DISABLED = 0
        const val STATUS_CANCELED = -1
    }
}