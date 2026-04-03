package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.util.EncryptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.security.MessageDigest

@ApiController
@RequestMapping("/api/admin/migration")
@Tag(name = "Admin Migration", description = "데이터 마이그레이션 API")
class AdminEncryptionMigrationController(
    private val encryptionService: EncryptionService,
    @PersistenceContext private val em: EntityManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/encrypt-personal-info")
    @Operation(
        summary = "개인정보 암호화 마이그레이션",
        description = "기존 평문 개인정보를 AES-256-GCM으로 암호화하고 해시 컬럼을 채웁니다. 멱등성 보장(이미 처리된 행은 건너뜀).",
    )
    @Transactional
    fun migrateEncryption(): Map<String, Any> {
        val memberResult = migrateMemberEmail()
        val authInfoResult = migrateMemberAuthInfo()
        val accountResult = migrateMemberAccountHolder()

        return mapOf(
            "member" to memberResult,
            "memberAuthInfo" to authInfoResult,
            "memberAccount" to accountResult,
        )
    }

    private fun migrateMemberEmail(): Map<String, Int> {
        // email_hash가 NULL인 행만 = 아직 마이그레이션 안 된 행
        @Suppress("UNCHECKED_CAST")
        val rows = em.createNativeQuery(
            "SELECT id, email FROM member WHERE email_hash IS NULL",
        ).resultList as List<Array<Any?>>

        var count = 0
        for (row in rows) {
            val id = (row[0] as Number).toLong()
            val email = row[1] as String

            try {
                val encrypted = encryptionService.encrypt(email)
                val hash = sha256(email)

                em.createNativeQuery(
                    "UPDATE member SET email = :encrypted, email_hash = :hash WHERE id = :id",
                ).setParameter("encrypted", encrypted)
                    .setParameter("hash", hash)
                    .setParameter("id", id)
                    .executeUpdate()

                count++
            } catch (e: Exception) {
                logger.error("Failed to encrypt member email: id=$id", e)
            }
        }

        logger.info("Member email migration completed: $count/${rows.size}")
        return mapOf("total" to rows.size, "migrated" to count)
    }

    private fun migrateMemberAuthInfo(): Map<String, Int> {
        // phone_number_hash가 NULL인 행만
        @Suppress("UNCHECKED_CAST")
        val rows = em.createNativeQuery(
            "SELECT id, name, birth_date, phone_number, bank_account FROM member_auth_info WHERE phone_number_hash IS NULL",
        ).resultList as List<Array<Any?>>

        var count = 0
        for (row in rows) {
            val id = (row[0] as Number).toLong()
            val name = row[1] as String
            val birthDate = row[2] as String
            val phoneNumber = row[3] as String
            val bankAccount = row[4] as? String

            try {
                val encName = encryptionService.encrypt(name)
                val encBirthDate = encryptionService.encrypt(birthDate)
                val encPhone = encryptionService.encrypt(phoneNumber)
                val encBank = bankAccount?.let { encryptionService.encrypt(it) }
                val phoneHash = sha256(phoneNumber)

                em.createNativeQuery(
                    """
                    UPDATE member_auth_info
                    SET name = :name, birth_date = :birthDate, phone_number = :phone,
                        bank_account = :bank, phone_number_hash = :phoneHash
                    WHERE id = :id
                    """.trimIndent(),
                ).setParameter("name", encName)
                    .setParameter("birthDate", encBirthDate)
                    .setParameter("phone", encPhone)
                    .setParameter("bank", encBank)
                    .setParameter("phoneHash", phoneHash)
                    .setParameter("id", id)
                    .executeUpdate()

                count++
            } catch (e: Exception) {
                logger.error("Failed to encrypt member_auth_info: id=$id", e)
            }
        }

        logger.info("MemberAuthInfo migration completed: $count/${rows.size}")
        return mapOf("total" to rows.size, "migrated" to count)
    }

    private fun migrateMemberAccountHolder(): Map<String, Int> {
        // account_holder 길이가 100 미만이면 아직 평문 (암호화된 Base64는 항상 더 김)
        @Suppress("UNCHECKED_CAST")
        val rows = em.createNativeQuery(
            "SELECT id, account_holder FROM member_account WHERE LENGTH(account_holder) < 100",
        ).resultList as List<Array<Any?>>

        var count = 0
        for (row in rows) {
            val id = (row[0] as Number).toLong()
            val accountHolder = row[1] as String

            try {
                val encrypted = encryptionService.encrypt(accountHolder)

                em.createNativeQuery(
                    "UPDATE member_account SET account_holder = :encrypted WHERE id = :id",
                ).setParameter("encrypted", encrypted)
                    .setParameter("id", id)
                    .executeUpdate()

                count++
            } catch (e: Exception) {
                logger.error("Failed to encrypt member_account holder: id=$id", e)
            }
        }

        logger.info("MemberAccount holder migration completed: $count/${rows.size}")
        return mapOf("total" to rows.size, "migrated" to count)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
