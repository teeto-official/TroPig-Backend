package com.tropig.backend.payment.entity

import com.tropig.backend.payment.enums.PurchaseStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "purchase")
data class Purchase(
    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false)
    val contentId: Long,

    @Column(nullable = false)
    val paymentId: Long, // Payment 엔티티 참조

    @Column(nullable = false)
    val amount: Long, // 구매 금액

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PurchaseStatus, // 구매 상태
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
