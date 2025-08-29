package com.back.back9.domain.websocket.vo

enum class CandleInterval(
    val suffix: String,
    val maxSize: Int
) {
    SEC("seconds", 1000),
    MIN_1("minutes/1", 1000),
    MIN_30("minutes/30", 1000),
    HOUR_1("minutes/60", 1000),
    DAY("days", 500),
    WEEK("weeks", 150),
    MONTH("months", 50),
    YEAR("years", 10);

    /**
     * Redis에서 사용할 최종 키를 생성합니다. (예: "KRW-BTC:minutes/1")
     */
    fun redisKey(symbol: String): String = "$symbol:$suffix"

    /**
     * 자바의 static 메서드와 동일한 역할을 하는 companion object 입니다.
     */
    companion object {
        fun fromWebSocketType(websocketType: String): CandleInterval = when (websocketType) {
            "candle.1s" -> SEC
            "candle.1m" -> MIN_1
            "candle.30m" -> MIN_30
            "candle.1h" -> HOUR_1
            "candle.1d" -> DAY
            "candle.1w" -> WEEK
            "candle.1M" -> MONTH
            "candle.1y" -> YEAR
            else -> throw IllegalArgumentException("Unknown WebSocket candle type: $websocketType")
        }
    }
}