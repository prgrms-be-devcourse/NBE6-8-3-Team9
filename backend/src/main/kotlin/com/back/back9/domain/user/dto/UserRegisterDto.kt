package com.back.back9.domain.user.dto

import jakarta.validation.constraints.NotBlank

data class UserRegisterDto(
    @field:NotBlank
    val userLoginId: String,
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
    @field:NotBlank
    val confirmPassword: String
) {
    init {
        require(password == confirmPassword) { "패스워드가 일치하지 않습니다" }
    }
}