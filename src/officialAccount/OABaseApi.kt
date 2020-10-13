package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.common.WxApi

abstract class OABaseApi : WxApi(){
    override val base = "https://api.weixin.qq.com/cgi-bin"
    override fun accessToken() = OfficialAccount.OA.accessToken.get()
}