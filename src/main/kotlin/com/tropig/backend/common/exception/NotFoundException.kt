package com.tropig.backend.common.exception

import com.tropig.backend.common.enums.MessageCode

class NotFoundException(
    message: String,
    code: MessageCode,
): RuntimeException(message)