package com.back.back9.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserRegisterDto(
    @field:NotBlank @field:Size(min = 2, max = 30)
    val userLoginId: String,
    @field:NotBlank @field:Size(min = 2, max = 30)
    val username: String,
    @field:NotBlank @field:Size(min = 2, max = 30)
    val password: String,
    @field:NotBlank @field:Size(min = 2, max = 30)
    val confirmPassword: String
) {
    init {
        require(password == confirmPassword) { "패스워드가 일치하지 않습니다" }
    }
}