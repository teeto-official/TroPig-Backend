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
    fun updatePickContents(pickContents: List<PickContent>): List<PickContent> {
        pickContentRepository.deleteAll()
        return pickContentRepository.saveAll(pickContents)
    }

    @Cacheable(value = ["pickContent"])
    fun getAllPickContent(): List<PickContent> = pickContentRepository.findAll()
}
