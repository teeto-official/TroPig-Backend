package com.tropig.backend.contents.service

import com.tropig.backend.contents.entity.PickContent
import com.tropig.backend.contents.repository.PickContentRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class PickContentService(
    private val pickContentRepository: PickContentRepository,
) {

    @Transactional
    fun updatePickContents(pickContents: List<PickContent>): List<PickContent> {
        pickContentRepository.deleteAll()
        return pickContentRepository.saveAll(pickContents)
    }
}