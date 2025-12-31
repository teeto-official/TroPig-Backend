package com.tropig.backend.common.exception

sealed class BankAccountVerificationException (
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class InvalidBankAccountException(message: String) : BankAccountVerificationException(message)
class ExternalVerificationFailedException(message: String) : BankAccountVerificationException(message)