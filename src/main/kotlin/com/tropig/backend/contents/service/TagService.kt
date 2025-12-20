package com.tropig.backend.contents.service

import com.tropig.backend.contents.repository.TagRepository
import org.springframework.stereotype.Service

@Service
class TagService(
    private val tagRepository: TagRepository
) {
}