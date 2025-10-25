package com.tropig.backend.member.controller

import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.service.MemberService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/member")
@CrossOrigin(origins = ["*"])
class MemberController(
    private val memberService: MemberService
) {

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<Member> {
        val user = memberService.getUserById(id)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/email/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<Member> {
        val user = memberService.getUserByEmail(email)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/sign-up")
    fun createUser(
        @RequestBody request: SignUpRequest
    ): ResponseEntity<Any> {
        return try {
            val user = memberService.signUp(request)
            ResponseEntity.status(HttpStatus.CREATED).body(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
    
    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<Any> {
        val user = memberService.updateUser(id, request.email, request.nickname)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        val deleted = memberService.deleteUser(id)
        return if (deleted) {
            ResponseEntity.ok(mapOf("message" to "User deleted successfully"))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

data class UpdateUserRequest(
    val email: String?,
    val nickname: String?
)
