package com.back.back9.domain.tradeLog.dto

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.tradeLog.entity.TradeLog.Companion.builder
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.wallet.entity.Wallet
import java.math.BigDecimal
import java.time.LocalDateTime

// 내부용 DTO (순수 Kotlin data class)
data class TradeLogDto(
    val id: Long?,
    val walletId: Long?,
    val createdAt: LocalDateTime?,
    val coinId: Long?,
    val coinSymbol: String?,
    val tradeType: TradeType?,
    val quantity: BigDecimal?,
    val price: BigDecimal?
) {
    constructor(tradeLog: TradeLog) : this(
        tradeLog.id,
        tradeLog.wallet?.id,
        tradeLog.createdAt,
        tradeLog.coin?.id,
        tradeLog.coin?.symbol,
        tradeLog.type,
        tradeLog.quantity,
        tradeLog.price?.toBigDecimal()
    )

    companion object {
        fun from(tradeLog: TradeLog): TradeLogDto {
            return TradeLogDto(
                tradeLog.id,
                tradeLog.wallet?.id,
                tradeLog.createdAt,
                tradeLog.coin?.id,
                tradeLog.coin?.symbol,
                tradeLog.type,
                tradeLog.quantity,
                tradeLog.price?.toBigDecimal()
            )
        }

        fun toEntity(dto: TradeLogDto, wallet: Wallet?, coin: Coin?): TradeLog {
            return builder()
                .wallet(wallet)
                .coin(coin)
                .type(dto.tradeType)
                .quantity(dto.quantity)
                .price(Money.of(dto.price))
                .build()
        }
    }
}
