package com.tropig.backend.banner.repository

import com.tropig.backend.banner.entity.Banner
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BannerRepository : JpaRepository<Banner, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Banner?

    fun existsByAliasAndDeletedAtIsNull(alias: String): Boolean

    fun findAllByDeletedAtIsNullOrderByOrderNoAscIdDesc(): List<Banner>

    @Query(
        """
        SELECT b
        FROM Banner b
        WHERE b.deletedAt IS NULL
          AND b.show = true
          AND b.startedAt < :now
          AND b.endedAt > :now
        ORDER BY b.orderNo ASC, b.id DESC
        """,
    )
    fun findDisplayable(now: LocalDateTime): List<Banner>

    @Query(
        """
        SELECT b
        FROM Banner b
        WHERE b.alias = :alias
          AND b.deletedAt IS NULL
          AND b.show = true
          AND b.startedAt < :now
          AND b.endedAt > :now
        """,
    )
    fun findDisplayableBannerByAlias(alias: String, now: LocalDateTime): Banner?
}
