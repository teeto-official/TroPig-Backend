package com.tropig.backend.payment.enums

enum class PaymentStatus {
    PENDING,    // 대기 중
    PAID,       // 결제 완료
    FAILED,     // 결제 실패
    CANCELLED,  // 취소됨
    REFUNDED    // 환불됨
}