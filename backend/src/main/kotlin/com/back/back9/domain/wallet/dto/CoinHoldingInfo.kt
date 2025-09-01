package com.back.back9.domain.wallet.dto

import com.back.back9.domain.wallet.entity.CoinAmount
import java.math.BigDecimal

data class CoinHoldingInfo(
    val coinId: Long?,
    val coinSymbol: String,
    val coinName: String,
    val quantity: BigDecimal,
    val totalInvestAmount: BigDecimal,
    val averageBuyPrice: BigDecimal
) {
    companion object {
        fun from(coinAmount: CoinAmount): CoinHoldingInfo =
            CoinHoldingInfo(
                coinAmount.coin.id,
                coinAmount.coin.symbol,
                coinAmount.coin.koreanName ?: coinAmount.coin.symbol,
                coinAmount.quantity,
                coinAmount.totalAmount.toBigDecimal(),
                coinAmount.getAverageBuyPrice().toBigDecimal()
            )
    }
}
