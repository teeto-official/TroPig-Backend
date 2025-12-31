package com.tropig.backend.common.exception

import com.tropig.backend.common.enums.MessageCode

class AuthenticatedException(
    message: String,
    code: MessageCode,
): RuntimeException(message)