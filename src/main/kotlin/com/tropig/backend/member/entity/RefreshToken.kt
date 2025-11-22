package com.tropig.backend.member.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "refresh_token")
data class RefreshToken(
    val memberId: Long,
    val token: String,
    val expiryDate: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}