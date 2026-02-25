package com.tropig.backend.payment.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
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
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()
}
