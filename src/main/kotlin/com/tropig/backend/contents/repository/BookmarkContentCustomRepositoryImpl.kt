package com.tropig.backend.contents.repository

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.model.request.SearchContentRequest
import com.tropig.backend.contents.model.result.BookmarkContentResult
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BookmarkContentCustomRepositoryImpl(
    @PersistenceContext private val em: EntityManager
): BookmarkContentCustomRepository {
    override fun getBookmarkList(
        memberId: Long,
        type: ContentType,
        cursorId: Long?,
        cursorCreatedAt: LocalDateTime?,
        sortMode: SortMode,
        size: Int
    ): CursorSlice<BookmarkContentResult> {
        val sql = StringBuilder(
        """
            SELECT
                c.id,
                c.alias,
                c.title,
                c.rule,
                c.genre,
                c.member_id,
                c.player_count_type,
                bc.updated_at
            FROM content c
            INNER JOIN bookmark_content bc ON c.id = bc.content_id
            WHERE
                c.type = :type
            AND
                c.status = :status
            AND
                bc.member_id = :memberId
            AND
                bc.deleted = false
            """.trimIndent()
        )
        val params = mutableMapOf<String, Any?>(
            "type" to type.name,
            "status" to ContentsStatus.PUBLISHED.name,
            "memberId" to memberId
        )

        when (sortMode) {
            SortMode.LATEST -> {
                if (cursorCreatedAt != null && cursorId != null) {
                    sql.append("\n  AND (bc.updated_at, bc.id) < (:cursorCreatedAt, :cursorId)")
                    params["cursorCreatedAt"] = cursorCreatedAt
                    params["cursorId"] = cursorId
                }
                sql.append("\nORDER BY bc.updated_at DESC NULLS LAST, bc.id DESC")
            }
            SortMode.OLDEST -> {
                if (cursorCreatedAt != null && cursorId != null) {
                    sql.append("\n  AND (bc.updated_at, bc.id) > (:cursorCreatedAt, :cursorId)")
                    params["cursorCreatedAt"] = cursorCreatedAt
                    params["cursorId"] = cursorId
                }
                sql.append("\nORDER BY bc.updated_at ASC NULLS LAST, bc.id ASC")
            }
            else -> {}
        }

        sql.append("\nLIMIT :size")
        params["size"] = size + 1

        val rows = execute(sql.toString(), params)
        val hasNext = rows.size > size
        val items = if (hasNext) rows.take(size) else rows

        val last = items.lastOrNull()

        return CursorSlice(
            items = items,
            hasNext = hasNext,
            nextCursorDateAt = last?.updatedAt,
            nextCursorId = last?.id
        )
    }

    private fun execute(sql: String, params: Map<String, Any?>): List<BookmarkContentResult> {
        val q = em.createNativeQuery(sql)
        params.forEach { (k, v) -> q.setParameter(k, v) }

        @Suppress("UNCHECKED_CAST")
        val rows = q.resultList as List<Array<Any?>>

        return rows.map { r ->
            BookmarkContentResult(
                id = (r[0] as Number).toLong(),
                alias = r[1] as String,
                title = r[2] as String,
                rule = Rule.valueOf(r[3] as String),
                genre = Genre.valueOf(r[4] as String),
                memberId = (r[5] as Number).toLong(),
                playerCountType = PlayerCountType.valueOf(r[6] as String),
                updatedAt = (r[7] as java.sql.Timestamp).toLocalDateTime(),
            )
        }
    }
}