package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.exception.IllegalArgumentException
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.common.model.SearchContext
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.model.request.CreateContentRequest
import com.tropig.backend.contents.model.request.SimpleSearchContentRequest
import com.tropig.backend.contents.model.request.UpdateContentRequest
import com.tropig.backend.contents.model.request.UpdateContentStatusRequest
import com.tropig.backend.contents.model.response.*
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.contents.service.TagService
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.service.CreatorService
import com.tropig.backend.payment.service.RevenueService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@ApiController
@RequestMapping("/api/content")
class CreatorController(
    private val contentService: ContentService,
    private val revenueService: RevenueService,
    private val tagService: TagService,
    private val creatorService: CreatorService,
    private val s3Service: S3Service,
) {

    @PostMapping
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    fun createContent(@LoginMember authMember: AuthMember, @RequestBody request: CreateContentRequest): Long {
        // CREATOR 권한 체크
        if (authMember.role != Role.CREATOR) {
            throw ContentException(
                message = "콘텐츠를 생성할 권한이 없습니다. CREATOR 권한이 필요합니다.",
                code = MessageCode.INCORRECT_ROLE,
            )
        }

        val content = contentService.createContent(
            request = request,
            memberId = authMember.memberId,
            writerNickname = authMember.nickname,
        )

        return content.id
    }

    @PutMapping("/{contentId}")
    @RequireAuth
    fun updateContent(
        @LoginMember authMember: AuthMember,
        @PathVariable contentId: Long,
        @RequestBody request: UpdateContentRequest,
    ): Long {
        // 1. CREATOR 권한 체크
        if (authMember.role != Role.CREATOR) {
            throw ContentException(
                message = "콘텐츠를 수정할 권한이 없습니다. CREATOR 권한이 필요합니다.",
                code = MessageCode.INCORRECT_ROLE,
            )
        }

        // 2. Content 조회 및 memberId 검증
        val content = contentService.findById(contentId)
            ?: throw ContentException(
                message = "Content를 찾을 수 없습니다.",
                code = MessageCode.NOT_FOUND_CONTENT,
            )

        if (content.memberId != authMember.memberId) {
            throw MemberException(
                message = "본인이 작성한 작품만 수정할 수 있습니다.",
                code = MessageCode.NOT_OWN_CONTENT,
            )
        }

        val updatedContent = contentService.updateContent(
            contentId = contentId,
            request = request,
            memberId = authMember.memberId,
            writerNickname = authMember.nickname,
        )

        return updatedContent.id
    }

    @RequireAuth
    @DeleteMapping("/{contentId}")
    fun deleteContent(
        @LoginMember authMember: AuthMember,
        @PathVariable
        contentId: Long,
    ) {
        // 1. AuthMember의 role이 CREATOR인지 확인
        if (authMember.role != Role.CREATOR) {
            throw ContentException(
                "CREATOR 권한이 필요합니다.",
                MessageCode.INCORRECT_ROLE,
            )
        }

        // 2. content.member_id가 authMember.member_id와 같은지 확인 (ContentService에서 처리)
        // 3. status만 DELETED로 변경 (ContentService에서 처리)
        val content = contentService.findById(contentId) ?: throw NotFoundException(
            "콘텐츠를 찾을 수 없습니다.",
            MessageCode.NOT_FOUND_CONTENT,
        )
        contentService.deleteContent(content, authMember.memberId)
    }

    @RequireAuth
    @PatchMapping("/{contentId}/status")
    fun updateContentStatus(
        @LoginMember authMember: AuthMember,
        @PathVariable contentId: Long,
        @RequestBody request: UpdateContentStatusRequest,
    ): Long {
        if (authMember.role != Role.CREATOR) {
            throw ContentException(
                message = "콘텐츠 상태를 변경할 권한이 없습니다. CREATOR 권한이 필요합니다.",
                code = MessageCode.INCORRECT_ROLE,
            )
        }

        if (request.status !in listOf(ContentsStatus.PRIVATE, ContentsStatus.PUBLISHED)) {
            throw IllegalArgumentException(
                message = "변경 가능한 상태는 비공개 또는 공개만 가능합니다. (입력값: contentId: $contentId, status: ${request.status})",
                code = MessageCode.INVALID_PARAMS,
            )
        }

        val content = contentService.findById(contentId)
            ?.takeIf { it.status != ContentsStatus.DELETED }
            ?: throw NotFoundException("콘텐츠를 찾을 수 없습니다.", MessageCode.NOT_FOUND_CONTENT)

        val updatedContent = contentService.updateContentStatus(
            content = content,
            memberId = authMember.memberId,
            status = request.status,
        )

        return updatedContent.id
    }

    @RequireAuth
    @GetMapping("/creator/{type}/{status}")
    fun getCreatorContent(
        @LoginMember authMember: AuthMember,
        @PathVariable type: ContentType,
        @PathVariable status: ContentsStatus,
        @RequestParam(defaultValue = "15") size: Int,
        @RequestParam(defaultValue = "0") cursorContentId: Long,
    ): List<CreatorContentResponse> {
        if (authMember.role != Role.CREATOR) {
            throw MemberException(
                message = "창작자가 아닙니다. memberId: ${authMember.memberId}",
                code = MessageCode.INCORRECT_ROLE,
            )
        }
        val contents = when (status) {
            ContentsStatus.PUBLISHED -> {
                revenueService.getCreatorContents(authMember.memberId, type)
                    .sortedByDescending { it.publishedAt }
            }
            ContentsStatus.DRAFT -> {
                contentService.getDraftContent(authMember.memberId)
                    .sortedByDescending { it.createdAt }
            }
            else -> {
                throw IllegalArgumentException(
                    message = "유효하지 않은 상태값입니다. PUBLISHED 또는 DRAFT만 가능합니다. (입력값: $status)",
                    code = MessageCode.INVALID_PARAMS,
                )
            }
        }
            .filter { cursorContentId == 0L || it.id < cursorContentId }
            .take(size)

        val contentIds = contents.map { it.id }
        val thumbnailPaths = contentService.getThumbnailPath(contentIds)
            .associateBy({ it.contentId }, { s3Service.toUrl(it.path) })
        val tags = tagService.findTagNamesByContentIds(contentIds)

        return contents.map {
            CreatorContentResponse(
                id = it.id,
                alias = it.alias,
                title = it.title,
                type = it.type,
                status = it.status,
                rule = it.rule,
                genre = it.genre,
                writer = authMember.nickname,
                playerCountType = it.playerCountType,
                thumbnailPath = thumbnailPaths[it.id],
                tags = tags[it.id] ?: emptyList(),
                publishedAt = it.publishedAt ?: it.updatedAt,
                freeContent = Content.removeSpoilers(it.freeContent),
                price = it.price.toInt(),
                publishingType = if (type == ContentType.RESOURCE) it.publishingType else null
            )
        }
    }

    @RequireAuth
    @GetMapping("/creator/{alias}")
    fun getContent(
        @LoginMember authMember: AuthMember,
        @PathVariable
        alias: String,
    ): CreatorContentDetailResponse {
        // 1. 콘텐츠 존재 확인
        val content = contentService.findByAlias(alias)
            ?: throw NotFoundException(
                "해당 콘텐츠를 찾을 수 없습니다.",
                MessageCode.NOT_FOUND_CONTENT,
            )

        // 2. CREATOR 권한 확인
        if (authMember.role != Role.CREATOR) {
            throw ContentException(
                message = "창작자 권한이 필요합니다.",
                code = MessageCode.INCORRECT_ROLE,
            )
        }

        // 3. 본인 콘텐츠 확인
        if (content.memberId != authMember.memberId) {
            throw ContentException(
                message = "본인이 작성한 콘텐츠만 조회할 수 있습니다.",
                code = MessageCode.NOT_OWN_CONTENT,
            )
        }

        // 4. 삭제되지 않은 콘텐츠 확인 (DRAFT, PRIVATE, PUBLISHED만 조회 가능)
        if (content.status !in ContentsStatus.authorStatuses) {
            throw ContentException(
                message = "삭제된 콘텐츠입니다.",
                code = MessageCode.NOT_FOUND_CONTENT,
            )
        }

        // 5. 데이터 조회 및 반환
        val tags = tagService.findByContentId(content.id)
        val nonFreeContent = content.nonFreeContent?.let { path ->
            s3Service.getFileAsString(path)
        }

        val thumbnailPath = contentService.getThumbnailPath(content.id)?.path
        val writer = creatorService.getWriter(authMember.memberId)!!

        return content.toCreatorContentDetailResponse(
            tags = tags,
            nonFreeContent = nonFreeContent,
            thumbnailPath = thumbnailPath?.let { s3Service.toUrl(it) },
            writer = WriterInfo(
                writerId = writer.id,
                nickname = writer.nickname,
                profilePath = s3Service.toUrl(writer.profile)
            )
        )
    }

    @RequireAuth
    @PostMapping("/search/related")
    fun getSearchRelatedContent(
        @LoginMember authMember: AuthMember,
        @RequestBody request: SimpleSearchContentRequest,
    ): CursorSlice<SimpleSearchContentResponse> {
        val isAdult = authMember.adult

        val dto = request.toDto(isAdult)
        val contents = contentService.searchContents(dto)

        return contents.mapWith(
            buildContext = { items ->
                val contentIds = items.map { it.id }
                val writerIds = items.map { it.memberId }.distinct()

                val nickByMemberId = creatorService.getWritersName(writerIds)
                val tagsByContentId = tagService.findTagNamesByContentIds(contentIds)
                val thumbnailPaths = contentService.getThumbnailPath(contentIds)
                    .associateBy({ it.contentId }, { s3Service.toUrl(it.path) })

                SearchContext(
                    nickByMemberId = nickByMemberId,
                    tagsByContentId = tagsByContentId,
                    bookmarkInfo = emptyMap(),
                    favoriteCounts = emptyMap(),
                    thumbnailPaths = thumbnailPaths,
                )
            },
        ) { content, ctx ->
            SimpleSearchContentResponse(
                id = content.id,
                title = content.title,
                rule = content.rule,
                writer = ctx.nickByMemberId[content.memberId] ?: "탈퇴한 작가입니다.",
                thumbnailPath = ctx.thumbnailPaths[content.id],
            )
        }
    }
}
