package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.PickContent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PickContentRepository: JpaRepository<PickContent, Long> {

    fun saveAll(pickContents: List<PickContent>): List<PickContent>
}