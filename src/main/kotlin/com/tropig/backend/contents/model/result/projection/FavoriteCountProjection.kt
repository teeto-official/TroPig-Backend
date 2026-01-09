package com.tropig.backend.contents.model.result.projection

interface FavoriteCountProjection {
    val contentId: Long
    val count: Long
}