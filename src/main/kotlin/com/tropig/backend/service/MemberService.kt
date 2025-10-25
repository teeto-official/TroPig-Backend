package com.tropig.backend.service

import com.tropig.backend.user.entity.Member
import com.tropig.backend.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class MemberService(private val memberRepository: MemberRepository) {
    
    fun getAllUsers(): List<Member> {
        return memberRepository.findAll()
    }
    
    fun getUserById(id: Long): Member? {
        return memberRepository.findById(id).orElse(null)
    }
    
    fun getUserByEmail(email: String): Member? {
        return memberRepository.findByEmail(email)
    }
    
    fun createUser(snsId: String, snsProvider: com.tropig.backend.user.enums.SnsProvider, email: String, nickname: String): Member {
        if (memberRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }
        
        val member = Member(
            snsId = snsId,
            snsProvider = snsProvider,
            email = email,
            nickname = nickname,
            role = com.tropig.backend.user.enums.Role.USER
        )
        
        return memberRepository.save(member)
    }
    
    fun updateUser(id: Long, email: String?, nickname: String?): Member? {
        val user = memberRepository.findById(id).orElse(null) ?: return null
        
        val updatedUser = user.copy(
            email = email ?: user.email,
            nickname = nickname ?: user.nickname,
        )
        
        return memberRepository.save(updatedUser)
    }
    
    fun deleteUser(id: Long): Boolean {
        return if (memberRepository.existsById(id)) {
            memberRepository.deleteById(id)
            true
        } else {
            false
        }
    }
}
