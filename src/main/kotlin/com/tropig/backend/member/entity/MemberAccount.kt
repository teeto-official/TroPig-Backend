package com.tropig.backend.member.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 창작자 계좌 정보
 * 본인인증 완료 후 창작자 인증 시 생성
 */
@Entity
@Table(
    name = "member_account",
    indexes = [
        Index(name = "idx_member_account_member_id", columnList = "member_id"),
        Index(name = "idx_member_account_expires_at", columnList = "expires_at"),
        Index(name = "idx_member_account_portone_partner_id", columnList = "portone_partner_id"),
    ],
)
@EntityListeners(AuditingEntityListener::class)
data class MemberAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    @Column(name = "bank_name", nullable = false, length = 50)
    var bankName: String,

    @Column(name = "account_number_encrypted", nullable = false, length = 500)
    var accountNumberEncrypted: String, // AES-256-GCM encrypted

    @Column(name = "account_holder", nullable = false, length = 20)
    var accountHolder: String,

    @Column(name = "portone_partner_id", nullable = false, length = 100)
    val portonePartnerId: String,

    @Column(name = "verified_at", nullable = false)
    var verifiedAt: LocalDateTime,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime, // verifiedAt + 1 year

    @Column(name = "last_changed_at", nullable = false)
    var lastChangedAt: LocalDateTime = LocalDateTime.now(),

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        /**
         * 인증 유효 기간 (년)
         */
        const val VERIFICATION_VALIDITY_YEARS = 1L

        /**
         * 갱신 가능 기간 (일) - 만료 전 며칠부터 갱신 가능한지
         */
        const val RENEWAL_WINDOW_DAYS = 30L

        /**
         * 계좌 변경 잠금 기간 (일) - 변경 후 며칠간 재변경 불가
         */
        const val ACCOUNT_CHANGE_LOCKOUT_DAYS = 30L
    }

    /**
     * Check if verification has expired
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    /**
     * Check if renewal is allowed (within 30 days of expiry or already expired)
     */
    fun canRenew(): Boolean {
        val now = LocalDateTime.now()
        val daysUntilExpiry = ChronoUnit.DAYS.between(now, expiresAt)
        return daysUntilExpiry <= RENEWAL_WINDOW_DAYS
    }

    /**
     * Get days until expiry (negative if expired)
     */
    fun getDaysUntilExpiry(): Long {
        val now = LocalDateTime.now()
        return ChronoUnit.DAYS.between(now, expiresAt)
    }

    /**
     * Check if account can be changed (30 days since last change)
     */
    fun canChangeAccount(): Boolean {
        val now = LocalDateTime.now()
        val daysSinceLastChange = ChronoUnit.DAYS.between(lastChangedAt, now)
        return daysSinceLastChange >= ACCOUNT_CHANGE_LOCKOUT_DAYS
    }

    /**
     * Get next available account change time
     */
    fun getNextChangeAvailableAt(): LocalDateTime? = if (!canChangeAccount()) {
        lastChangedAt.plusDays(ACCOUNT_CHANGE_LOCKOUT_DAYS)
    } else {
        null
    }

    /**
     * Renew verification for another year
     */
    fun renew() {
        val now = LocalDateTime.now()
        this.verifiedAt = now
        this.expiresAt = now.plusYears(VERIFICATION_VALIDITY_YEARS)
    }

    /**
     * Update account information
     */
    fun updateAccount(bankName: String, accountNumberEncrypted: String, accountHolder: String) {
        this.bankName = bankName
        this.accountNumberEncrypted = accountNumberEncrypted
        this.accountHolder = accountHolder
        this.lastChangedAt = LocalDateTime.now()
    }
}
