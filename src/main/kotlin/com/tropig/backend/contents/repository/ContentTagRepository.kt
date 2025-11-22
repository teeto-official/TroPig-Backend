package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.ContentTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContentTagRepository: JpaRepository<ContentTag, Long> {
}