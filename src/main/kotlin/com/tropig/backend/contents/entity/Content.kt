package com.tropig.backend.contents.entity

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.*
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Duration
import java.time.LocalDateTime

@Entity
@Table(name = "content")
data class Content(
    var alias: String,
    var title: String,
    var type: ContentType,
    val memberId: Long,
    var rule: Rule,
    var genre: Genre,
    var referralType: ReferralType,
    var termType: TermType,
    var publishingType: PublishingType?,
    var publishingPath: String?,
    var status: ContentsStatus,
    var publishedAt: LocalDateTime?,
    var freeContent: String?,
    var nonFreeContent: String?,
    var price: Double,
    var level: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now()
}
