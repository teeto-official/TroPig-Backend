package com.tropig.backend.contents.service

import com.tropig.backend.contents.entity.PickContent
import com.tropig.backend.contents.repository.PickContentRepository
import org.springframework.stereotype.Service

@Service
class PickContentService(
    private val pickContentRepository: PickContentRepository,
) {

    fun saveAllPickContents(pickContents: List<PickContent>): List<PickContent> =
        pickContentRepository.saveAll(pickContents)

    fun deleteAll() =
        pickContentRepository.deleteAll()
}