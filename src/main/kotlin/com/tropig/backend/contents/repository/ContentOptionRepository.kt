package com.tropig.backend.contents.repository

import com.tropig.backend.common.enums.OptionType
import com.tropig.backend.contents.entity.ContentOption
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository

interface ContentOptionRepository : JpaRepository<ContentOption, Long> {

    @Cacheable(cacheNames = ["contentOptions"])
    fun findAllByShowTrue(): List<ContentOption>

    fun findAllByType(type: OptionType): List<ContentOption>
}
