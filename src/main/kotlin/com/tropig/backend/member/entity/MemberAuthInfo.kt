package com.tropig.backend.member.entity

import com.tropig.backend.common.enums.BankCode
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "member_auth_info")
data class MemberAuthInfo(
    val userId: Long,
    val di: String,
    val birthedAt: LocalDateTime,
    val authUserAt: LocalDateTime,
    val isCreator: Boolean,
    val authCreatorAt: LocalDateTime,
    val bankCode: BankCode,
    val bankAccount: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}
