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
    val orderId: String,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus,

    @Column(nullable = false)
    val currency: String = "KRW",

    @Column(nullable = true)
    var method: String? = null,

    @Column(nullable = true, unique = true)
    var paymentKey: String? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    var tossResponse: String? = null,

    @Column(nullable = true)
    var failureReason: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
