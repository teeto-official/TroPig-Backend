package com.tropig.backend.payment.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.model.result.BookmarkContentInfo
import com.tropig.backend.contents.model.result.TagDto
import com.tropig.backend.contents.service.BookmarkContentService
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.contents.service.TagService
import com.tropig.backend.member.service.CreatorService
import com.tropig.backend.payment.model.request.ConfirmPurchaseRequest
import com.tropig.backend.payment.model.request.CreatePurchaseRequest
import com.tropig.backend.payment.model.request.FailPurchaseRequest
import com.tropig.backend.payment.model.request.PurchasedContentListRequest
import com.tropig.backend.payment.model.response.CreatePurchaseResponse
import com.tropig.backend.payment.model.response.PurchaseResponse
import com.tropig.backend.payment.model.response.PurchasedContentListResponse
import com.tropig.backend.payment.service.PaymentContentService
import com.tropig.backend.payment.service.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@ApiController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "결제 API")
class PaymentController(
    private val paymentService: PaymentService,
    private val paymentContentService: PaymentContentService,
    private val creatorService: CreatorService,
    private val tagService: TagService,
    private val bookmarkContentService: BookmarkContentService,
    private val contentService: ContentService,
    private val s3Service: S3Service,
) {
    @RequireAuth
    @PostMapping("/purchase")
    @Operation(summary = "구매 요청 생성", description = "콘텐츠 구매를 위한 결제를 생성합니다.")
    fun createPurchase(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: CreatePurchaseRequest,
    ): ResponseEntity<CreatePurchaseResponse> {
        val response = paymentService.createPurchase(authMember.memberId, authMember.adult, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @RequireAuth
    @PostMapping("/purchase/confirm")
    @Operation(summary = "결제 승인", description = "생성된 결제를 승인합니다.")
    fun confirmPurchase(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: ConfirmPurchaseRequest,
    ): ResponseEntity<PurchaseResponse> {
        val response = paymentService.confirmPurchase(authMember.memberId, request)
        return ResponseEntity.ok(response)
    }

    @RequireAuth
    @PostMapping("/purchase/fail")
    @Operation(summary = "결제 실패 처리", description = "결제 실패 시 Payment 상태를 FAILED로 업데이트합니다.")
    fun failPurchase(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: FailPurchaseRequest,
    ): ResponseEntity<Void> {
        paymentService.failPurchase(authMember.memberId, request)
        return ResponseEntity.ok().build()
    }

    @RequireAuth
    @GetMapping("/purchase/{purchaseId}")
    @Operation(summary = "구매 내역 조회", description = "특정 구매 내역을 조회합니다.")
    fun getPurchase(
        @LoginMember authMember: AuthMember,
        @PathVariable purchaseId: Long,
    ): ResponseEntity<PurchaseResponse> {
        val response = paymentService.getPurchase(authMember.memberId, purchaseId)
        return ResponseEntity.ok(response)
    }

    @RequireAuth
    @GetMapping("/content/{contentId}/purchased")
    @Operation(summary = "콘텐츠 구매 여부 확인", description = "특정 콘텐츠의 구매 여부를 확인합니다.")
    fun isContentPurchased(
        @LoginMember authMember: AuthMember,
        @PathVariable contentId: Long,
    ): ResponseEntity<Map<String, Boolean>> {
        val purchased = paymentService.isContentPurchased(authMember.memberId, contentId)
        return ResponseEntity.ok(mapOf("purchased" to purchased))
    }

    @RequireAuth
    @GetMapping("/purchase/count")
    @Operation(summary = "구매 건수 조회", description = "회원의 총 구매 건수를 조회합니다.")
    fun getPurchaseCount(@LoginMember authMember: AuthMember): ResponseEntity<Map<String, Long>> {
        val count = paymentContentService.getPurchaseCount(authMember.memberId)
        return ResponseEntity.ok(mapOf("count" to count))
    }

    @RequireAuth
    @GetMapping("/purchase")
    @Operation(summary = "구매한 컨텐츠 목록 조회", description = "회원이 구매한 컨텐츠 목록을 조회합니다.")
    fun getPurchasedContents(
        @LoginMember authMember: AuthMember,
        @Parameter(name = "cursorId", description = "커서 bookmarkId", `in` = ParameterIn.QUERY)
        cursorId: Long? = null,
        @Parameter(name = "cursorCreatedAt", description = "커서 등록일자", `in` = ParameterIn.QUERY)
        cursorCreatedAt: LocalDateTime? = null,
        @Parameter(
            name = "sortMode",
            description = "정렬 순서",
            `in` = ParameterIn.QUERY,
            schema = Schema(
                allowableValues = ["LATEST", "OLDEST"],
                defaultValue = "LATEST",
            ),
        )
        sortMode: SortMode = SortMode.LATEST,
        @Parameter(name = "size", description = "페이지 크기", `in` = ParameterIn.QUERY)
        size: Int = 15,
    ): CursorSlice<PurchasedContentListResponse> {
        val request = PurchasedContentListRequest(
            sortMode = sortMode,
            cursorCreatedAt = cursorCreatedAt,
            cursorId = cursorId ?: 0,
            size = size,
        )
        val memberId = authMember.memberId
        val contents = paymentContentService.getPurchasedContents(memberId, request)

        return contents.mapWith(
            buildContext = { items ->
                val contentIds = items.map { it.content.id }
                val writerIds = items.map { it.content.memberId }.distinct()

                val nickByMemberId = creatorService.getWritersName(writerIds)
                val tagsByContentId = tagService.findTagNamesByContentIds(contentIds)
                val bookmarksInfo = bookmarkContentService.getBookmarkInfo(memberId, contentIds)
                val thumbnailPaths = contentService.getThumbnailPath(contentIds)
                    .associateBy({ it.contentId }, { s3Service.toUrl(it.path) })

                PurchasedContentContext(
                    nickByMemberId = nickByMemberId,
                    tagsByContentId = tagsByContentId,
                    bookmarkInfo = bookmarksInfo,
                    thumbnailPaths = thumbnailPaths,
                )
            },
        ) { item, ctx ->
            val content = item.content
            PurchasedContentListResponse(
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
                isBookmark = ctx.bookmarkInfo[content.id]?.bookmarked ?: false,
                publishedAt = content.publishedAt!!,
                freeContent = content.freeContent,
                purchasedAt = item.purchasedAt,
                purchaseAmount = item.purchaseAmount,
                price = content.price.toInt(),
            )
        }
    }
}

private data class PurchasedContentContext(
    val nickByMemberId: Map<Long, String>,
    val tagsByContentId: Map<Long, List<TagDto>>,
    val bookmarkInfo: Map<Long, BookmarkContentInfo>,
    val thumbnailPaths: Map<Long, String?>,
)
