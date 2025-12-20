package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.model.request.SignInRequest
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.request.UpdateMemberRequest
import com.tropig.backend.member.model.response.MemberResponse
import com.tropig.backend.member.model.response.TokenResponse
import com.tropig.backend.member.service.MemberService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@ApiController
@RequestMapping("/api/member")
class MemberController(
    private val memberService: MemberService
) {
    @PostMapping("/sign-up")
    fun createUser(
        @RequestBody request: SignUpRequest
    ): ResponseEntity<Any> {
        return try {
            val user = memberService.signUp(request)
            ResponseEntity.status(HttpStatus.CREATED).body(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/sign-in")
    fun loginUser(
        @RequestBody request: SignInRequest
    ): TokenResponse {
        val member = memberService.signIn(request)
        return member
    }

    @RequireAuth
    @PutMapping
    fun updateUser(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @RequestBody request: UpdateMemberRequest,
    ): MemberResponse {
        val member = memberService.getUserByEmail(authMember.email) ?: throw NotFoundException(
            message = "회원 정보를 찾을 수 없습니다.",
            code = MessageCode.NOT_FOUND_MEMBER
        )

        val updatedMember = memberService.updateUser(member, request)
        return MemberResponse.from(updatedMember)
    }

    @RequireAuth
    @GetMapping
    fun findUser(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
    ): MemberResponse {
        val member = memberService.getUserByEmail(authMember.email) ?: throw NotFoundException(
            message = "회원 정보를 찾을 수 없습니다.",
            code = MessageCode.NOT_FOUND_MEMBER
        )
        return MemberResponse.from(member)
    }
}
