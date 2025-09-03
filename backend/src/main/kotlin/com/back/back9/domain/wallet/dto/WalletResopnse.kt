package com.back.back9.domain.wallet.dto

import com.back.back9.domain.wallet.entity.CoinAmount
import com.back.back9.domain.wallet.entity.Wallet

data class WalletResponse(
    val walletId: Long?,
    val userId: Long?,
    val address: String,
    val balance: java.math.BigDecimal,
    val coinAmounts: List<CoinAmountResponse>
) {
    companion object {
        fun from(wallet: Wallet): WalletResponse {
            val coinAmountResponses = wallet.coinAmounts.map(CoinAmountResponse::from)
            return WalletResponse(
                walletId = wallet.id,
                userId = wallet.user.id,
                address = wallet.address,
                balance = wallet.balance.toBigDecimal(),
                coinAmounts = coinAmountResponses
            )
        }

        fun fromWithValidCoinAmounts(wallet: Wallet, validCoinAmounts: List<CoinAmount>): WalletResponse {
            val coinAmountResponses = validCoinAmounts.map(CoinAmountResponse::from)
            return WalletResponse(
                walletId = wallet.id,
                userId = wallet.user.id,
                address = wallet.address,
                balance = wallet.balance.toBigDecimal(),
                coinAmounts = coinAmountResponses
            )
        }
    }
}
