package com.tropig.backend.common.enums

enum class Genre {
    ROMANCE,
    COMIC,
    ;

    companion object {
        fun fromList(genres: String?): List<Genre> {
            return genres?.split(",")
                ?.map { Genre.valueOf(it) }
                ?: emptyList()
        }
    }
}