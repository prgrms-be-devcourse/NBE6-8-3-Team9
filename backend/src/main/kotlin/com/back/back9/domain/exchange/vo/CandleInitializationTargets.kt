package com.back.back9.domain.exchange.vo

import com.back.back9.domain.websocket.vo.CandleInterval

/**
 * Redis 데이터 초기화 시 각 캔들 주기별 목표 데이터 개수를 정의하는 객체
 */
object CandleInitializationTargets {
    val intervalTargetCount: Map<CandleInterval, Int> = mapOf(
        CandleInterval.SEC to 1000,
        CandleInterval.MIN_1 to 1000,
        CandleInterval.MIN_30 to 1000,
        CandleInterval.HOUR_1 to 1000,
        CandleInterval.DAY to 500,
        CandleInterval.WEEK to 400,
        CandleInterval.MONTH to 200
    )
}