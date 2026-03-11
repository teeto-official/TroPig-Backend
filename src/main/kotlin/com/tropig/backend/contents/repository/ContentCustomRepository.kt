package com.tropig.backend.contents.repository

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import com.tropig.backend.contents.model.result.CountSearchContentsResult

interface ContentCustomRepository {

    fun searchContents(request: SearchContentRequestDto): CursorSlice<Content>
    fun countSearchContents(request: SearchContentRequestDto): CountSearchContentsResult

    fun findRandomGenreContent(genres: List<Genre>, isAdult: Boolean): Content

    fun findRandomRuleContent(rules: List<Rule>, isAdult: Boolean): Content

    fun findRandomContent(isAdult: Boolean): Content
}
