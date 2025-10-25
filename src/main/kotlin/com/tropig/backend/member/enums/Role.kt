package com.tropig.backend.member.enums

enum class Role(
    val isCreator: Boolean,
) {
    USER(false),
    TEAM(true),
    CREATOR(true),
    ADMIN(false)
}
