package com.tropig.backend.contents.repository

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.model.request.SearchContentRequest
import com.tropig.backend.contents.model.request.SearchOptionalContentRequest
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import kotlin.math.min

@Repository
class ContentCustomRepositoryImpl(
    @PersistenceContext private val em: EntityManager
) : ContentCustomRepository {

    override fun searchContents(request: SearchContentRequest, type: ContentType): CursorSlice<Content> {
        val common = request.toCommon()

        return runSearch(
            common = common,
            type = type,
            sortSpec = when (common.sortMode) {
                SortMode.LATEST -> SortSpec.latest()
                SortMode.TITLE -> SortSpec.title()
            },
            filterAppender = { sql, params ->
                // EQ 필터
                appendEqEnum(sql, params, "c.rule", "rule", request.rule)
                appendEqEnum(sql, params, "c.genre", "genre", request.genre)
                appendEqEnum(sql, params, "c.player_count_type", "playerCountType", request.playerCountType)
                appendEqEnum(sql, params, "c.level", "level", request.level)
            }
        )
    }

    override fun searchOptionalContents(request: SearchOptionalContentRequest, type: ContentType): CursorSlice<Content> {
        val common = request.toCommon()

        return runSearch(
            common = common,
            type = type,
            sortSpec = when (common.sortMode) {
                SortMode.LATEST -> SortSpec.latest()
                SortMode.TITLE -> SortSpec.title()
            },
            filterAppender = { sql, params ->
                // IN 필터 (컬렉션 기반)
                appendInEnum(sql, params, "c.rule", "rules", request.rules)
                appendInEnum(sql, params, "c.genre", "genres", request.genres)
                appendInEnum(sql, params, "c.player_count_type", "playerCountTypes", request.playerCountTypes)
            }
        )
    }

    /**
     * DTO 수정 없이 "공통 필드"만 내부 타입으로 변환해서 처리
     */
    private data class CommonSearchReq(
        val size: Int,
        val isAdult: Boolean,
        val searchText: String? = null,
        val sortMode: SortMode,
        val cursorId: Long,
        val cursorDateAt: LocalDateTime?,
        val cursorTitle: String?
    )

    private fun SearchContentRequest.toCommon(): CommonSearchReq =
        CommonSearchReq(
            size = this.size,
            isAdult = this.isAdult,
            searchText = this.searchText,
            sortMode = this.sortMode,
            cursorId = this.cursorId,
            cursorDateAt = this.cursorPublishedAt,
            cursorTitle = this.cursorTitle
        )

    private fun SearchOptionalContentRequest.toCommon(): CommonSearchReq =
        CommonSearchReq(
            size = this.size,
            isAdult = this.isAdult,
            sortMode = this.sortMode,
            cursorId = this.cursorId,
            cursorDateAt = this.cursorPublishedAt,
            cursorTitle = this.cursorTitle
        )

    private fun runSearch(
        common: CommonSearchReq,
        type: ContentType,
        sortSpec: SortSpec,
        filterAppender: (StringBuilder, MutableMap<String, Any?>) -> Unit
    ): CursorSlice<Content> {
        val size = min(common.size.coerceAtLeast(1), 30)
        val fetchSize = size + 1

        val (sql, params) = buildBaseSqlCommon(common, type)
        filterAppender(sql, params)

        sortSpec.appendCursor(sql, params, common)
        sortSpec.appendOrderBy(sql)

        sql.append("\nLIMIT :limit")
        params["limit"] = fetchSize

        val rows = execute(sql.toString(), params)
        val hasNext = rows.size > size
        val items = if (hasNext) rows.take(size) else rows

        val last = items.lastOrNull()
        return sortSpec.buildSlice(items, hasNext, last)
    }

    private fun buildBaseSqlCommon(
        common: CommonSearchReq,
        type: ContentType
    ): Pair<StringBuilder, MutableMap<String, Any?>> {
        val sql = StringBuilder(
            """
            SELECT c.*
            FROM content c
            WHERE c.type = :type
              AND c.status = :status
            """.trimIndent()
        )

        val params = mutableMapOf<String, Any?>(
            "type" to type.name,
            "status" to ContentsStatus.PUBLISHED.name
        )

        if (!common.isAdult) {
            sql.append("\n  AND c.adult = false")
        }

        appendKeywordClause(sql, params, common.searchText)
        return sql to params
    }

    @Suppress("UNCHECKED_CAST")
    private fun execute(sql: String, params: Map<String, Any?>): List<Content> {
        val q = em.createNativeQuery(sql, Content::class.java)
        params.forEach { (k, v) -> q.setParameter(k, v) }
        return q.resultList as List<Content>
    }

    private data class SortSpec(
        val mode: SortMode,
        val appendCursor: (StringBuilder, MutableMap<String, Any?>, CommonSearchReq) -> Unit,
        val appendOrderBy: (StringBuilder) -> Unit,
        val buildSlice: (List<Content>, Boolean, Content?) -> CursorSlice<Content>
    ) {
        companion object {
            fun latest(): SortSpec = SortSpec(
                mode = SortMode.LATEST,
                appendCursor = { sql, params, req ->
                    if (req.cursorDateAt != null && req.cursorId != 0L) {
                        sql.append("\n  AND (c.published_at, c.id) < (:cursorPublishedAt, :cursorId)")
                        params["cursorPublishedAt"] = req.cursorDateAt
                        params["cursorId"] = req.cursorId
                    }
                },
                appendOrderBy = { sql ->
                    sql.append("\nORDER BY c.published_at DESC NULLS LAST, c.id DESC")
                },
                buildSlice = { items, hasNext, last ->
                    CursorSlice(
                        items = items,
                        hasNext = hasNext,
                        nextCursorDateAt = last?.publishedAt,
                        nextCursorId = last?.id
                    )
                }
            )

            fun title(): SortSpec = SortSpec(
                mode = SortMode.TITLE,
                appendCursor = { sql, params, req ->
                    if (req.cursorTitle != null && req.cursorId != 0L) {
                        sql.append("\n  AND (lower(c.title), c.id) > (lower(:cursorTitle), :cursorId)")
                        params["cursorTitle"] = req.cursorTitle
                        params["cursorId"] = req.cursorId
                    }
                },
                appendOrderBy = { sql ->
                    sql.append("\nORDER BY lower(c.title) ASC, c.id ASC")
                },
                buildSlice = { items, hasNext, last ->
                    CursorSlice(
                        items = items,
                        hasNext = hasNext,
                        nextCursorTitle = last?.title,
                        nextCursorId = last?.id
                    )
                }
            )
        }
    }

    private fun appendKeywordClause(sql: StringBuilder, params: MutableMap<String, Any?>, keyword: String?) {
        val q = keyword?.trim().orEmpty()
        if (q.isEmpty() || q.length == 1) return

        val pattern = if (q.length == 2) "${q.lowercase()}%" else "%${q.lowercase()}%"
        sql.append("\n  AND lower(c.search_text) LIKE :kw")
        params["kw"] = pattern
    }

    private fun appendEqEnum(
        sql: StringBuilder,
        params: MutableMap<String, Any?>,
        column: String,
        paramName: String,
        value: Any?
    ) {
        if (value == null) return
        sql.append("\n  AND $column = :$paramName")
        params[paramName] = toDbParamValue(value)
    }

    private fun appendInEnum(
        sql: StringBuilder,
        params: MutableMap<String, Any?>,
        column: String,
        paramName: String,
        values: Collection<Any>?
    ) {
        if (values.isNullOrEmpty()) return
        val converted = values.mapNotNull { toDbParamValue(it) }
        if (converted.isEmpty()) return

        sql.append("\n  AND $column IN (:$paramName)")
        params[paramName] = converted
    }

    private fun toDbParamValue(value: Any?): Any? =
        when (value) {
            null -> null
            is Enum<*> -> value.name
            is Int, is Long, is String -> value
            else -> error("지원하지 않는 타입: ${value::class}")
        }
}
