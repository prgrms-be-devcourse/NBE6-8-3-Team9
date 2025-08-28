package com.back.back9.domain.user.dto

import com.back.back9.domain.user.entity.User
import java.time.LocalDateTime

data class UserWithUsernameDto(
    val id: Long?,
    val username: String,
    val userLoginId: String,
    val role: String,
    val createdAt: LocalDateTime?,
    val modifiedAt: LocalDateTime?
) {
    constructor(user: User) : this(
        id = user.id,
        username = user.username,
        userLoginId = user.userLoginId,
        role = user.role.name,
        createdAt = user.createdAt,
        modifiedAt = user.modifiedAt
    )
}