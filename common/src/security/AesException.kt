package com.github.rwsbillyang.wxSDK.security

/**
 * @copyright Copyright (c) 1998-2014 Tencent Inc.
 */
class AesException internal constructor(
    val code: Int
) : Exception(getMessage(code)) {

    companion object {
        const val OK = 0
        const val ValidateSignatureError = -40001
        const val ParseXmlError = -40002
        const val ComputeSignatureError = -40003
        const val IllegalAesKey = -40004
        const val ValidateAppidError = -40005
        const val EncryptAESError = -40006
        const val DecryptAESError = -40007
        const val IllegalBuffer = -40008
//        const val EncodeBase64Error = -40009
//        const val DecodeBase64Error = -40010
//        const val GenReturnXmlError = -40011
        private fun getMessage(code: Int): String? {
            return when (code) {
                ValidateSignatureError -> "签名验证错误"
                ParseXmlError -> "xml解析失败"
                ComputeSignatureError -> "sha加密生成签名失败"
                IllegalAesKey -> "SymmetricKey非法"
                ValidateAppidError -> "appid校验失败"
                EncryptAESError -> "aes加密失败"
                DecryptAESError -> "aes解密失败"
                IllegalBuffer -> "解密后得到的buffer非法"
                else -> null // cannot be
            }
        }
    }

}
