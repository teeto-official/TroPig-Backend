package com.tropig.backend.user.entity

import com.tropig.backend.user.enums.Role
import com.tropig.backend.user.enums.SnsProvider
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

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

    @Column(name = "created_at")
    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
