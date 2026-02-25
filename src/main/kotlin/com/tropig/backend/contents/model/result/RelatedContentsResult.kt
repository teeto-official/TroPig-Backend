package com.tropig.backend.contents.model.result

import com.tropig.backend.contents.entity.Content

data class RelatedContentsResult(
    val scenarios: List<Content>,
    val resources: List<Content>,
)

