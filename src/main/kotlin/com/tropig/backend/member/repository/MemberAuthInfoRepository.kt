package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.MemberAuthInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberAuthInfoRepository : JpaRepository<MemberAuthInfo, Long> {
    /**
     * memberId로 본인인증 정보를 조회합니다.
     */
    fun findByMemberId(memberId: Long): MemberAuthInfo?

    /**
     * memberId로 존재 여부를 확인합니다.
     */
    fun existsByMemberId(memberId: Long): Boolean

    /**
     * CI(Correlation ID)로 본인인증 정보를 조회합니다.
     * 중복 계정 방지용
     */
    fun findByCi(ci: String): MemberAuthInfo?

    /**
     * CI 존재 여부를 확인합니다.
     */
    fun existsByMemberIdNotAndCi(memberId: Long, ci: String): Boolean

    /**
     * DI(Site-specific ID)로 본인인증 정보를 조회합니다.
     */
    fun findByDi(di: String): MemberAuthInfo?

    /**
     * DI 존재 여부를 확인합니다.
     */
    fun existsByMemberIdNotAndDi(memberId: Long, di: String): Boolean

    /**
     * 휴대폰 번호로 본인인증 정보를 조회합니다.
     */
    fun findByPhoneNumberHash(phoneNumberHash: String): MemberAuthInfo?
}
