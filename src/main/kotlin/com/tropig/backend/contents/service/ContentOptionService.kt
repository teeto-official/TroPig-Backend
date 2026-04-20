package com.tropig.backend.contents.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.enums.OptionType
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.contents.entity.ContentOption
import com.tropig.backend.contents.model.request.AdminContentOptionBatchUpdateRequest
import com.tropig.backend.contents.model.request.AdminContentOptionRequest
import com.tropig.backend.contents.model.request.AdminContentOptionUpdateRequest
import com.tropig.backend.contents.repository.ContentOptionRepository
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

@Service
class ContentOptionService(
    private val contentOptionRepository: ContentOptionRepository,
) {

    fun getOptions(type: OptionType): List<ContentOption> =
        contentOptionRepository.findAllByType(type).sortedBy { it.sortOrder }

    @Transactional
    @CacheEvict(value = ["contentOptions"], allEntries = true)
    fun createOption(type: OptionType, request: AdminContentOptionRequest): ContentOption =
        contentOptionRepository.save(
            ContentOption(
                type = type,
                name = request.name,
                displayName = request.displayName,
                show = request.show,
                sortOrder = request.sortOrder,
            ),
        )

    @Transactional
    @CacheEvict(value = ["contentOptions"], allEntries = true)
    fun batchUpdateOptions(request: AdminContentOptionBatchUpdateRequest): List<ContentOption> {
        val optionById = contentOptionRepository.findAllById(request.items.map { it.id }).associateBy { it.id }
        val updatedOptions = request.items.map { item ->
            val existing = optionById[item.id] ?: throw optionNotFoundException()
            existing.copy(
                displayName = item.displayName,
                show = item.show,
                sortOrder = item.sortOrder,
            )
        }
        val savedOptions = contentOptionRepository.saveAll(updatedOptions).associateBy { it.id }
        return request.items.map { item -> savedOptions[item.id] ?: throw optionNotFoundException() }
    }

    @Transactional
    @CacheEvict(value = ["contentOptions"], allEntries = true)
    fun updateOption(id: Long, request: AdminContentOptionUpdateRequest): ContentOption {
        val existing = findOptionById(id)
        return contentOptionRepository.save(
            existing.copy(
                displayName = request.displayName,
                show = request.show,
                sortOrder = request.sortOrder,
            ),
        )
    }

    @Transactional
    @CacheEvict(value = ["contentOptions"], allEntries = true)
    fun deleteOption(id: Long) {
        contentOptionRepository.delete(findOptionById(id))
    }

    private fun findOptionById(id: Long): ContentOption =
        contentOptionRepository.findById(id).orElseThrow(::optionNotFoundException)

    private fun optionNotFoundException(): NotFoundException =
        NotFoundException("해당 옵션을 찾을 수 없습니다.", MessageCode.NOT_FOUND_CONTENT_INFO)
}
