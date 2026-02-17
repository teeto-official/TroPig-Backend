package com.tropig.backend.payment.entity

import com.tropig.backend.payment.enums.PaymentStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "payment")
data class Payment(
    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false)
    val contentId: Long,

    @Column(nullable = false, unique = true)
    val portonePaymentId: String, // 포트원 결제 ID

    @Column(nullable = false)
    val amount: Long, // 결제 금액 (원)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus, // 결제 상태

    @Column(nullable = false)
    val currency: String = "KRW", // 통화

    @Column(nullable = true)
    var method: String? = null, // 결제 수단 (카드, 계좌이체 등)

    @Column(nullable = true)
    val channelKey: String? = null, // 채널 키

    @Column(nullable = true)
    val storeId: String? = null, // 스토어 ID

    @Column(nullable = true)
    var receiptId: String? = null, // 포트원 트랜잭션 ID

    @Column(nullable = true, columnDefinition = "TEXT")
    var portoneResponse: String? = null, // 포트원 응답 전체 (JSON)

    @Column(nullable = true)
    var failureReason: String? = null, // 실패 사유
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
    
    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()
    
    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
