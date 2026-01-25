package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.common.model.SearchContext
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.request.CountSearchContentRequest
import com.tropig.backend.contents.model.request.CreateContentRequest
import com.tropig.backend.contents.model.request.SearchContentRequest
import com.tropig.backend.contents.model.response.*
import com.tropig.backend.contents.service.*
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.service.CreatorService
import com.tropig.backend.payment.service.PaymentContentService
import com.tropig.backend.payment.service.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@ApiController
@RequestMapping("/api/content")
class ContentController(
    private val contentService: ContentService,
    private val pickContentService: PickContentService,
    private val bookmarkContentService: BookmarkContentService,
    private val favoriteContentService: FavoriteContentService,
    private val creatorService: CreatorService,
    private val paymentContentService: PaymentContentService,
    private val tagService: TagService,
    private val s3Service: S3Service,
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
        val writerName = creatorService.getWritersName(contents.map { it.writerId })
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

    @PostMapping("/search/count")
    fun countSearchContent(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember?,
        @RequestBody request: CountSearchContentRequest,
    ): CountSearchContentResponse {
        val isAdult = authMember?.adult ?: false
        val tagIds = request.tags?.let { tagList ->
            val nameSet = tagList.toSet()
            tagService.findAllTags()
                .asSequence()
                .filter { it.name in nameSet }
                .map { it.id }
                .toList()
        }

        val dto = request.toCountDto(isAdult, tagIds)
        return contentService.countSearchContents(dto).toResponse()
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

        val tagIds = request.tags?.let { tagList ->
            val nameSet = tagList.toSet()

            tagService.findAllTags()
                .asSequence()
                .filter { it.name in nameSet }
                .map { it.id }
                .toList()
        }

        val dto = request.toDto(isAdult, type, tagIds)
        val contents = contentService.searchContents(dto)

        return contents.mapWith(
            buildContext = { items ->
                val contentIds = items.map { it.id }
                val writerIds = items.map { it.memberId }.distinct()

                val nickByMemberId = creatorService.getWritersName(writerIds)
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
                freeContent = content.freeContent,
            )
        }
    }

    @GetMapping("/tag")
    fun getSearchTagList(): SearchTagResponse {
        val tags = tagService.findAllTags().map {
            SearchTagResponse.TagResponse(it.id, it.name, it.type)
        }
        val genres = Genre.entries.map {
            SearchTagResponse.GenreResponse(it, it.displayName)
        }
        val rules = Rule.entries.map {
            SearchTagResponse.RuleResponse(it, it.displayName)
        }

        return SearchTagResponse(tags, genres, rules)
    }

    @GetMapping("/{alias}")
    fun getContent(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember?,
        @PathVariable
        alias: String,
    ): ContentDetailResponse {
        val content = contentService.findByAlias(alias)?.let {
            // 1. 작가이고 본인 작품일 경우 조회 가능 (작가는 항상 성인 인증이 되어 있음)
            val isCreatorAndOwnContent = authMember?.role == Role.CREATOR && authMember.memberId == it.memberId

            // 2. 구입한 유저일 경우 조회 가능 (추후 추가)
            // val isPurchased = authMember?.let { paymentService.isContentPurchased(it.memberId, it.id) } ?: false

            // 3. 성인 콘텐츠 조회 제한: adult가 false이고 콘텐츠가 adult인 경우 조회 불가
            // 작가이고 본인 작품이 아닌 경우에만 체크 (작가는 항상 성인 인증이 되어 있으므로)
            val isAdultContentBlocked = authMember?.adult == false && it.adult

            if (isAdultContentBlocked && !isCreatorAndOwnContent) {
                throw ContentException(
                    "성인 인증이 필요한 콘텐츠입니다.",
                    MessageCode.NOT_FOUND_CONTENT
                )
            }

            it
        } ?: throw NotFoundException(
            "해당 시나리오/자료를 찾을 수 없습니다.",
            MessageCode.NOT_FOUND_CONTENT
        )

        val writer = creatorService.getWriter(content.memberId)

        val tags = tagService.findByContentId(content.id)
        val bookmark = authMember?.let {
            bookmarkContentService.existsBookmark(it.memberId, content.id)
        } ?: false
        val purchased = authMember?.let { member ->
            val isPurchased = paymentContentService.isContentPurchased(member.memberId, content.id)
            if (isPurchased) {
                content.nonFreeContent?.let { purchase ->
                    s3Service.getFileAsString(purchase)
                }
            } else {
                null
            }
        }

        return content.toDetailResponse(
            writer,
            tags,
            purchased,
            bookmark,
        )

    }

    @PostMapping
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    fun createContent(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @RequestBody request: CreateContentRequest,
    ): Map<String, Long> {
        // CREATOR 권한 체크
        if (authMember.role != Role.CREATOR) {
            throw ContentException(
                message = "콘텐츠를 생성할 권한이 없습니다. CREATOR 권한이 필요합니다.",
                code = MessageCode.INCORRECT_ROLE
            )
        }

        val content = contentService.createContent(
            request = request,
            memberId = authMember.memberId,
            writerNickname = authMember.nickname,
        )

        return mapOf("id" to content.id)
    }
}