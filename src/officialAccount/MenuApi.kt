package com.github.rwsbillyang.wxSDK.officialAccount


class MenuApi: OABaseApi() {
    override val group = "menu"
    companion object{
        const val CREATE = "create"
        const val DETAIL = "get"
        const val SET_AGENT = "delete"
    }

    fun create() = doPost(CREATE)

}