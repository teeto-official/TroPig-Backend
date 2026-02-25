package com.tropig.backend.common.exception

import com.tropig.backend.common.enums.MessageCode

class MemberException(message: String, val code: MessageCode) : RuntimeException(message)
