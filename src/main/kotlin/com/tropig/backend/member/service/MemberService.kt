package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.enums.OptionType
import com.tropig.backend.common.exception.IllegalArgumentException
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.util.StringUtil
import com.tropig.backend.contents.repository.ContentOptionRepository
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.entity.MemberAuthInfo
import com.tropig.backend.member.entity.WithdrawMember
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.model.request.SignInRequest
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.request.UpdateMemberRequest
import com.tropig.backend.member.model.response.TokenResponse
import com.tropig.backend.member.repository.MemberAuthInfoRepository
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.member.repository.WithdrawMemberRepository
import com.tropig.backend.member.service.jwt.JwtTokenProvider
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
    private val withdrawMemberRepository: WithdrawMemberRepository,
    private val contentOptionRepository: ContentOptionRepository,
    private val contentRepository: ContentRepository,
    private val s3Service: S3Service,
    private val jwtTokenProvider: JwtTokenProvider,
    private val stringUtil: StringUtil,
    private val memberCacheService: MemberCacheService,
    private val redisRefreshTokenService: RedisRefreshTokenService,
) {

    companion object {
        private const val REJOIN_DAYS: Long = 7
    }

    fun saveMember(member: Member) = memberRepository.save(member)

    fun getUserByEmail(email: String): Member? = memberRepository.findByEmail(email)

    fun getUserById(id: Long): Member? = memberRepository.findMemberByIdAndDeletedAtIsNull(id)

    fun getUserByNickname(nickname: String): Member? = memberRepository.findByNicknameAndDeletedAtIsNull(nickname)

    @Cacheable(value = ["memberProfile"], key = "#nickname")
    fun getMemberProfile(nickname: String): Member? = memberRepository.findByNicknameAndDeletedAtIsNull(nickname)

    fun findMemberAuthInfo(memberId: Long): MemberAuthInfo? = memberAuthInfoRepository.findByMemberId(memberId)

    @Transactional
    fun createMember(request: SignUpRequest): Member {
        memberRepository.findByEmail(request.email)?.let {
            it.deletedAt?.let { date ->
                if (date.plusDays(REJOIN_DAYS) >= LocalDateTime.now()) {
                    // 탈퇴일로부터 7일이 지나지 않음
                    throw MemberException(
                        message = "탈퇴 후, ${REJOIN_DAYS}일간 재가입이 불가합니다. 탈퇴일: $date",
                        code = MessageCode.CANNOT_REJOIN_MEMBER,
                    )
                }
            } ?: throw MemberException(
                "해당 이메일을 사용하는 유저가 있습니다. email: ${request.email}",
                MessageCode.ALREADY_EXISTS_MEMBER,
            )
        }

        val nickname = generateSequence { makeNickname() }
            .first { !memberRepository.existsByNicknameAndIdNot(it, 0) }

        return memberRepository.save(request.toEntity(nickname))
    }

    fun signUp(request: SignUpRequest): TokenResponse {
        val member = createMember(request)
        val now = Date()
        val token = jwtTokenProvider.getToken(member, now)

        redisRefreshTokenService.save(member.id, token.second)

        return TokenResponse(
            token.first,
            token.second,
            Instant.ofEpochMilli(token.third)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime(),
        )
    }

    fun signIn(request: SignInRequest): TokenResponse {
        val member = loginMember(request)

        val token = jwtTokenProvider.getToken(member, Date())

        redisRefreshTokenService.save(member.id, token.second)

        return TokenResponse(
            token.first,
            token.second,
            Instant.ofEpochMilli(token.third)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime(),
        )
    }

    fun validateRefreshToken(token: String): Long {
        if (!jwtTokenProvider.validateToken(token)) {
            throw IllegalArgumentException("유효하지 않은 refresh token입니다.")
        }
        val memberId = jwtTokenProvider.getUserIdFromToken(token)

        val storedToken = redisRefreshTokenService.findByMemberId(memberId)
        if (storedToken != null && storedToken != token) {
            throw IllegalArgumentException("폐기된 refresh token입니다.")
        }

        return memberId
    }

    fun refreshToken(member: Member): TokenResponse {
        val now = Date()
        val token = jwtTokenProvider.getToken(member, now)

        redisRefreshTokenService.save(member.id, token.second)

        return TokenResponse(
            token.first,
            token.second,
            Instant.ofEpochMilli(token.third)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime(),
        )
    }

    private fun loginMember(request: SignInRequest): Member {
        val member = memberRepository.findBySnsIdAndSnsProviderAndEmail(
            request.snsId,
            request.snsProvider,
            request.email,
        ) ?: throw NotFoundException(
            message = "가입된 정보가 없는 유저입니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )

        member.deletedAt?.let {
            if (it.plusDays(7) > LocalDateTime.now()) {
                throw MemberException(
                    "탈퇴한 회원입니다.",
                    MessageCode.WITHDRAW_MEMBER,
                )
            }
        }

        return member
    }

    @Transactional
    fun updateUser(member: Member, request: UpdateMemberRequest): Member {
        request.nickname?.let {
            if (it.length > 16) {
                throw IllegalArgumentException(
                    "닉네임은 16자 이하로 입력해주세요.",
                    MessageCode.INVALID_PARAMS,
                )
            }
            if (memberRepository.existsByNicknameAndIdNot(it, member.id)) {
                throw IllegalArgumentException(
                    "이미 존재하는 닉네임입니다.",
                    MessageCode.ALREADY_EXISTS,
                )
            }
        }
        val oldNickname = member.nickname
        member.apply {
            request.nickname?.let { this.nickname = it }
            request.bio?.let { this.bio = it }

            request.profile?.let {
                val profilePath = s3Service.uploadFile(
                    it.inputStream,
                    it.contentType ?: "image/jpeg",
                    it.originalFilename ?: UUID.randomUUID().toString(),
                    member.id,
                    true,
                )

                this.profile = profilePath
            }

            request.isMarketing?.let {
                if (it && (this.marketingAt == null)) {
                    this.marketingAt = LocalDateTime.now()
                } else if (!it && (this.marketingAt != null)) {
                    this.marketingAt = null
                }
            }
        }

        // CREATOR인 경우 닉네임 변경 시 본인 콘텐츠의 searchText에서 이전 닉네임을 새 닉네임으로 치환
        if (request.nickname != null && member.role == Role.CREATOR && oldNickname != request.nickname) {
            val contents = contentRepository.findByMemberId(member.id)
            if (contents.isNotEmpty()) {
                contents.forEach { content ->
                    content.searchText = content.searchText.replace(oldNickname, request.nickname)
                }
                contentRepository.saveAll(contents)
            }
        }

        if (request.favoriteRules.isNotEmpty()) {
            member.favoriteRules = request.favoriteRules.joinToString(",")
        }

        if (request.favoriteGenres.isNotEmpty()) {
            member.favoriteGenres = request.favoriteGenres.joinToString(",")
        }

        return memberRepository.save(member).also {
            memberCacheService.evictMember(member.id)
        }
    }

    @Transactional
    fun deleteUser(id: Long): Member? = memberRepository.findMemberByIdAndDeletedAtIsNull(id)?.let {
        // 90일간 보관용
        withdrawMemberRepository.save(
            WithdrawMember(
                memberId = it.id,
                snsId = it.snsId,
                snsProvider = it.snsProvider,
                email = it.email,
                nickname = it.nickname,
                bio = it.bio,
            ),
        )
        // 7일간 보관용
        it.deletedAt = LocalDateTime.now()
        memberCacheService.evictMember(id)
        redisRefreshTokenService.deleteByMemberId(id)
        it
    }

    /**
     * 저장된 즐겨찾기 문자열을 ID 목록으로 변환합니다.
     * - 신규 형식 (숫자 콤마): "1,5,12" → [1, 5, 12]
     * - 구 형식 (enum 이름 콤마): "ROMANCE,HORROR" → content_option 조회 후 ID로 치환
     */
    fun parseFavoriteIds(raw: String?, type: OptionType): List<Long> {
        if (raw.isNullOrBlank()) return emptyList()
        val tokens = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val ids = tokens.mapNotNull { it.toLongOrNull() }
        if (ids.size == tokens.size) return ids // 이미 모두 숫자

        val nameToId = contentOptionRepository.findAllByType(type).associate { it.name to it.id }
        return tokens.mapNotNull { token ->
            token.toLongOrNull() ?: nameToId[token]
        }
    }

    /**
     * 전체 회원의 favoriteGenres/favoriteRules를 구 enum 이름에서 content_option ID로 일괄 변환합니다.
     * 이미 숫자 형식인 경우 건너뜁니다.
     * @return 실제로 변환된 회원 수
     */
    @Transactional
    fun migrateFavoriteTagsToIds(): Int {
        val genreNameToId = contentOptionRepository.findAllByType(OptionType.GENRE).associate { it.name to it.id }
        val ruleNameToId = contentOptionRepository.findAllByType(OptionType.RULE).associate { it.name to it.id }

        val allMembers = memberRepository.findAll()
        var migratedCount = 0

        allMembers.forEach { member ->
            var changed = false

            val genres = member.favoriteGenres
            if (!genres.isNullOrBlank() && genres.split(",").any { it.trim().toLongOrNull() == null }) {
                member.favoriteGenres = genres.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .mapNotNull { token -> token.toLongOrNull() ?: genreNameToId[token] }
                    .joinToString(",")
                changed = true
            }

            val rules = member.favoriteRules
            if (!rules.isNullOrBlank() && rules.split(",").any { it.trim().toLongOrNull() == null }) {
                member.favoriteRules = rules.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .mapNotNull { token -> token.toLongOrNull() ?: ruleNameToId[token] }
                    .joinToString(",")
                changed = true
            }

            if (changed) {
                memberRepository.save(member)
                migratedCount++
            }
        }

        return migratedCount
    }

    private fun makeNickname(): String {
        val (adjustList, nounList) = stringUtil.getWordLists()
        val adjust = adjustList.random()
        val noun = nounList.random()
        val num = (1..9999).random()
        return adjust + noun + num.toString().padStart(4, '0')
    }
}
