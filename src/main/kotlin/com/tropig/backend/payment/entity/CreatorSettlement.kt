package com.tropig.backend.payment.entity

import com.tropig.backend.payment.enums.WithdrawalStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "creator_settlement")
data class CreatorSettlement(
    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = true)
    val description: String? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: WithdrawalStatus = WithdrawalStatus.PENDING,

    @Column(nullable = true)
    val settlementDate: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
