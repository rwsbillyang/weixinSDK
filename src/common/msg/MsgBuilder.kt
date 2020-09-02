package com.github.rwsbillyang.wxSDK.common.msg


class MsgBuilder(str: String? = null) {
    private var builder: StringBuilder = if(str.isNullOrBlank()) StringBuilder()else StringBuilder(str)

    fun append(str: String?) {
        builder.append(str)
    }

    fun insert(str: String?) {
        builder.insert(0, str)
    }

    fun addTag(tagName: String, text: String?) {
        if (text.isNullOrBlank()) {
            return
        }
        builder.append("<$tagName>$text</$tagName>\n")
    }

    fun addData(tagName: String, data: String?) {
        if (data.isNullOrBlank()) {
            return
        }
        builder.append("<$tagName><![CDATA[$data]]></$tagName>\n")
    }

    override fun toString(): String {
        return builder.toString()
    }
}
