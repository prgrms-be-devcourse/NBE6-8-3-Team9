package com.back.back9.domain.user.dto

import com.back.back9.domain.user.entity.User
import java.time.LocalDateTime

data class UserDto(
        val id: Long?,
        val userLoginId: String,
        val username: String,
        val role: String,
        val createdAt: LocalDateTime?,
        val modifiedAt: LocalDateTime?
) {
    companion object {
        fun from(user: User): UserDto {
            return UserDto(
                id = user.id,
                userLoginId = user.userLoginId,
                username = user.username,
                role = user.role.name,
                createdAt = user.createdAt,
                modifiedAt = user.modifiedAt
            )
        }
    }
}