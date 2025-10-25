package com.tropig.backend.member.entity

import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.enums.SnsProvider
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "member")
data class Member(
    val snsId: String,
    val snsProvider: SnsProvider,

    @Column(nullable = false, unique = true)
    val email: String,
    
    @Column(nullable = false)
    val nickname: String,

    @Column(nullable = false)
    val role: Role,

    @Column(nullable = false)
    var isMarketing: Boolean = false,

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @Column(nullable = false)
    var isAdult: Boolean = false

    @Column(nullable = true)
    var bio: String? = null

    @Column(nullable = true)
    var marketingAt: LocalDateTime? = null

    @Column(nullable = true)
    var deletedAt: LocalDateTime? = null

    @Column(nullable = true)
    var favoriteGenres: String? = null

    @Column(nullable = true)
    var favoriteRules: String? = null

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}
