package com.tropig.backend.contents.enums

enum class ContentType {
    SCENARIO,
    RESOURCE,
    ;

    companion object {
        fun fromString(value: String): ContentType = valueOf(value.uppercase())
    }
}
