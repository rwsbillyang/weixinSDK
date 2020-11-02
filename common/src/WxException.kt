package com.github.rwsbillyang.wxSDK

import java.lang.RuntimeException

class WxException(val msg: String? = null, cause: Throwable? = null ) : RuntimeException(cause)