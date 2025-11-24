package com.tropig.backend.contents.model

import com.tropig.backend.contents.enums.PublishingType
import kotlinx.serialization.Serializable

@Serializable
data class PublishingInfo(
    var type: PublishingType,
    var path: String?,
)