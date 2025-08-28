package com.back.back9.domain.coin.dto

import jakarta.validation.constraints.NotBlank

data class CoinAddRequest(
        @field:NotBlank(message = "symbol 입력은 필수입니다.")
        val symbol: String,
        val koreanName: String,
        val englishName: String
)
