package com.tropig.backend.partner.entity

import com.tropig.backend.partner.enums.PartnerStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "partner")
@EntityListeners(AuditingEntityListener::class)
data class Partner(
    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false)
    val portonePartnerId: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PartnerStatus = PartnerStatus.PENDING,

    @Column(nullable = true)
    var failureReason: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}
