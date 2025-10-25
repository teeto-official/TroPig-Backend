package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.MemberAuthInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberAuthInfoRepository: JpaRepository<MemberAuthInfo, Long>