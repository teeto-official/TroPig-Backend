package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.model.request.AccountAuthRequest
import com.tropig.backend.member.model.request.AdultAuthRequest
import com.tropig.backend.member.model.response.AccountAuthResponse
import com.tropig.backend.member.model.response.AdultAuthResponse
import com.tropig.backend.member.service.AuthService
import com.tropig.backend.member.service.MemberService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiController
@RequestMapping("/api/auth")
class AuthController(
    private val memberService: MemberService,
    private val authService: AuthService,
) {

    @RequireAuth
    @PostMapping("/account")
    suspend fun verifyAccount(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @RequestBody
        request: AccountAuthRequest
    ): AccountAuthResponse {
        // 문서상 birthdate와 businessRegistrationNumber는 동시에 쓰지 않는 것을 권장 :contentReference[oaicite:12]{index=12}
        if (request.birthdate != null && request.businessRegistrationNumber != null) {
            throw IllegalArgumentException("Provide either birthdate or businessRegistrationNumber, not both.")
        }
        return authService.updateAccountInfo(authMember.memberId, request)
    }

    @RequireAuth
    @PostMapping("/adult")
    suspend fun verifyAdult(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @RequestBody request: AdultAuthRequest
    ): AdultAuthResponse {
        val isAdult = authService.updateAdultInfo(authMember.memberId, request.identityVerificationId)
        return AdultAuthResponse(isAdult = isAdult)
    }
}