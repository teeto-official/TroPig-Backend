package com.tropig.backend.member.model.request

import com.tropig.backend.member.enums.SnsProvider

data class SignInRequest(val snsId: String, val snsProvider: SnsProvider, val email: String)
