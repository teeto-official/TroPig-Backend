package com.tropig.backend.contents.repository

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.model.request.SearchContentRequest
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import kotlin.math.min

@Repository
class ContentCustomRepositoryImpl(
    @PersistenceContext private val em: EntityManager
): ContentCustomRepository {
    override fun searchContents(request: SearchContentRequest, type: ContentType): CursorSlice<Content> {
        val size = min(request.size.coerceAtLeast(1), 30)
        val fetchSize = size + 1

        return when (request.sortMode) {
            SortMode.LATEST -> runSearch(
                req = request,
                type = type,
                size = size,
                fetchSize = fetchSize,
                sortSpec = SortSpec.latest()
            )

            SortMode.TITLE -> runSearch(
                req = request,
                type = type,
                size = size,
                fetchSize = fetchSize,
                sortSpec = SortSpec.title()
            )
            else -> CursorSlice(
                emptyList(),
                false,
            )
        }
    }

    private fun runSearch(
        req: SearchContentRequest,
        type: ContentType,
        size: Int,
        fetchSize: Int,
        sortSpec: SortSpec
    ): CursorSlice<Content> {
        val (sql, params) = buildBaseSql(req, type)
        sortSpec.appendCursor(sql, params, req)
        sortSpec.appendOrderBy(sql)
        sql.append("\nLIMIT :limit")
        params["limit"] = fetchSize

        val rows = execute(sql.toString(), params)
        val hasNext = rows.size > size
        val items = if (hasNext) rows.take(size) else rows

        val last = items.lastOrNull()
        return sortSpec.buildSlice(items, hasNext, last)
    }

    private fun buildBaseSql(req: SearchContentRequest, type: ContentType): Pair<StringBuilder, MutableMap<String, Any?>> {
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

        if (!req.isAdult) {
            sql.append("\n  AND c.adult = false")
        }

        // keyword
        appendKeywordClause(sql, params, req.searchText)

        // 옵션 IN 필터들
        appendEqEnum(sql, params, "c.rule", "rule", req.rule)
        appendEqEnum(sql, params, "c.genre", "genre", req.genre)
        appendEqEnum(sql, params, "c.player_count_type", "playerCountType", req.playerCountType)
        appendEqEnum(sql, params, "c.level", "level", req.level)

        return sql to params
    }

    @Suppress("UNCHECKED_CAST")
    private fun execute(sql: String, params: Map<String, Any?>): List<Content> {
        val q = em.createNativeQuery(sql, Content::class.java)

        params.forEach { (k, v) -> q.setParameter(k, v) }
        return q.resultList as List<Content>
    }

    /**
     * 정렬/커서/nextCursor 계산을 한 곳으로 모음
     */
    private data class SortSpec(
        val mode: SortMode,
        val appendCursor: (StringBuilder, MutableMap<String, Any?>, SearchContentRequest) -> Unit,
        val appendOrderBy: (StringBuilder) -> Unit,
        val buildSlice: (List<Content>, Boolean, Content?) -> CursorSlice<Content>
    ) {
        companion object {
            fun latest(): SortSpec = SortSpec(
                mode = SortMode.LATEST,
                appendCursor = { sql, params, req ->
                    if (req.cursorPublishedAt != null && req.cursorId != 0L) {
                        sql.append("\n  AND (c.published_at, c.id) < (:cursorPublishedAt, :cursorId)")
                        params["cursorPublishedAt"] = req.cursorPublishedAt
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

    /**
     * 키워드 검색 정책:
     * - null/blank: 조건 없음
     * - 1글자: 거부
     * - 2글자: prefix 검색 (q%)
     * - 3글자+: contains 검색 (%q%)
     */
    private fun appendKeywordClause(sql: StringBuilder, params: MutableMap<String, Any?>, keyword: String?) {
        val q = keyword?.trim().orEmpty()
        if (q.isEmpty() || q.length == 1) return

        // prefix vs contains
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
        params[paramName] = when (value) {
            is Enum<*> -> value.name   // enum은 문자열로 저장
            is Int -> value
            is Long -> value
            is String -> value
            else -> error("지원하지 않는 타입: ${value::class}")
        }
    }

    private fun bindParams(query: jakarta.persistence.Query, params: Map<String, Any?>) {
        params.forEach { (k, v) ->
            query.setParameter(k, v)
        }
    }
}