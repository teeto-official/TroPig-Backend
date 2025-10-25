package com.tropig.backend.controller

import com.tropig.backend.user.entity.Member
import com.tropig.backend.service.MemberService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/member")
@CrossOrigin(origins = ["*"])
class MemberController(private val memberService: MemberService) {
    
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<Member>> {
        val users = memberService.getAllUsers()
        return ResponseEntity.ok(users)
    }
    
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
    
    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<Any> {
        return try {
            val user = memberService.createUser(request.snsId, request.snsProvider, request.email, request.nickname)
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

data class CreateUserRequest(
    val snsId: String,
    val snsProvider: com.tropig.backend.user.enums.SnsProvider,
    val email: String,
    val nickname: String
)

data class UpdateUserRequest(
    val email: String?,
    val nickname: String?
)
