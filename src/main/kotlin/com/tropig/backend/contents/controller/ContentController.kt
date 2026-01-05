package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.common.model.SearchContext
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.request.SearchContentRequest
import com.tropig.backend.contents.model.response.PickContentResponse
import com.tropig.backend.contents.model.response.SearchContentResponse
import com.tropig.backend.contents.service.*
import com.tropig.backend.member.service.MemberService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@ApiController
@RequestMapping("/api/content")
class ContentController(
    private val contentService: ContentService,
    private val pickContentService: PickContentService,
    private val bookmarkContentService: BookmarkContentService,
    private val favoriteContentService: FavoriteContentService,
    private val memberService: MemberService,
    private val tagService: TagService,
) {

    @GetMapping("/pick/{type}")
    fun getPickContent(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember?,
        @PathVariable
        type: ContentType
    ): List<PickContentResponse> {
        val isAdult = authMember?.adult ?: false
        val pickContents = pickContentService.getAllPickContent()
        val contents = contentService.getPickContentsByType(type, isAdult, pickContents.map { it.contentId })
        val writerName = memberService.getWritersName(contents.map { it.writerId })
        val contentsMap = contents.associateBy { it.id }

        return pickContents.mapNotNull {
            val content = contentsMap[it.contentId] ?: return@mapNotNull null

            PickContentResponse(
                title = content.title,
                alias = content.alias,
                thumbnailPath = content.thumbnailPath,
                writer = writerName[content.writerId] ?: "",
                tags = content.tags,
                orderNo = it.orderNo
            )
        }.sortedBy { it.orderNo }
    }

    @PostMapping("/search/{type}")
    fun searchContent(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember?,
        @PathVariable
        type: ContentType,
        @RequestBody request: SearchContentRequest,
    ): CursorSlice<SearchContentResponse> {
        val isAdult = authMember?.adult ?: false
        val memberId = authMember?.memberId
        val contents = contentService.searchContents(request, isAdult, type)

        return contents.mapWith(
            buildContext = { items ->
                val contentIds = items.map { it.id }
                val writerIds = items.map { it.memberId }.distinct()

                val nickByMemberId = memberService.getWritersName(writerIds)
                val tagsByContentId = tagService.findTagNamesByContentIds(contentIds)
                val bookmarksInfo = bookmarkContentService.getBookmarkInfo(memberId, contentIds)
                val favoriteCountsByContentId = favoriteContentService.getFavoriteCountByContentIds(contentIds)
                val thumbnailPaths = contentService.getThumbnailPath(contentIds)
                    .associateBy({ it.contentId }, { it.path })

                SearchContext(
                    nickByMemberId = nickByMemberId,
                    tagsByContentId = tagsByContentId,
                    bookmarkInfo = bookmarksInfo,
                    favoriteCounts = favoriteCountsByContentId,
                    thumbnailPaths = thumbnailPaths,
                )
            }
        ) { content, ctx ->
            val bookmarkInfo = ctx.bookmarkInfo[content.id]
            SearchContentResponse(
                id = content.id,
                alias = content.alias,
                title = content.title,
                type = content.type,
                rule = content.rule,
                genre = content.genre,
                writer = ctx.nickByMemberId[content.memberId] ?: "탈퇴한 작가입니다.",
                playerCountType = content.playerCountType,
                thumbnailPath = ctx.thumbnailPaths[content.id],
                tags = ctx.tagsByContentId[content.id].orEmpty(),
                isBookmark = bookmarkInfo?.bookmarked ?: false,
                bookmarkCount = bookmarkInfo?.bookmarkCount ?: 0L,
                favoriteCount = ctx.favoriteCounts[content.id] ?: 0L,
                publishedAt = content.publishedAt!!,
            )
        }
    }
}