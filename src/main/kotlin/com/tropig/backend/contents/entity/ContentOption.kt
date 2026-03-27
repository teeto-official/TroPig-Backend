package com.tropig.backend.contents.entity

import com.tropig.backend.common.enums.OptionType
import jakarta.persistence.*

@Entity
@Table(name = "content_option")
data class ContentOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(value = EnumType.STRING)
    val type: OptionType,
    val name: String,
    val displayName: String,
    val show: Boolean = true,
    val sortOrder: Int = 0,
)
