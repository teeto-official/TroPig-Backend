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
    val memberId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    var name: String? = null
    var di: String? = null
    var birthedAt: LocalDateTime? = null
    var authUserAt: LocalDateTime? = null

    var authCreatorAt: LocalDateTime? = null
    @Enumerated(value = EnumType.STRING)
    var bankCode: BankCode? = null
    var bankAccount: String? = null

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}
