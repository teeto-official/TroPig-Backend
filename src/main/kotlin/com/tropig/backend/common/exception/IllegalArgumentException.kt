package com.tropig.backend.common.exception

import com.tropig.backend.common.enums.MessageCode

class IllegalArgumentException(
    message: String,
    val code: MessageCode,
): RuntimeException(message)