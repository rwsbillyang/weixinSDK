package com.github.rwsbillyang.wxSDK.common.msg


interface IEventHandler{
    /**
     *  缺省处理函数
     * */
    fun onDefault(e: WxBaseEvent): ReBaseMSg?
}