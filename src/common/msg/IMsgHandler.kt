package com.github.rwsbillyang.wxSDK.common.msg



interface IMsgHandler{
    fun onDefault(msg: WxBaseMsg): ReBaseMSg?
}