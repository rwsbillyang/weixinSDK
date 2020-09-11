package com.github.rwsbillyang.wxSDK.work


/**
 * 企业应用API
 * https://work.weixin.qq.com/api/doc/90000/90135/90226
 * */
object AgentApi : WorkBaseApi(){
    override val group = "agent"
    
    /**
     * 获取access_token对应的应用列表
     *
     * https://work.weixin.qq.com/api/doc/90000/90135/90227
     * */
    fun list() = doGet("list", null)

    /**
     * 获取指定的应用详情
     * */
    fun detail(id: String) = doGet("get", mapOf("agentid" to id))

    /**
     * 设置应用
     * https://work.weixin.qq.com/api/doc/90000/90135/90228
     * */
    fun setAgent(body: Map<String, Any?>) = doPost("set",body)
}

