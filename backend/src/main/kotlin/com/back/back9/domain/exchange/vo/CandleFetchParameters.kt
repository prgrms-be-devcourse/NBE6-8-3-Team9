package com.back.back9.domain.exchange.vo

/**
 * 캔들 데이터 조회 시 사용되는 파라미터(개수)를 정의하는 객체
 */
object CandleFetchParameters {
    // 최초 차트 로딩 시 가져올 캔들 개수
    const val INITIAL_CANDLE_COUNT = 120

    // 무한 스크롤 시 추가로 가져올 캔들 개수
    const val PREVIOUS_CANDLE_COUNT = 50

    // 외부 API(Upbit)에 한 번에 요청할 수 있는 최대 캔들 개수
    const val MAX_API_REQUEST_COUNT = 200
}