package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.Content
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContentRepository: JpaRepository<Content, Long> {
}