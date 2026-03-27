package com.tropig.backend.contents.repository

import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import com.tropig.backend.contents.model.result.CountSearchContentsResult

interface ContentCustomRepository {

    fun searchContents(request: SearchContentRequestDto): CursorSlice<Content>
    fun countSearchContents(request: SearchContentRequestDto): CountSearchContentsResult

    fun findRandomGenreContents(type: ContentType, genreId: Long, isAdult: Boolean): List<Content>

    fun findRandomRuleContents(ruleId: Long, isAdult: Boolean): List<Content>

    fun findRandomContents(type: ContentType, isAdult: Boolean): List<Content>
}
