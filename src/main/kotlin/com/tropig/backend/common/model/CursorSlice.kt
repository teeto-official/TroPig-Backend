package com.tropig.backend.common.model

import java.time.LocalDateTime

data class CursorSlice<T>(
    val items: List<T>,   // 이번 페이지 결과
    val hasNext: Boolean, // 다음 페이지가 있는지
    val nextCursorId: Long? = null,
    val nextCursorDateAt: LocalDateTime? = null,
    val nextCursorTitle: String? = null
) {
    fun <R> map(transform: (T) -> R): CursorSlice<R> =
        CursorSlice(
            items = items.map(transform),
            hasNext = hasNext,
            nextCursorDateAt = nextCursorDateAt,
            nextCursorId = nextCursorId,
            nextCursorTitle = nextCursorTitle
        )

    /**
     * items에서 파생한 컨텍스트(추가 조회 결과 등)를 먼저 만들고,
     * 그 컨텍스트를 가지고 변환.
     */
    inline fun <C, R> mapWith(
        buildContext: (List<T>) -> C,
        crossinline transform: (T, C) -> R
    ): CursorSlice<R> {
        if (items.isEmpty()) {
            return CursorSlice(
                items = emptyList(),
                hasNext = hasNext,
                nextCursorId = nextCursorId,
                nextCursorTitle = nextCursorTitle,
                nextCursorDateAt = nextCursorDateAt
            )
        }


        val ctx = buildContext(items)
        return map { t -> transform(t, ctx) }
    }
}
