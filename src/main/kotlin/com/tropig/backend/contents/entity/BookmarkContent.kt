package com.tropig.backend.contents.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "bookmark_content",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_bookmark_content_member_content",
            columnNames = ["member_id", "content_id"]
        )
    ]
)
data class BookmarkContent(
    val contentId: Long,
    val memberId: Long,
    val deleted: Boolean,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now()
}
