package com.back.back9.domain.wallet.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class ChargePointsRequest(
    @field:NotNull(message = "충전할 포인트는 필수입니다")
    @field:Positive(message = "충전할 포인트는 양수여야 합니다")
    val amount: BigDecimal
)
