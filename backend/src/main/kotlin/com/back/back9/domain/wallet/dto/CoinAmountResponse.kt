package com.back.back9.domain.wallet.dto

import com.back.back9.domain.wallet.entity.CoinAmount
import java.math.BigDecimal

data class CoinAmountResponse(
    val coinId: Long?,
    val coinSymbol: String,
    val coinName: String,
    val quantity: BigDecimal,
    val totalAmount: BigDecimal
) {
    companion object {
        fun from(coinAmount: CoinAmount): CoinAmountResponse {
            val coin = coinAmount.coin
            return CoinAmountResponse(
                coin.id,
                coin.symbol,
                coin.koreanName ?: coin.symbol,
                coinAmount.quantity,
                coinAmount.totalAmount.toBigDecimal()
            )
        }
    }
}
