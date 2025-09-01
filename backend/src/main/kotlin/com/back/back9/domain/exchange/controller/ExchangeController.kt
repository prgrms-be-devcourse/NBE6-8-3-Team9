package com.back.back9.domain.exchange.controller

import com.back.back9.domain.exchange.dto.CandleInitialRequestDTO
import com.back.back9.domain.exchange.dto.CandlePreviousRequestDTO
import com.back.back9.domain.exchange.dto.CandleResponseDTO
import com.back.back9.domain.exchange.service.ExchangeService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 거래소 관련 캔들 데이터를 제공하는 REST 컨트롤러
 *
 * - 초기 캔들 조회
 * - 과거 캔들 조회 (커서 기반)
 * - 최신 코인별 캔들 조회
 */
@RestController
@RequestMapping("/api/exchange")
class ExchangeController(
    private val exchangeService: ExchangeService
) {

    /**
     * 초기 구간의 캔들을 조회합니다.
     * @param request 조회할 구간(interval)과 마켓(market)을 포함한 요청 DTO
     * @return 해당 구간의 초기 캔들 리스트
     */
    @PostMapping("/initial")
    fun getInitialCandles(@RequestBody request: CandleInitialRequestDTO): List<CandleResponseDTO> {
        return exchangeService.getInitialCandles(request.interval, request.market)
    }

    /**
     * 특정 시점(cursorTimestamp) 이전의 캔들을 조회합니다.
     * @param request 조회할 구간, 마켓, 기준 타임스탬프를 포함한 요청 DTO
     * @return 요청된 기준 이전의 캔들 리스트
     */
    @PostMapping("/previous")
    fun getPreviousCandles(@RequestBody request: CandlePreviousRequestDTO): List<CandleResponseDTO> {
        return exchangeService.getPreviousCandles(
            request.interval,
            request.market,
            request.cursorTimestamp
        )
    }

    /**
     * 모든 코인의 최신 캔들을 조회합니다.
     * @return 코인별 최신 캔들 리스트
     */
    @GetMapping("/coins-latest")
    fun getCoinList(): List<CandleResponseDTO> {
        return exchangeService.coinsLatest
    }
}
