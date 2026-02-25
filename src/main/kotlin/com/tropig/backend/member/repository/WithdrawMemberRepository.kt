package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.WithdrawMember
import org.springframework.data.jpa.repository.JpaRepository

interface WithdrawMemberRepository : JpaRepository<WithdrawMember, Long>
