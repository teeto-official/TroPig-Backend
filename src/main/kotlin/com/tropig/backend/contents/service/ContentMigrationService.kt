package com.tropig.backend.contents.service

import com.tropig.backend.common.enums.OptionType
import com.tropig.backend.contents.repository.ContentOptionRepository
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class ContentMigrationService(
    private val entityManager: EntityManager,
    private val contentOptionRepository: ContentOptionRepository,
) {

    /**
     * content 테이블의 구 rule/genre 컬럼(enum 이름) 값을
     * rule_id/genre_id(content_option ID)로 일괄 변환합니다.
     *
     * 전제조건: content 테이블에 rule, genre, rule_id, genre_id 컬럼이 모두 존재해야 합니다.
     * 완료 후 content_tag_master_cleanup.sql 로 구 컬럼을 삭제하세요.
     *
     * @return 변환된 rule 건수, 변환된 genre 건수
     */
    @Transactional
    fun migrateRuleAndGenreToIds(): MigrateContentTagResult {
        val ruleNameToId = contentOptionRepository.findAllByType(OptionType.RULE)
            .associate { it.name to it.id }
        val genreNameToId = contentOptionRepository.findAllByType(OptionType.GENRE)
            .associate { it.name to it.id }

        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createNativeQuery(
            "SELECT id, rule, genre FROM content WHERE rule IS NOT NULL OR genre IS NOT NULL"
        ).resultList as List<Array<Any?>>

        var ruleCount = 0
        var genreCount = 0

        rows.forEach { row ->
            val contentId = (row[0] as Number).toLong()
            val rule = row[1] as? String
            val genre = row[2] as? String

            val ruleId = rule?.let { ruleNameToId[it] }
            val genreId = genre?.let { genreNameToId[it] }

            if (ruleId != null) {
                entityManager.createNativeQuery(
                    "UPDATE content SET rule_id = :ruleId WHERE id = :contentId AND rule_id IS NULL"
                ).setParameter("ruleId", ruleId)
                    .setParameter("contentId", contentId)
                    .executeUpdate()
                ruleCount++
            }

            if (genreId != null) {
                entityManager.createNativeQuery(
                    "UPDATE content SET genre_id = :genreId WHERE id = :contentId AND genre_id IS NULL"
                ).setParameter("genreId", genreId)
                    .setParameter("contentId", contentId)
                    .executeUpdate()
                genreCount++
            }
        }

        return MigrateContentTagResult(ruleCount = ruleCount, genreCount = genreCount)
    }
}

data class MigrateContentTagResult(
    val ruleCount: Int,
    val genreCount: Int,
)
