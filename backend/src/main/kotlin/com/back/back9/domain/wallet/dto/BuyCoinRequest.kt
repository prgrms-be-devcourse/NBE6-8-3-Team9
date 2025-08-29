package com.back.back9.domain.wallet.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class BuyCoinRequest(
    @field:NotNull @field:Positive(message = "코인 ID는 양수여야 합니다")
    val coinId: Long,

    @field:NotNull @field:Positive(message = "지갑 ID는 양수여야 합니다")
    val walletId: Long,

    @field:NotNull(message = "거래 금액은 필수입니다")
    @field:Positive(message = "거래 금액은 양수여야 합니다")
    val amount: BigDecimal,

    @field:NotNull(message = "거래 수량은 필수입니다")
    @field:Positive(message = "거래 수량은 양수여야 합니다")
    val quantity: BigDecimal
)
