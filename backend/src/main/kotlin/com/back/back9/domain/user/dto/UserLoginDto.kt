package com.back.back9.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserLoginReqBody(
    @field:NotBlank @field:Size(min = 2, max = 30)
    val userLoginId: String,
    @field:NotBlank @field:Size(min = 2, max = 30)
    val password: String
)

data class UserLoginResBody(
    val item: UserDto,
    val apiKey: String,
    val accessToken: String
)