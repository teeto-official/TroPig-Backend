package com.tropig.backend.payment.repository

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.payment.enums.PurchaseStatus
import com.tropig.backend.payment.model.request.PurchasedContentListRequest
import com.tropig.backend.payment.model.result.PurchasedContentProjection
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.math.min

@Repository
class PurchaseCustomRepositoryImpl(
    @PersistenceContext private val em: EntityManager
) : PurchaseCustomRepository {

    override fun findPurchasedContents(
        memberId: Long,
        request: PurchasedContentListRequest,
    ): CursorSlice<PurchasedContentProjection> {
        val size = min(request.size.coerceAtLeast(1), 30)
        val fetchSize = size + 1

        val sql = StringBuilder(
            """
            SELECT p.id AS p_id, p.content_id, p.created_at AS p_created_at, p.amount AS p_amount
            FROM purchase p
            WHERE p.member_id = :memberId
              AND p.status = :status
            """.trimIndent()
        )

        val params = mutableMapOf<String, Any?>(
            "memberId" to memberId,
            "status" to PurchaseStatus.COMPLETED.name,
        )

        when (request.sortMode) {
            SortMode.LATEST -> {
                if (request.cursorCreatedAt != null && request.cursorId != 0L) {
                    sql.append("\n  AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorId)")
                    params["cursorCreatedAt"] = request.cursorCreatedAt
                    params["cursorId"] = request.cursorId
                }
                sql.append("\nORDER BY p.created_at DESC, p.id DESC")
            }
            SortMode.OLDEST -> {
                if (request.cursorCreatedAt != null && request.cursorId != 0L) {
                    sql.append("\n  AND (p.created_at, p.id) > (:cursorCreatedAt, :cursorId)")
                    params["cursorCreatedAt"] = request.cursorCreatedAt
                    params["cursorId"] = request.cursorId
                }
                sql.append("\nORDER BY p.created_at ASC, p.id ASC")
            }
            else -> {
                sql.append("\nORDER BY p.created_at DESC, p.id DESC")
            }
        }

        sql.append("\nLIMIT :limit")
        params["limit"] = fetchSize

        val purchaseRows = executePurchaseList(sql.toString(), params)
        val hasNext = purchaseRows.size > size
        val trimmed = if (hasNext) purchaseRows.take(size) else purchaseRows

        if (trimmed.isEmpty()) {
            return CursorSlice(items = emptyList(), hasNext = false)
        }

        // bulk load contents
        val contentIds = trimmed.map { it.contentId }
        val contentById = loadContents(contentIds)

        val items = trimmed.mapNotNull { row ->
            val content = contentById[row.contentId] ?: return@mapNotNull null
            PurchasedContentProjection(
                content = content,
                purchasedAt = row.purchasedAt,
                purchaseAmount = row.purchaseAmount,
            )
        }

        val lastRow = trimmed.lastOrNull()
        return CursorSlice(
            items = items,
            hasNext = hasNext,
            nextCursorDateAt = lastRow?.purchasedAt,
            nextCursorId = lastRow?.purchaseId,
        )
    }

    private data class PurchaseRow(
        val purchaseId: Long,
        val contentId: Long,
        val purchasedAt: LocalDateTime,
        val purchaseAmount: Long,
    )

    @Suppress("UNCHECKED_CAST")
    private fun executePurchaseList(sql: String, params: Map<String, Any?>): List<PurchaseRow> {
        val q = em.createNativeQuery(sql)
        params.forEach { (k, v) -> q.setParameter(k, v) }

        val results = q.resultList as List<Array<Any?>>
        return results.map { row ->
            PurchaseRow(
                purchaseId = (row[0] as Number).toLong(),
                contentId = (row[1] as Number).toLong(),
                purchasedAt = (row[2] as Timestamp).toLocalDateTime(),
                purchaseAmount = (row[3] as Number).toLong(),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadContents(contentIds: List<Long>): Map<Long, Content> {
        if (contentIds.isEmpty()) return emptyMap()
        val q = em.createQuery("SELECT c FROM Content c WHERE c.id IN (:ids)", Content::class.java)
        q.setParameter("ids", contentIds)
        val contents = q.resultList as List<Content>
        return contents.associateBy { it.id }
    }
}
