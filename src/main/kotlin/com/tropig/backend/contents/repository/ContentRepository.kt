package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContentRepository :
    JpaRepository<Content, Long>,
    ContentCustomRepository {

    fun findByIdInAndType(ids: List<Long>, type: ContentType): List<Content>

    fun findContentsByIdInAndTypeAndAdult(ids: List<Long>, type: ContentType, adult: Boolean): List<Content>

    fun findByMemberId(memberId: Long): List<Content>

    fun findByAliasAndStatusNot(alias: String, status: ContentsStatus): Content?

    fun findByAliasAndStatusIn(alias: String, status: List<ContentsStatus>): Content?

    fun findByIdAndStatusNot(id: Long, status: ContentsStatus): Content?

    fun findByIdAndStatus(id: Long, status: ContentsStatus): Content?

    fun findByMemberIdAndTypeAndStatusIn(memberId: Long, type: ContentType, status: List<ContentsStatus>): List<Content>

    fun findByMemberIdAndStatus(memberId: Long, status: ContentsStatus): List<Content>

    fun findByMemberIdAndStatusIn(memberId: Long, status: List<ContentsStatus>): List<Content>

    fun findTop20ByTypeAndStatusOrderByPublishedAtDesc(type: ContentType, status: ContentsStatus): List<Content>

    fun findTop20ByTypeAndStatusAndAdultOrderByPublishedAtDesc(
        type: ContentType,
        status: ContentsStatus,
        adult: Boolean,
    ): List<Content>
}
