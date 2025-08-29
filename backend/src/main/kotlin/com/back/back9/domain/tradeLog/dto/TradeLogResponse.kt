package com.back.back9.domain.tradeLog.dto

import java.time.format.DateTimeFormatter

// 프론트 응답 DTO
data class TradeLogResponse(
    val date: String?,
    val coinSymbol: String?,
    val tradeType: String?,
    val price: String?,
    val quantity: String?
) {
    constructor(tradeLogDto: TradeLogDto) : this(
        tradeLogDto.createdAt?.toLocalDate()
            ?.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
        tradeLogDto.coinSymbol,
        tradeLogDto.tradeType?.toString(),
        tradeLogDto.price?.toPlainString(),
        tradeLogDto.quantity?.toPlainString()
    )
}