package com.tropig.backend.contents.model.result

import com.tropig.backend.contents.model.response.CountSearchContentResponse

data class CountSearchContentsResult(val scenarioCount: Long, val resourceCount: Long) {
    fun toResponse() = CountSearchContentResponse(
        this.scenarioCount,
        this.resourceCount,
    )
}
