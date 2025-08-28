package com.back.back9.domain.coin.dto

import com.back.back9.domain.coin.entity.Coin
import java.time.LocalDateTime

data class CoinDto(
    val id: Long?,
    val symbol: String,
    val koreanName: String?,
    val englishName: String?,
    val createdAt: LocalDateTime?,
    val modifiedAt: LocalDateTime?
) {
    constructor(coin: Coin) : this(
        coin.id,
        coin.symbol,
        coin.koreanName,
        coin.englishName,
        coin.createdAt,
        coin.modifiedAt
    )
}
