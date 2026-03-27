package com.tropig.backend.payment.repository

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.payment.enums.PurchaseStatus
import com.tropig.backend.payment.model.request.PurchasedContentListRequest
import com.tropig.backend.payment.model.result.PurchasedContentData
import com.tropig.backend.payment.model.result.PurchasedContentProjection
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.math.min

@Repository
class PurchaseCustomRepositoryImpl(@PersistenceContext private val em: EntityManager) : PurchaseCustomRepository {

    override fun findPurchasedContents(
        memberId: Long,
        request: PurchasedContentListRequest,
    ): CursorSlice<PurchasedContentProjection> {
        val size = min(request.size.coerceAtLeast(1), 30)
        val fetchSize = size + 1

        val sql = StringBuilder(
            """
            SELECT p.id AS p_id, p.created_at AS p_created_at, p.amount AS p_amount,
                   c.id, c.alias, c.title, c.type, c.member_id, c.rule_id, c.genre_id,
                   c.player_count_type, c.term_type, c.publishing_type, c.publishing_info,
                   c.status, c.adult, c.published_at, c.free_content, c.non_free_content,
                   c.price, c.level, c.search_text, c.prevent_right_click,
                   c.created_at AS c_created_at, c.updated_at AS c_updated_at
            FROM purchase p
            JOIN content c ON p.content_id = c.id
            WHERE p.member_id = :memberId
              AND p.status = :status
            """.trimIndent(),
        )

        val params = mutableMapOf<String, Any?>(
            "memberId" to memberId,
            "status" to PurchaseStatus.COMPLETED.name,
        )

        if (request.type != null) {
            sql.append("\n  AND c.type = :type")
            params["type"] = request.type.name
        }

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

        val rows = executeQuery(sql.toString(), params)
        val hasNext = rows.size > size
        val trimmed = if (hasNext) rows.take(size) else rows

        if (trimmed.isEmpty()) {
            return CursorSlice(items = emptyList(), hasNext = false)
        }

        val lastRow = trimmed.last()
        return CursorSlice(
            items = trimmed,
            hasNext = hasNext,
            nextCursorDateAt = lastRow.purchasedAt,
            nextCursorId = lastRow.purchaseId,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun executeQuery(sql: String, params: Map<String, Any?>): List<PurchasedContentProjection> {
        val q = em.createNativeQuery(sql)
        params.forEach { (k, v) -> q.setParameter(k, v) }

        val results = q.resultList as List<Array<Any?>>
        return results.map { r ->
            // r[0]=p.id, r[1]=p.created_at, r[2]=p.amount
            // r[3]=c.id, r[4]=c.alias, r[5]=c.title, r[6]=c.type, r[7]=c.member_id
            // r[8]=c.rule, r[9]=c.genre, r[10]=c.player_count_type, r[11]=c.term_type
            // r[12]=c.publishing_type, r[16]=c.published_at, r[17]=c.free_content, r[19]=c.price
            PurchasedContentProjection(
                purchaseId = (r[0] as Number).toLong(),
                content = PurchasedContentData(
                    id = (r[3] as Number).toLong(),
                    alias = r[4] as String,
                    title = r[5] as String,
                    type = ContentType.valueOf(r[6] as String),
                    memberId = (r[7] as Number).toLong(),
                    ruleId = (r[8] as? Number)?.toLong(),
                    genreId = (r[9] as? Number)?.toLong(),
                    playerCountType = PlayerCountType.valueOf(r[10] as String),
                    publishingType = PublishingType.valueOf(r[12] as String),
                    publishedAt = (r[16] as? Timestamp)?.toLocalDateTime(),
                    freeContent = r[17] as? String,
                    price = (r[19] as Number).toDouble(),
                ),
                purchasedAt = (r[1] as Timestamp).toLocalDateTime(),
                purchaseAmount = (r[2] as Number).toLong(),
            )
        }
    }
}
