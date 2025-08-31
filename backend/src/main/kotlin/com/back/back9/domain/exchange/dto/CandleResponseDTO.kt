package com.back.back9.domain.exchange.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API 응답으로 사용될 캔들 데이터 DTO.
 * data class를 사용하여 equals, hashCode, toString, copy 등의 메서드를 자동으로 생성합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CandleResponseDTO(
    // JSON 필드와 생성자 파라미터를 매핑합니다.
    @param:JsonProperty("timestamp") val timestamp: Long,
    @param:JsonProperty("candle_date_time_kst") val time: LocalDateTime,
    @param:JsonProperty("market") val symbol: String,
    @param:JsonProperty("opening_price") val open: BigDecimal,
    @param:JsonProperty("high_price") val high: BigDecimal,
    @param:JsonProperty("low_price") val low: BigDecimal,
    @param:JsonProperty("trade_price") val close: BigDecimal,
    @param:JsonProperty("candle_acc_trade_volume") val volume: BigDecimal
) {
    /**
     * 코인 이름은 JSON 역직렬화 이후, 서비스 로직에서 추가로 할당되므로
     * 주 생성자 외부에서 var(변경 가능) 프로퍼티로 선언합니다.
     */
    var name: String? = null

    /**
     * data class가 자동으로 생성하는 toString()은 주 생성자의 프로퍼티만 포함합니다.
     * 기존 코드와 동일하게 'name' 필드까지 문자열에 포함시키기 위해 toString() 메서드를 직접 오버라이드합니다.
     */
    override fun toString(): String {
        return "CandleResponseDTO(" +
                "timestamp=$timestamp, " +
                "time=$time, " +
                "symbol=$symbol, " +
                "open=$open, " +
                "high=$high, " +
                "low=$low, " +
                "close=$close, " +
                "volume=$volume, " +
                "name=$name" +
                ")"
    }
}