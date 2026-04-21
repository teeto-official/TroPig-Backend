package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.PickContent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PickContentRepository : JpaRepository<PickContent, Long> {

    @Modifying
    @Query("DELETE FROM PickContent p WHERE p.contentId IN :contentIds")
    fun deleteAllByContentIdIn(@Param("contentIds") contentIds: List<Long>)
}
