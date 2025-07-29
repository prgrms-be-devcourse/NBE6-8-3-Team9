package com.back.back9.domain.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BuyCoinRequest(
        @NotNull
        @Positive(message = "코인 ID는 양수여야 합니다")
        Long coinId,
        @NotNull
        @Positive(message = "지갑 ID는 양수여야 합니다")
        Long walletId,
        @NotNull(message = "거래 금액은 필수입니다")
        @Positive(message = "거래 금액은 양수여야 합니다")
        BigDecimal amount,
        @NotNull(message = "거래 수량은 필수입니다")
        @Positive(message = "거래 수량은 양수여야 합니다")
        BigDecimal quantity
) {
}
