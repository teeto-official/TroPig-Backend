package com.tropig.backend.contents.service

import com.tropig.backend.contents.repository.FavoriteContentRepository
import org.springframework.stereotype.Service

@Service
class FavoriteContentService(private val favoriteContentRepository: FavoriteContentRepository) {

    fun getFavoriteCountByContentIds(contentIds: List<Long>): Map<Long, Long> =
        favoriteContentRepository.countByContentIdsIn(contentIds)
            .map { it.contentId to it.count }
            .associateBy({ it.first }, { it.second })
}
