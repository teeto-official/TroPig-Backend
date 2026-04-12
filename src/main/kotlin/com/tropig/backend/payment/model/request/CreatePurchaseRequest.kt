package com.tropig.backend.payment.model.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotNull

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreatePurchaseRequest(
    @field:NotNull(message = "콘텐츠 ID는 필수입니다")
    val contentId: Long,
)
