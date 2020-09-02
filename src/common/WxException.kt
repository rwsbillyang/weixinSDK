package com.github.rwsbillyang.wxSDK.common

import java.lang.RuntimeException

class WxException(val msg: String? = null, cause: Throwable? = null ) : RuntimeException(cause)