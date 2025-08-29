package com.back.back9.domain.wallet.dto

import com.back.back9.domain.wallet.entity.Wallet

data class WalletBalanceResponse(
    val walletId: Long,
    val userId: Long,
    val address: String,
    val balance: java.math.BigDecimal
) {
    companion object {
        fun of(walletId: Long, userId: Long, address: String, balance: java.math.BigDecimal) =
            WalletBalanceResponse(walletId, userId, address, balance)

        fun from(wallet: Wallet) = WalletBalanceResponse(
            walletId = wallet.id,
            userId = wallet.user.id,
            address = wallet.address,
            balance = wallet.balance.toBigDecimal()
        )
    }
}
