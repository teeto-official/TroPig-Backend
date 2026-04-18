package com.tropig.backend.contents.service

import com.tropig.backend.contents.entity.PickContent
import com.tropig.backend.contents.repository.PickContentRepository
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PickContentService(private val pickContentRepository: PickContentRepository) {

    @Transactional
    @CacheEvict(value = ["pickContent", "pickContentByType"], allEntries = true)
    fun updatePickContentsByType(
        existingContentIds: List<Long>,
        newPickContents: List<PickContent>,
    ): List<PickContent> {
        if (existingContentIds.isNotEmpty()) {
            pickContentRepository.deleteAllByContentIdIn(existingContentIds)
        }
        return pickContentRepository.saveAll(newPickContents)
    }

    @Cacheable(value = ["pickContent"])
    fun getAllPickContent(): List<PickContent> = pickContentRepository.findAll()
}
