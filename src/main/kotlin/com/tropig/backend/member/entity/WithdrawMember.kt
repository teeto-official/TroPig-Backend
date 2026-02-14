package com.tropig.backend.member.entity

import com.tropig.backend.member.enums.SnsProvider
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "withdraw_member")
data class WithdrawMember(
    val memberId: Long,
    val snsId: String,
    @Enumerated(value = EnumType.STRING)
    val snsProvider: SnsProvider,
    val email: String,
    val nickname: String,
    val bio: String?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    val deletedAt: LocalDateTime = LocalDateTime.now()

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}
