package com.back.back9.domain.user.repository

import com.back.back9.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByApiKey(apiKey: String): User?
    fun findByUserLoginId(userLoginId: String): User?
    fun findByUsername(username: String): User?
    fun findByUsernameContaining(keyword: String): List<User>
}