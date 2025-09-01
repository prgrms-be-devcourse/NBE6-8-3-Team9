package com.back.back9.domain.exchange.dto

import com.back.back9.domain.websocket.vo.CandleInterval

/**
 * 최초 캔들 데이터 요청 DTO.
 * data class와 val을 사용하여 불변 객체로 만듭니다.
 */
data class CandleInitialRequestDTO(
    val interval: CandleInterval,
    val market: String
)