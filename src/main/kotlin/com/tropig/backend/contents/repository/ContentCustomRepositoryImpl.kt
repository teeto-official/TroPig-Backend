package com.tropig.backend.contents.repository

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import com.tropig.backend.contents.model.result.CountSearchContentsResult
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import kotlin.math.min

@Repository
class ContentCustomRepositoryImpl(@PersistenceContext private val em: EntityManager) : ContentCustomRepository {
    private companion object {
        const val RANDOM_CONTENT_LIMIT = 8
    }

    override fun searchContents(request: SearchContentRequestDto): CursorSlice<Content> {
        val common = request.toCommon()

        return runSearchSlice(
            common = common,
            type = request.type,
            sortSpec = request.sortSpec(),
            filterAppender = request.filterAppender(),
        )
    }

    override fun countSearchContents(request: SearchContentRequestDto): CountSearchContentsResult {
        val common = request.toCommon()

        return runSearchCount(
            common = common,
            publishingTypes = request.publishingTypes,
            filterAppender = request.filterAppender(),
        )
    }

    override fun findRandomGenreContents(type: ContentType, genres: List<Genre>, isAdult: Boolean): List<Content> {
        var sql = """
            SELECT *
            FROM content c
            WHERE
                c.type = :type
            AND c.status = :status
            AND c.genre IN (:genres)
            {adultCondition}
            ORDER BY RANDOM()
        """.trimIndent()

        val params = mutableMapOf<String, Any?>(
            "type" to type.name,
            "status" to ContentsStatus.PUBLISHED.name,
            "genres" to genres.map { it.name },
        )

        sql = sql.replace("{adultCondition}", if (isAdult) "" else "AND c.adult = false")
        return executeRandomList(sql, params)
    }

    override fun findRandomRuleContents(rules: List<Rule>, isAdult: Boolean): List<Content> {
        var sql = """
            SELECT *
            FROM content c
            WHERE
                c.type = :type
            AND c.status = :status
            AND c.rule IN (:rules)
            {adultCondition}
            ORDER BY RANDOM()
        """.trimIndent()

        val params = mutableMapOf<String, Any?>(
            "type" to ContentType.SCENARIO.name,
            "status" to ContentsStatus.PUBLISHED.name,
            "rules" to rules.map { it.name },
        )

        sql = sql.replace("{adultCondition}", if (isAdult) "" else "AND c.adult = false")
        return executeRandomList(sql, params)
    }

    override fun findRandomContents(type: ContentType, isAdult: Boolean): List<Content> {
        var sql = """
            SELECT *
            FROM content c
            WHERE
                c.type = :type
            AND c.status = :status
            {adultCondition}
            ORDER BY RANDOM()
        """.trimIndent()

        val params = mutableMapOf<String, Any?>(
            "type" to type.name,
            "status" to ContentsStatus.PUBLISHED.name,
        )

        sql = sql.replace("{adultCondition}", if (isAdult) "" else "AND c.adult = false")
        return executeRandomList(sql, params)
    }

    private data class CommonSearchReq(
        val size: Int,
        val isAdult: Boolean,
        val searchText: String? = null,
        val sortMode: SortMode,
        val cursorId: Long,
        val cursorDateAt: LocalDateTime?,
        val cursorTitle: String?,
        val isIncludeTags: Boolean,
    )

    private fun SearchContentRequestDto.toCommon(): CommonSearchReq = CommonSearchReq(
        size = this.size,
        isAdult = this.isAdult,
        searchText = this.searchText,
        sortMode = this.sortMode,
        cursorId = this.cursorId,
        cursorDateAt = this.cursorPublishedAt,
        cursorTitle = this.cursorTitle,
        isIncludeTags = !this.tags.isNullOrEmpty(),
    )

    private fun SearchContentRequestDto.sortSpec(): SortSpec = when (this.sortMode) {
        SortMode.LATEST -> SortSpec.latest()
        SortMode.TITLE -> SortSpec.title()
        SortMode.OLDEST -> SortSpec.oldest()
    }

    /**
     * search/count에서 중복되던 필터 로직을 request 확장으로 뽑아둠
     */
    private fun SearchContentRequestDto.filterAppender(): (
        StringBuilder,
        MutableMap<String, Any?>,
    ) -> Unit =
        { sql, params ->
            appendInEnum(sql, params, "c.rule", "rules", this.rules)
            appendInEnum(sql, params, "c.genre", "genres", this.genres)
            appendInEnum(sql, params, "c.player_count_type", "playerCountTypes", this.playerCountTypes)

            // tag 필터가 있으면 ct 조인이 필요 (buildBase...에서 isIncludeTags로 조인 여부 결정)
            appendInEnum(sql, params, "ct.tag_id", "tagIds", this.tags)

            if (this.level?.size != 4) {
                appendInEnum(sql, params, "c.level", "level", this.level)
            }

            appendInEnum(sql, params, "c.publishing_type", "publishingTypes", this.publishingTypes)
        }

    private fun runSearchSlice(
        common: CommonSearchReq,
        type: ContentType,
        sortSpec: SortSpec,
        filterAppender: (StringBuilder, MutableMap<String, Any?>) -> Unit,
    ): CursorSlice<Content> {
        val size = min(common.size.coerceAtLeast(1), 30)
        val fetchSize = size + 1

        val (sql, params) = buildBaseSelectSqlCommon(common, type)
        filterAppender(sql, params)

        sortSpec.appendCursor(sql, params, common)
        sortSpec.appendOrderBy(sql)

        sql.append("\nLIMIT :limit")
        params["limit"] = fetchSize

        val rows = executeList(sql.toString(), params)
        val hasNext = rows.size > size
        val items = if (hasNext) rows.take(size) else rows

        val last = items.lastOrNull()
        return sortSpec.buildSlice(items, hasNext, last)
    }

    private fun runSearchCount(
        common: CommonSearchReq,
        publishingTypes: List<PublishingType>? = null,
        filterAppender: (StringBuilder, MutableMap<String, Any?>) -> Unit,
    ): CountSearchContentsResult {
        val (sql, params) = buildBaseCountSqlCommon(common, publishingTypes)
        filterAppender(sql, params)

        val (scenarioCount, resourceCount) = executeCountPair(sql.toString(), params)

        return CountSearchContentsResult(
            scenarioCount = scenarioCount,
            resourceCount = resourceCount,
        )
    }

    /**
     * ===== Base SQL Builders =====
     */
    private fun buildBaseSelectSqlCommon(
        common: CommonSearchReq,
        type: ContentType,
    ): Pair<StringBuilder, MutableMap<String, Any?>> {
        val sql = StringBuilder(
            """
            SELECT c.*
            FROM content c
            """.trimIndent(),
        )

        if (common.isIncludeTags) {
            sql.append("\nINNER JOIN content_tag ct ON c.id = ct.content_id")
        }

        sql.append(
            """
            
            WHERE c.type = :type
              AND c.status = :status
            """.trimIndent(),
        )

        val params = mutableMapOf<String, Any?>(
            "type" to type.name,
            "status" to ContentsStatus.PUBLISHED.name,
        )

        if (!common.isAdult) {
            sql.append("\n  AND c.adult = false")
        }

        appendKeywordClause(sql, params, common.searchText)
        return sql to params
    }

    private fun buildBaseCountSqlCommon(
        common: CommonSearchReq,
        publishingTypes: List<PublishingType>? = null,
    ): Pair<StringBuilder, MutableMap<String, Any?>> {
        val resourceCase = if (!publishingTypes.isNullOrEmpty()) {
            "COUNT(DISTINCT CASE WHEN c.type = 'RESOURCE' AND c.publishing_type IN (:countPublishingTypes) THEN c.id END)"
        } else {
            "COUNT(DISTINCT CASE WHEN c.type = 'RESOURCE' THEN c.id END)"
        }

        val sql = StringBuilder(
            """
            SELECT
                COUNT(DISTINCT CASE WHEN c.type = 'SCENARIO' THEN c.id END) AS scenario_count,
                $resourceCase AS resource_count
            FROM content c
            """.trimIndent(),
        )

        if (common.isIncludeTags) {
            sql.append("\nINNER JOIN content_tag ct ON c.id = ct.content_id")
        }

        sql.append(
            """
            
            WHERE c.status = :status
            """.trimIndent(),
        )

        val params = mutableMapOf<String, Any?>(
            "status" to ContentsStatus.PUBLISHED.name,
        )

        if (!publishingTypes.isNullOrEmpty()) {
            params["countPublishingTypes"] = publishingTypes.map { it.name }
        }

        if (!common.isAdult) {
            sql.append("\n  AND c.adult = false")
        }

        appendKeywordClause(sql, params, common.searchText)

        return sql to params
    }

    /**
     * ===== Execute helpers =====
     */
    @Suppress("UNCHECKED_CAST")
    private fun executeList(sql: String, params: Map<String, Any?>): List<Content> {
        val q = em.createNativeQuery(sql, Content::class.java)
        params.forEach { (k, v) -> q.setParameter(k, v) }
        return q.resultList as List<Content>
    }

    private fun executeRandomList(sql: String, params: Map<String, Any?>): List<Content> {
        val q = em.createNativeQuery(sql, Content::class.java)
        params.forEach { (k, v) -> q.setParameter(k, v) }
        q.maxResults = RANDOM_CONTENT_LIMIT
        @Suppress("UNCHECKED_CAST")
        return q.resultList as List<Content>
    }

    private fun executeCountPair(sql: String, params: Map<String, Any?>): Pair<Long, Long> {
        val q = em.createNativeQuery(sql)
        params.forEach { (k, v) -> q.setParameter(k, v) }

        val arr = when (val row = q.singleResult) {
            is Array<*> -> row
            is Any -> (row as Array<*>) // 방어용
            else -> error("Unexpected count result type: ${row::class}")
        }

        val scenario = (arr[0] as Number).toLong()
        val resource = (arr[1] as Number).toLong()
        return scenario to resource
    }

    private data class SortSpec(
        val mode: SortMode,
        val appendCursor: (StringBuilder, MutableMap<String, Any?>, CommonSearchReq) -> Unit,
        val appendOrderBy: (StringBuilder) -> Unit,
        val buildSlice: (List<Content>, Boolean, Content?) -> CursorSlice<Content>,
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
                        nextCursorId = last?.id,
                    )
                },
            )

            fun oldest(): SortSpec = SortSpec(
                mode = SortMode.OLDEST,
                appendCursor = { sql, params, req ->
                    if (req.cursorDateAt != null && req.cursorId != 0L) {
                        sql.append("\n  AND (c.published_at, c.id) > (:cursorPublishedAt, :cursorId)")
                        params["cursorPublishedAt"] = req.cursorDateAt
                        params["cursorId"] = req.cursorId
                    }
                },
                appendOrderBy = { sql ->
                    sql.append("\nORDER BY c.published_at ASC NULLS LAST, c.id ASC")
                },
                buildSlice = { items, hasNext, last ->
                    CursorSlice(
                        items = items,
                        hasNext = hasNext,
                        nextCursorDateAt = last?.publishedAt,
                        nextCursorId = last?.id,
                    )
                },
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
                        nextCursorId = last?.id,
                    )
                },
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

    private fun appendInEnum(
        sql: StringBuilder,
        params: MutableMap<String, Any?>,
        column: String,
        paramName: String,
        values: Collection<Any>?,
    ) {
        if (values.isNullOrEmpty()) return
        val converted = values.mapNotNull { toDbParamValue(it) }
        if (converted.isEmpty()) return

        sql.append("\n  AND $column IN (:$paramName)")
        params[paramName] = converted
    }

    private fun toDbParamValue(value: Any?): Any? = when (value) {
        null -> null
        is Enum<*> -> value.name
        is Int, is Long, is String -> value
        else -> error("지원하지 않는 타입: ${value::class}")
    }
}
