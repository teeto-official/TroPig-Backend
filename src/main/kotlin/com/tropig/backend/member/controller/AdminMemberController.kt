package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.member.service.MemberService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@ApiController
@RequestMapping("/admin/member")
@Tag(name = "Admin Member", description = "관리자 회원 API")
class AdminMemberController(
    private val memberService: MemberService,
) {

    @PostMapping("/migrate/favorite-tags")
    @Operation(
        summary = "즐겨찾기 태그 ID 마이그레이션",
        description = "favoriteGenres/favoriteRules에 저장된 구 enum 이름 값을 content_option ID로 일괄 변환합니다.",
    )
    fun migrateFavoriteTags(): Map<String, Int> {
        val migratedCount = memberService.migrateFavoriteTagsToIds()
        return mapOf("migratedCount" to migratedCount)
    }
}
