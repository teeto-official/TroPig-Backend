package com.tropig.backend.common.exception

import com.tropig.backend.common.enums.MessageCode

class NotFoundException(
    message: String,
    val code: MessageCode,
): RuntimeException(message)