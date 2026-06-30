package com.tropig.backend.banner.entity

import com.tropig.backend.banner.enums.BannerType
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "banner")
data class Banner(
    var alias: String,
    var title: String,
    var subtitle: String?,
    @Enumerated(value = EnumType.STRING)
    var type: BannerType,
    var mobileImagePath: String?,
    var pcImagePath: String,
    var htmlPath: String?,
    var startedAt: LocalDateTime,
    var endedAt: LocalDateTime,
    var orderNo: Int,
    var show: Boolean,
    var lastModifiedAdminId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()

    var deletedAt: LocalDateTime? = null
}
