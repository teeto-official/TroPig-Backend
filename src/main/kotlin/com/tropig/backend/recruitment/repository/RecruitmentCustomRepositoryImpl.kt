package com.tropig.backend.recruitment.repository

import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.model.dto.SearchRecruitmentDto
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RecruitmentCustomRepositoryImpl(@PersistenceContext private val em: EntityManager) : RecruitmentCustomRepository {
    override fun searchRecruitments(request: SearchRecruitmentDto): Page<Recruitment> {
        val params = mutableMapOf<String, Any>()
        val conditions = StringBuilder()

        request.keyword?.takeUnless { it.isBlank() }?.let {
            conditions.append(" AND r.title ILIKE :keyword")
            params["keyword"] = "%${escapeLike(it.trim())}%"
        }

        request.ruleIds?.takeUnless { it.isEmpty() }?.let { ruleIds ->
            val ruleConditions = ruleIds.mapIndexed { index, ruleId ->
                params["rule$index"] = """[{"ruleId": $ruleId}]"""
                "r.details -> 'rules' @> CAST(:rule$index AS jsonb)"
            }
            conditions.append(" AND (${ruleConditions.joinToString(" OR ")})")
        }

        request.environments?.takeUnless { it.isEmpty() }?.let { environments ->
            val envConditions = environments.mapIndexed { index, environment ->
                params["env$index"] = """["${environment.name}"]"""
                "r.details -> 'environments' @> CAST(:env$index AS jsonb)"
            }
            conditions.append(" AND (${envConditions.joinToString(" OR ")})")
        }

        val baseSql = """
            FROM recruitment r
            WHERE r.deleted_at IS NULL
            $conditions
        """.trimIndent()

        val countQuery = em.createNativeQuery("SELECT COUNT(*) $baseSql")
        params.forEach { (key, value) -> countQuery.setParameter(key, value) }
        val total = (countQuery.singleResult as Number).toLong()

        val listSql = """
            SELECT r.* $baseSql
            ORDER BY
                CASE WHEN r.status = :recruitingStatus AND r.deadline_at > :now THEN 0 ELSE 1 END,
                CASE WHEN r.status = :recruitingStatus AND r.deadline_at > :now THEN r.deadline_at END ASC,
                r.deadline_at DESC,
                r.id DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val listQuery = em.createNativeQuery(listSql, Recruitment::class.java)
        params.forEach { (key, value) -> listQuery.setParameter(key, value) }
        listQuery.setParameter("recruitingStatus", RecruitmentStatus.RECRUITING.name)
        listQuery.setParameter("now", LocalDateTime.now())
        listQuery.setParameter("limit", request.size)
        listQuery.setParameter("offset", request.page.toLong() * request.size)

        @Suppress("UNCHECKED_CAST")
        val items = listQuery.resultList as List<Recruitment>

        return PageImpl(items, PageRequest.of(request.page, request.size), total)
    }

    private fun escapeLike(keyword: String): String = keyword
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}
