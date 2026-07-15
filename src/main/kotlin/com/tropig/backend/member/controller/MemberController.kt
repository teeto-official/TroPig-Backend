package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.response.CountSearchContentResponse
import com.tropig.backend.contents.model.response.SearchContentResponse
import com.tropig.backend.contents.service.BookmarkContentService
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.FavoriteContentService
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.contents.service.TagService
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.model.request.SignInRequest
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.request.UpdateMemberRequest
import com.tropig.backend.member.model.response.MemberProfileResponse
import com.tropig.backend.member.model.response.MemberResponse
import com.tropig.backend.member.model.response.MemberSearchResponse
import com.tropig.backend.member.model.response.TokenResponse
import com.tropig.backend.member.service.CreatorService
import com.tropig.backend.member.service.MemberService
import com.tropig.backend.member.service.RedisRefreshTokenService
import com.tropig.backend.member.service.jwt.JwtTokenProvider
import com.tropig.backend.member.service.jwt.TokenBlacklistService
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
    private val creatorService: CreatorService,
    private val tagService: TagService,
    private val bookmarkContentService: BookmarkContentService,
    private val favoriteContentService: FavoriteContentService,
    private val s3Service: S3Service,
    private val tokenBlacklistService: TokenBlacklistService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisRefreshTokenService: RedisRefreshTokenService,
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
    @PostMapping("/sign-out")
    fun signOut(
        @LoginMember authMember: AuthMember,
        @RequestHeader("Authorization") authorizationHeader: String,
    ): ResponseEntity<Void> {
        val token = authorizationHeader.removePrefix("Bearer ")
        val expiration = jwtTokenProvider.getExpiration(token)
        tokenBlacklistService.blacklist(token, expiration)
        redisRefreshTokenService.deleteByMemberId(authMember.memberId)
        return ResponseEntity.noContent().build()
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
//        val favoriteGenreIds = memberService.parseFavoriteIds(updatedMember.favoriteGenres, OptionType.GENRE)
//        val favoriteRuleIds = memberService.parseFavoriteIds(updatedMember.favoriteRules, OptionType.RULE)
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

    @GetMapping("/search")
    @RequireAuth
    fun searchMembers(@RequestParam keyword: String): List<MemberSearchResponse> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }

        return memberService.searchMembersByNickname(trimmed).map { member ->
            MemberSearchResponse(
                id = member.id,
                nickname = member.nickname,
                profileUrl = s3Service.toUrl(member.profile),
            )
        }
    }

    @GetMapping("/profile/{nickname}")
    fun getMemberProfile(@PathVariable nickname: String): MemberProfileResponse {
        val member = memberService.getMemberProfile(nickname) ?: throw NotFoundException(
            message = "회원 정보를 찾을 수 없습니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )
        return MemberProfileResponse.from(member)
            .let { it.copy(profile = s3Service.toUrl(it.profile)) }
    }

    @GetMapping("/profile/{nickname}/content/count")
    fun getMemberContentCount(
        @LoginMember authMember: AuthMember?,
        @PathVariable nickname: String,
    ): CountSearchContentResponse {
        val member = memberService.getMemberProfile(nickname) ?: throw NotFoundException(
            message = "회원 정보를 찾을 수 없습니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )

        if (member.role != Role.CREATOR) {
            return CountSearchContentResponse(scenarioCount = 0L, resourceCount = 0L)
        }

        val isAdult = authMember?.adult ?: false
        val scenarioCount = contentService.getPublishedContentCountByMember(member.id, ContentType.SCENARIO, isAdult)
        val resourceCount = contentService.getPublishedContentCountByMember(member.id, ContentType.RESOURCE, isAdult)
        return CountSearchContentResponse(scenarioCount = scenarioCount, resourceCount = resourceCount)
    }

    @GetMapping("/profile/{nickname}/content")
    fun getMemberContents(
        @LoginMember authMember: AuthMember?,
        @PathVariable nickname: String,
        @RequestParam type: ContentType,
        @RequestParam(defaultValue = "15") size: Int,
        @RequestParam(defaultValue = "0") cursorContentId: Long,
    ): CursorSlice<SearchContentResponse> {
        val member = memberService.getUserByNickname(nickname) ?: throw NotFoundException(
            message = "회원 정보를 찾을 수 없습니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )

        if (member.role != Role.CREATOR) {
            return CursorSlice(items = emptyList(), hasNext = false)
        }

        val isAdult = authMember?.adult ?: false
        val allContents = contentService.getPublishedContentsByMember(member.id, type, isAdult)
            .sortedByDescending { it.publishedAt }
            .filter { cursorContentId == 0L || it.id < cursorContentId }

        val hasNext = allContents.size > size
        val contents = allContents.take(size)

        val contentIds = contents.map { it.id }
        val thumbnailPaths = contentService.getThumbnailPath(contentIds)
            .associateBy({ it.contentId }, { s3Service.toUrl(it.path) })
        val tags = tagService.findTagNamesByContentIds(contentIds)
        val bookmarkInfo = authMember?.let {
            bookmarkContentService.getBookmarkInfo(it.memberId, contentIds)
        } ?: emptyMap()
        val favoriteCounts = favoriteContentService.getFavoriteCountByContentIds(contentIds)

        val lastContent = contents.lastOrNull()

        return CursorSlice(
            items = contents.map {
                SearchContentResponse(
                    id = it.id,
                    alias = it.alias,
                    title = it.title,
                    type = it.type,
                    publishingType = if (type == ContentType.RESOURCE) it.publishingType else null,
                    ruleId = it.ruleId,
                    genreId = it.genreId,
                    writer = member.nickname,
                    playerCountType = it.playerCountType,
                    thumbnailPath = thumbnailPaths[it.id],
                    tags = tags[it.id] ?: emptyList(),
                    isBookmark = bookmarkInfo[it.id]?.bookmarked ?: false,
                    bookmarkCount = bookmarkInfo[it.id]?.bookmarkCount ?: 0L,
                    favoriteCount = favoriteCounts[it.id] ?: 0L,
                    publishedAt = it.publishedAt!!,
                    freeContent = Content.removeSpoilers(it.freeContent),
                    price = it.price,
                )
            },
            hasNext = hasNext,
            nextCursorId = if (hasNext) lastContent?.id else null,
            nextCursorDateAt = if (hasNext) lastContent?.publishedAt else null,
        )
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
