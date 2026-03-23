package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.exception.IllegalArgumentException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.common.model.SearchContext
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.model.request.CountSearchContentRequest
import com.tropig.backend.contents.model.request.SearchContentRequest
import com.tropig.backend.contents.model.response.*
import com.tropig.backend.contents.service.*
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.service.CreatorService
import com.tropig.backend.member.service.MemberService
import com.tropig.backend.payment.service.PaymentContentService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import org.springframework.web.bind.annotation.*

@ApiController
@RequestMapping("/api/content")
class ContentController(
    private val contentService: ContentService,
    private val pickContentService: PickContentService,
    private val bookmarkContentService: BookmarkContentService,
    private val favoriteContentService: FavoriteContentService,
    private val relatedContentService: RelatedContentService,
    private val creatorService: CreatorService,
    private val memberService: MemberService,
    private val paymentContentService: PaymentContentService,
    private val tagService: TagService,
    private val s3Service: S3Service,
) {

    @GetMapping("/pick/{type}")
    fun getPickContent(
        @LoginMember authMember: AuthMember?,
        @PathVariable
        type: ContentType,
    ): List<PickContentResponse> {
        val isAdult = authMember?.adult ?: false
        val pickContents = pickContentService.getAllPickContent()
        val contents = contentService.getPickContentsByType(type, isAdult, pickContents.map { it.contentId })
        val writerName = creatorService.getWritersName(contents.map { it.writerId })
        val contentsMap = contents.associateBy { it.id }
        val bookmarks = authMember?.let {
            bookmarkContentService.getBookmarkList(it.memberId, contentsMap.keys.toList())
        } ?: emptyMap()

        return pickContents.mapNotNull {
            val content = contentsMap[it.contentId] ?: return@mapNotNull null
            val isBookmark = bookmarks[it.contentId] != null

            PickContentResponse(
                id = content.id,
                title = content.title,
                alias = content.alias,
                thumbnailPath = s3Service.toUrl(content.thumbnailPath),
                writer = writerName[content.writerId] ?: "",
                tags = content.tags,
                orderNo = it.orderNo,
                rule = content.rule,
                playerCountType = content.playerCountType,
                isBookmark = isBookmark,
                publishingType = if (type == ContentType.RESOURCE) {
                    content.publishingType
                } else {
                    null
                },
                price = content.price,
            )
        }.sortedBy { it.orderNo }
    }

    @GetMapping("/newest/{type}")
    fun getNewestContent(
        @LoginMember authMember: AuthMember?,
        @PathVariable
        type: ContentType,
    ): List<PickContentResponse> {
        val isAdult = authMember?.adult ?: false
        val contents = contentService.getNewestContents(type, isAdult)
        val writerName = creatorService.getWritersName(contents.map { it.memberId })
        val contentIds = contents.map { it.id }
        val tags = contentService.getTags(contentIds)
        val thumbnails = contentService.getThumbnailPath(contentIds).associateBy { it.contentId }
        val bookmarks = authMember?.let {
            bookmarkContentService.getBookmarkList(it.memberId, contentIds)
        } ?: emptyMap()

        return contents.map {
            val isBookmark = bookmarks[it.id] != null
            PickContentResponse(
                id = it.id,
                title = it.title,
                alias = it.alias,
                thumbnailPath = s3Service.toUrl(thumbnails[it.id]?.path),
                writer = writerName[it.memberId] ?: "",
                tags = tags[it.id] ?: emptyList(),
                orderNo = 0,
                rule = it.rule,
                playerCountType = it.playerCountType,
                isBookmark = isBookmark,
                publishingType = if (type == ContentType.RESOURCE) it.publishingType else null,
                price = it.price.toInt(),
            )
        }
    }

    @PostMapping("/search/count")
    fun countSearchContent(
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
                    .associateBy({ it.contentId }, { s3Service.toUrl(it.path) })

                SearchContext(
                    nickByMemberId = nickByMemberId,
                    tagsByContentId = tagsByContentId,
                    bookmarkInfo = bookmarksInfo,
                    favoriteCounts = favoriteCountsByContentId,
                    thumbnailPaths = thumbnailPaths,
                )
            },
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
                price = content.price,
            )
        }
    }

    @GetMapping("/tag")
    fun getSearchTagList(): SearchTagResponse {
        val tags = tagService.findAllTags().map {
            SearchTagResponse.TagResponse(it.id, it.name, it.type)
        }
        val genres = Genre.entries.filter { it.displayName.isNotEmpty() }
            .map { SearchTagResponse.GenreResponse(it, it.displayName) }
        val rules = Rule.entries.filter { it.displayName.isNotEmpty() }
            .map { SearchTagResponse.RuleResponse(it, it.displayName) }
        val publishingTypes = PublishingType.entries.filter { it.displayName.isNotEmpty() }
            .map { SearchTagResponse.PublishingTypeResponse(it, it.displayName) }

        return SearchTagResponse(tags, genres, rules, publishingTypes)
    }

    @GetMapping("/{alias}")
    fun getContent(
        @LoginMember authMember: AuthMember?,
        @PathVariable
        alias: String,
    ): ContentDetailResponse {
        val content = contentService.findByAlias(alias)
            ?: throw NotFoundException(
                "해당 시나리오/자료를 찾을 수 없습니다.",
                MessageCode.NOT_FOUND_CONTENT,
            )

        // 1. 작가이고 본인 작품일 경우: DRAFT, PRIVATE, PUBLISHED 상태에서 조회 가능
        val isCreatorAndOwnContent = authMember?.role == Role.CREATOR && authMember.memberId == content.memberId
        if (isCreatorAndOwnContent) {
            if (content.status in ContentsStatus.authorStatuses) {
                // 작가는 성인 인증이 되어 있으므로 성인 콘텐츠 체크 불필요
                return buildContentDetailResponse(content, authMember)
            }
        }

        // 2. 구매한 유저일 경우: PRIVATE, PUBLISHED 상태에서 조회 가능
        val isPurchased = authMember?.let {
            paymentContentService.isContentPurchased(it.memberId, content.id)
        } ?: false
        if (isPurchased) {
            if (content.status in ContentsStatus.purchasedStatuses) {
                // 구매한 경우 성인 콘텐츠 체크 불필요 (구매 시 이미 체크됨)
                return buildContentDetailResponse(content, authMember)
            }
        }

        // 3. 그 외의 경우: PUBLISHED 상태에서만 조회 가능, 성인 콘텐츠 체크 필요
        if (content.status !in ContentsStatus.publicStatuses) {
            throw NotFoundException(
                "해당 시나리오/자료를 찾을 수 없습니다.",
                MessageCode.NOT_FOUND_CONTENT,
            )
        }

        // 성인 콘텐츠 조회 제한: adult가 false이고 콘텐츠가 adult인 경우 조회 불가
        if (authMember?.adult == false && content.adult) {
            throw ContentException(
                "성인 인증이 필요한 콘텐츠입니다.",
                MessageCode.NOT_FOUND_CONTENT,
            )
        }

        return buildContentDetailResponse(content, authMember)
    }

    private fun buildContentDetailResponse(content: Content, authMember: AuthMember?): ContentDetailResponse {
        val writer = creatorService.getWriter(content.memberId)
        val thumbnailPath = contentService.getThumbnailPath(content.id)?.path
        val tags = tagService.findByContentId(content.id)
        val bookmark = authMember?.let {
            bookmarkContentService.existsBookmark(it.memberId, content.id)
        } ?: false

        val purchased = authMember?.let { member ->
            val isOwner = member.memberId == content.memberId
            val isPurchased = paymentContentService.isContentPurchased(member.memberId, content.id)
            if (isOwner || isPurchased || content.price == 0.00) {
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
            thumbnailPath = s3Service.toUrl(thumbnailPath),
            writerProfileUrl = s3Service.toUrl(writer?.profile),
        )
    }

    @GetMapping("/random/{contentType}")
    fun getRandomContent(
        @LoginMember authMember: AuthMember?,
        @PathVariable contentType: String,
        @Parameter(name = "type", description = "타입", `in` = ParameterIn.QUERY, example = "GENRE, RULE")
        @RequestParam(required = false) type: String? = null,
        @Parameter(name = "size", description = "반환할 콘텐츠 수", `in` = ParameterIn.QUERY, example = "8")
        @RequestParam(defaultValue = "8") size: Int,
    ): List<PickContentResponse> {
        val clampedSize = size.coerceIn(1, 50)
        val parsedContentType = try {
            ContentType.fromString(contentType)
        } catch (_: java.lang.IllegalArgumentException) {
            throw IllegalArgumentException(
                message = "유효하지 않은 콘텐츠 타입입니다. 시나리오 또는 자료만 가능합니다. (입력값: $contentType)",
                code = MessageCode.INVALID_PARAMS,
            )
        }
        val recommendType = type?.uppercase()

        if (authMember == null) {
            val contents = contentService.getRandomContents(parsedContentType, false, clampedSize)
            return buildPickContentResponses(contents, null, parsedContentType)
        }

        val contents = authMember.let {
            val member = memberService.getUserById(it.memberId)
                ?: throw NotFoundException(
                    message = "${MessageCode.NOT_FOUND_MEMBER} memberId = ${it.memberId}",
                    code = MessageCode.NOT_FOUND_MEMBER,
                )

            when (parsedContentType) {
                ContentType.SCENARIO -> {
                    if (recommendType == "GENRE" && !member.favoriteGenres.isNullOrBlank()) {
                        contentService.getRandomGenreContents(
                            ContentType.SCENARIO,
                            member.favoriteGenres!!,
                            it.adult,
                            clampedSize,
                        )
                    } else if (recommendType == "RULE" && !member.favoriteRules.isNullOrBlank()) {
                        contentService.getRandomRuleContents(member.favoriteRules!!, it.adult, clampedSize)
                    } else {
                        contentService.getRandomContents(ContentType.SCENARIO, it.adult, clampedSize)
                    }
                }
                ContentType.RESOURCE -> {
                    if (!member.favoriteGenres.isNullOrBlank()) {
                        contentService.getRandomGenreContents(
                            ContentType.RESOURCE,
                            member.favoriteGenres!!,
                            it.adult,
                            clampedSize,
                        )
                    } else {
                        contentService.getRandomContents(ContentType.RESOURCE, it.adult, clampedSize)
                    }
                }
            }
        }

        val fallbackContents = contents.ifEmpty {
            contentService.getRandomContents(parsedContentType, authMember.adult, clampedSize)
        }

        return buildPickContentResponses(fallbackContents, authMember, parsedContentType)
    }

    @GetMapping("/{contentId}/related-resources")
    fun getRelatedResources(
        @LoginMember authMember: AuthMember?,
        @PathVariable contentId: Long,
    ): List<PickContentResponse> {
        val isAdult = authMember?.adult ?: false
        val resources = relatedContentService.getRelatedContents(contentId, isAdult).resources
        return buildPickContentResponses(resources, authMember, ContentType.RESOURCE)
    }

    private fun buildPickContentResponses(
        contents: List<Content>,
        authMember: AuthMember?,
        contentType: ContentType,
    ): List<PickContentResponse> {
        val contentIds = contents.map { it.id }
        val writerName = creatorService.getWritersName(contents.map { it.memberId })
        val tags = contentService.getTags(contentIds)
        val thumbnails = contentService.getThumbnailPath(contentIds).associateBy { it.contentId }
        val bookmarks = authMember?.let {
            bookmarkContentService.getBookmarkList(it.memberId, contentIds)
        } ?: emptyMap()

        return contents.map {
            PickContentResponse(
                id = it.id,
                title = it.title,
                alias = it.alias,
                thumbnailPath = s3Service.toUrl(thumbnails[it.id]?.path),
                writer = writerName[it.memberId] ?: "",
                tags = tags[it.id] ?: emptyList(),
                orderNo = 0,
                rule = it.rule,
                playerCountType = it.playerCountType,
                isBookmark = bookmarks[it.id] != null,
                publishingType = if (contentType == ContentType.RESOURCE) it.publishingType else null,
                price = it.price.toInt(),
            )
        }
    }

    @GetMapping("/{contentId}/purchased")
    fun getPurchasedContent(
        @LoginMember authMember: AuthMember?,
        @PathVariable contentId: Long,
    ): PurchasedContentResponse {
        val memberId = authMember?.memberId
        val content = contentService.getNonFreeContent(
            contentId = contentId,
            memberId = memberId,
            isContentPurchased = { memberId, contentId ->
                paymentContentService.isContentPurchased(memberId, contentId)
            },
        )

        return PurchasedContentResponse(content = content)
    }
}
