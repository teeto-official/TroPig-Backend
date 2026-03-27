package com.tropig.backend.contents.entity

import com.tropig.backend.contents.enums.*
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "content")
data class Content(
    var alias: String,
    var title: String,
    @Enumerated(value = EnumType.STRING)
    var type: ContentType,
    val memberId: Long,
    var ruleId: Long? = null,
    var genreId: Long? = null,
    @Enumerated(value = EnumType.STRING)
    var playerCountType: PlayerCountType,
    @Enumerated(value = EnumType.STRING)
    var termType: TermType,
    @Enumerated(value = EnumType.STRING)
    var publishingType: PublishingType,
    var publishingInfo: String?,
    @Enumerated(value = EnumType.STRING)
    var status: ContentsStatus,
    var adult: Boolean,
    var publishedAt: LocalDateTime?,
    @Column(columnDefinition = "TEXT")
    var freeContent: String?,
    var nonFreeContent: String?,
    var price: Double,
    var level: Int,
    var searchText: String,
    var preventRightClick: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now()

    companion object {
        private val SPOILER_REGEX = Regex("<spoiler>.*?</spoiler>", RegexOption.DOT_MATCHES_ALL)

        fun removeSpoilers(content: String?): String? =
            content?.replace(SPOILER_REGEX, "")?.trim()?.ifEmpty { null }
    }
}
