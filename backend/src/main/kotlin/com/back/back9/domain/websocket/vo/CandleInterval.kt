package com.back.back9.domain.websocket.vo

/**
 * Upbit API의 캔들 종류와 Redis 키 생성을 관리하는 Enum 클래스
 */
enum class CandleInterval(val suffix: String, val webSocketType: String) {
    SEC("minutes/1", "candle.1s"),
    MIN_1("minutes/1", "candle.1m"),
    MIN_3("minutes/3", ""),
    MIN_5("minutes/5", ""),
    MIN_10("minutes/10", ""),
    MIN_15("minutes/15", ""),
    MIN_30("minutes/30", ""),
    HOUR_1("minutes/60", ""),
    HOUR_4("minutes/240", ""),
    DAY("days", ""),
    WEEK("weeks", ""),
    MONTH("months", "");

    /**
     * Redis 저장을 위한 고유 키를 생성합니다. (예: "CANDLE:MIN_1:KRW-BTC")
     */
    fun redisKey(marketCode: String): String {
        return "CANDLE:${this.name}:$marketCode"
    }

    companion object {
        fun fromWebSocketType(type: String): CandleInterval {
            return entries.find { it.webSocketType == type }
                ?: throw IllegalArgumentException("Unsupported WebSocket candle type: $type")
        }
    }
}