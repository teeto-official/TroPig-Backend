package com.tropig.backend.contents.service

import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.repository.ReportContentRepository
import com.tropig.backend.member.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class ReportService(
    private val memberRepository: MemberRepository,
    private val contentRepository: ContentRepository,
    private val reportContentRepository: ReportContentRepository,
) {
}