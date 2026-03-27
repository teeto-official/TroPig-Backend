package com.tropig.backend.member.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter

/**
 * 본인인증 정보 엔티티
 * 사용자의 신원 확인 정보를 저장합니다.
 * PortOne Identity Verification API를 통해 검증된 정보입니다.
 */
@Entity
@Table(
    name = "member_auth_info",
    indexes = [
        Index(name = "idx_member_auth_info_member_id", columnList = "member_id", unique = true),
        Index(name = "idx_member_auth_info_phone_number", columnList = "phone_number"),
    ],
)
data class MemberAuthInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    @Column(nullable = false, length = 20)
    val name: String,

    @Column(name = "birth_date", nullable = false, length = 10)
    val birthDate: String, // YYYY-MM-DD format

    @Column(name = "phone_number", nullable = false, length = 11)
    var phoneNumber: String, // 01012345678 format (no hyphens)

    @Column(length = 64, unique = true)
    var ci: String? = null, // SHA-256 hash of CI

    @Column(length = 64, unique = true)
    var di: String? = null, // SHA-256 hash of DI

    @Column(name = "verified_at", nullable = false)
    var verifiedAt: LocalDateTime,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var creator: Boolean = false,

    @Column(name = "auth_creator_at")
    var authCreatorAt: LocalDateTime? = null,

    @Column(name = "bank_account", length = 255)
    var bankAccount: String? = null,

    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /**
         * 휴대폰 번호를 마스킹 처리합니다.
         * 예: "01012345678" -> "010-****-5678"
         */
        fun maskPhoneNumber(phone: String): String {
            require(phone.length >= 10) { "Phone number must be at least 10 digits" }
            return "${phone.substring(0, 3)}-****-${phone.substring(phone.length - 4)}"
        }

        /**
         * 생년월일로부터 나이를 계산합니다.
         */
        fun calculateAge(birthDate: String): Int {
            val birth = java.time.LocalDate.parse(birthDate, DATE_FORMATTER)
            val now = java.time.LocalDate.now()
            return Period.between(birth, now).years
        }

        /**
         * 성인 여부를 판단합니다 (만 19세 이상).
         */
        fun isAdult(birthDate: String): Boolean = calculateAge(birthDate) >= 19

        /**
         * SHA-256 해시를 생성합니다 (CI/DI 단방향 해시 저장용).
         */
        fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(value.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * 현재 나이를 반환합니다.
     */
    fun getAge(): Int = calculateAge(birthDate)

    /**
     * 성인 여부를 반환합니다.
     */
    fun isAdult(): Boolean = isAdult(birthDate)

    /**
     * 마스킹 처리된 휴대폰 번호를 반환합니다.
     */
    fun getMaskedPhoneNumber(): String = maskPhoneNumber(phoneNumber)
}
