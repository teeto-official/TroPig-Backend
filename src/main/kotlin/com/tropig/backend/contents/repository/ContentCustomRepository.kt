package com.tropig.backend.contents.repository

import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.request.SearchContentRequest
import com.tropig.backend.contents.model.request.SearchOptionalContentRequest

interface ContentCustomRepository {

    fun searchContents(request: SearchContentRequest, type: ContentType): CursorSlice<Content>

    fun searchOptionalContents(request: SearchOptionalContentRequest, type: ContentType): CursorSlice<Content>
}