package com.github.rwsbillyang.wxSDK.work


import kotlinx.serialization.*


/**
 * 企业应用API
 * https://work.weixin.qq.com/api/doc/90000/90135/90226
 * */
class AgentApi : WorkBaseApi(){
    override val group = "agent"

    companion object{
        const val LIST = "list"
        const val DETAIL = "get"
        const val SET_AGENT = "set"
    }

    /**
     * 获取access_token对应的应用列表
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90227
     * */
    fun list() = doGet(LIST, null)

    /**
     * 获取指定的应用详情
     * */
    fun detail(id: String) = doGet(DETAIL, mapOf("agentid" to id))

    /**
     * 设置应用
     * https://work.weixin.qq.com/api/doc/90000/90135/90228
     * */
    fun setAgent(body: Map<String, Any?>) = doPost(SET_AGENT,body)
}

