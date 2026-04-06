package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.OptionType
import com.tropig.backend.contents.entity.ContentOption

data class AdminContentOptionResponse(
    val id: Long,
    val type: OptionType,
    val name: String,
    val displayName: String,
    val show: Boolean,
    val sortOrder: Int,
)

fun ContentOption.toAdminResponse() = AdminContentOptionResponse(
    id = id,
    type = type,
    name = name,
    displayName = displayName,
    show = show,
    sortOrder = sortOrder,
)
