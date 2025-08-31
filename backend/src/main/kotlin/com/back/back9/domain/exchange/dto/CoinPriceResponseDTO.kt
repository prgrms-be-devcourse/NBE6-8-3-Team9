package com.back.back9.domain.exchange.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 실시간 코인 가격 응답 DTO.
 * data class와 val을 사용하여 불변 객체로 만듭니다.
 */
data class CoinPriceResponseDTO(
    val symbol: String?,

    @param:JsonProperty("trade_price")
    val price: BigDecimal?,

    val time: LocalDateTime?
)