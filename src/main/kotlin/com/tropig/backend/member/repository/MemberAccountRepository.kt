package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.MemberAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberAccountRepository: JpaRepository<MemberAccount, Long>