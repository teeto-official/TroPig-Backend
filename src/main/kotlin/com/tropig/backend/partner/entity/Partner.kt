package com.tropig.backend.partner.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "partner")
data class Partner(
    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false)
    val portonePartnerId: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = true)
    var status: String = "PENDING", // PENDING, ACTIVE, INACTIVE, FAILED

    @Column(nullable = true)
    var failureReason: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}
