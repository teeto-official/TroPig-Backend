package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.common.model.SearchContext
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.response.BookmarkContentResponse
import com.tropig.backend.contents.service.BookmarkContentService
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.TagService
import com.tropig.backend.member.service.CreatorService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@ApiController
@RequestMapping("/api/bookmark")
class BookmarkController(
    private val bookmarkContentService: BookmarkContentService,
    private val creatorService: CreatorService,
    private val tagService: TagService,
    private val contentService: ContentService,
) {

    @RequireAuth
    @GetMapping("/{type}")
    fun getBookmarkList(
        @LoginMember authMember: AuthMember,
        @Parameter(name = "type", description = "시나리오/자료", `in` = ParameterIn.PATH)
        @PathVariable
        type: ContentType,
        @Parameter(name = "cursorId", description = "커서 bookmarkId", `in` = ParameterIn.QUERY)
        cursorId: Long? = null,
        @Parameter(name = "cursorCreatedAt", description = "커서 등록일자", `in` = ParameterIn.QUERY)
        cursorCreatedAt: LocalDateTime? = null,
        @Parameter(
            name = "sortMode", description = "정렬 순서", `in` = ParameterIn.QUERY,
            schema = Schema(
                allowableValues = ["LATEST", "OLDEST"],
                defaultValue = "LATEST"
            )
        )
        sortMode: SortMode = SortMode.LATEST,
        @Parameter(name = "size", description = "페이지 크기", `in` = ParameterIn.QUERY)
        size: Int = 15,
    ): CursorSlice<BookmarkContentResponse> {
        val bookmarkList = bookmarkContentService.getBookmarkList(authMember.memberId, type, cursorId, cursorCreatedAt, sortMode, size)

        return bookmarkList.mapWith(
            buildContext = { items ->
                val contentIds = items.map { it.id }
                val writerIds = items.map { it.memberId }.distinct()

                val nickByMemberId = creatorService.getWritersName(writerIds)
                val tagsByContentId = tagService.findTagNamesByContentIds(contentIds)
                val thumbnailPaths = contentService.getThumbnailPath(contentIds)
                    .associateBy({ it.contentId }, { it.path })

                SearchContext(
                    nickByMemberId = nickByMemberId,
                    tagsByContentId = tagsByContentId,
                    bookmarkInfo = emptyMap(),
                    favoriteCounts = emptyMap(),
                    thumbnailPaths = thumbnailPaths,
                )
            }
        ) { content, ctx ->
            BookmarkContentResponse(
                id = content.id,
                alias = content.alias,
                title = content.title,
                rule = content.rule,
                genre = content.genre,
                writer = ctx.nickByMemberId[content.memberId] ?: "탈퇴한 작가입니다.",
                playerCountType = content.playerCountType,
                thumbnailPath = ctx.thumbnailPaths[content.id],
                tags = ctx.tagsByContentId[content.id].orEmpty(),
                updatedAt = content.updatedAt,
            )
        }
    }

    @RequireAuth
    @PostMapping("/{contentId}")
    fun insertBookmark(
        @LoginMember authMember: AuthMember,
        @Parameter(name = "contentId", description = "시나리오/자료 id", `in` = ParameterIn.PATH)
        @PathVariable
        contentId: Long,
    ) {
        bookmarkContentService.saveBookmark(authMember.memberId, contentId)
    }

    @RequireAuth
    @DeleteMapping("/{contentId}")
    fun deleteBookmark(
        @LoginMember authMember: AuthMember,
        @Parameter(name = "contentId", description = "시나리오/자료 id", `in` = ParameterIn.PATH)
        @PathVariable
        contentId: Long,
    ) {
        bookmarkContentService.deleteBookmark(authMember.memberId, contentId)
    }
}