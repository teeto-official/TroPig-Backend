package com.tropig.backend.user.enums

enum class Role(
    val isCreator: Boolean,
) {
    USER(false),
    TEAM(true),
    CREATOR(true),
    ADMIN(false)
}
