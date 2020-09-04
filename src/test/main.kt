package com.github.rwsbillyang.wxSDK.test

fun main(args: Array<String>) {
    val workTest = WorkTest()

    workTest.testUrl()
    workTest.testMsgDecrypt()
    workTest.testEncryptReMsg()

}