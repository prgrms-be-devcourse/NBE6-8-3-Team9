package com.back.back9.global.redis.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

/**
 * 캔들 데이터 DTO (안정성 및 정확성 확보 최종 버전)
 * - 금융 데이터의 정확성을 위해 Double 대신 BigDecimal을 사용합니다.
 * - @JsonProperty를 통해 JSON 필드와 객체 속성을 명확하게 매핑합니다.
 */
data class Candle(
    @get:JsonProperty("candle_date_time_kst")
    val candleDateTimeKst: String,

    @get:JsonProperty("opening_price")
    val openingPrice: BigDecimal,

    @get:JsonProperty("high_price")
    val highPrice: BigDecimal,

    @get:JsonProperty("low_price")
    val lowPrice: BigDecimal,

    @get:JsonProperty("trade_price")
    val tradePrice: BigDecimal,

    @get:JsonProperty("candle_acc_trade_price")
    val candleAccTradePrice: BigDecimal = BigDecimal.ZERO,

    @get:JsonProperty("candle_acc_trade_volume")
    val candleAccTradeVolume: BigDecimal = BigDecimal.ZERO,

    @get:JsonProperty("timestamp")
    val timestamp: Long
)