package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.model.request.SignInRequest
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.request.UpdateMemberRequest
import com.tropig.backend.member.model.response.MemberResponse
import com.tropig.backend.member.model.response.TokenResponse
import com.tropig.backend.member.service.MemberService
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ApiController
@RequestMapping("/api/member")
class MemberController(
    private val memberService: MemberService,
    private val contentService: ContentService,
    private val s3Service: S3Service,
) {
    @PostMapping("/sign-up")
    fun createUser(@RequestBody request: SignUpRequest): ResponseEntity<Any> = try {
        val user = memberService.signUp(request)
        ResponseEntity.status(HttpStatus.CREATED).body(user)
    } catch (e: IllegalArgumentException) {
        ResponseEntity.badRequest().body(mapOf("error" to e.message))
    }

    @PostMapping("/sign-in")
    fun loginUser(@RequestBody request: SignInRequest): TokenResponse {
        val member = memberService.signIn(request)
        return member
    }

    @RequireAuth
    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateUser(@LoginMember authMember: AuthMember, @ModelAttribute request: UpdateMemberRequest): MemberResponse {
        val member = memberService.getUserByEmail(authMember.email) ?: throw NotFoundException(
            message = "회원 정보를 찾을 수 없습니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )

        val memberAuthInfo = memberService.findMemberAuthInfo(memberId = member.id)
        val updatedMember = memberService.updateUser(member, request)
        return MemberResponse.from(updatedMember, memberAuthInfo)
            .let { it.copy(profile = s3Service.toUrl(it.profile)) }
    }

    @RequireAuth
    @GetMapping
    fun findUser(@LoginMember authMember: AuthMember): MemberResponse {
        val member = memberService.getUserByEmail(authMember.email) ?: throw NotFoundException(
            message = "회원 정보를 찾을 수 없습니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )

        val memberAuthInfo = memberService.findMemberAuthInfo(authMember.memberId)
        return MemberResponse.from(member, memberAuthInfo)
            .let { it.copy(profile = s3Service.toUrl(it.profile)) }
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestHeader("Authorization") authorizationHeader: String?): ResponseEntity<Any> {
        return try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Invalid authorization header"))
            }
            val refreshToken = authorizationHeader.substring("Bearer ".length)
            val memberId = memberService.validateRefreshToken(refreshToken)
            val member = memberService.getUserById(memberId) ?: throw NotFoundException(
                message = "회원 정보를 찾을 수 없습니다.",
                code = MessageCode.NOT_FOUND_MEMBER,
            )

            ResponseEntity.ok(memberService.refreshToken(member))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to (e.message ?: "Invalid token")))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to (e.message ?: "Member not found")))
        }
    }

    @RequireAuth
    @DeleteMapping
    @Transactional
    fun withdrawMember(@LoginMember authMember: AuthMember) {
        val deletedMember = memberService.deleteUser(authMember.memberId) ?: throw NotFoundException(
            message = "탈퇴처리할 유저가 없습니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )
        if (deletedMember.role == Role.CREATOR) {
            contentService.updateContentToDelete(authMember.memberId)
        }
    }
}
