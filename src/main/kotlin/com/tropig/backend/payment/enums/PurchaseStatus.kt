package com.tropig.backend.payment.enums

enum class PurchaseStatus {
    PENDING, // 대기 중
    COMPLETED, // 구매 완료
    CANCELLED, // 취소됨
    REFUNDED, // 환불됨
}
