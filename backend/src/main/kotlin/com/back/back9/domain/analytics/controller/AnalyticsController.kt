package com.back.back9.domain.analytics.controller

import com.back.back9.domain.analytics.dto.ProfitRateResponse
import com.back.back9.domain.analytics.service.AnalyticsService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
@RestController
@RequestMapping("/api/analytics")
@Validated
class AnalyticsController(private val analyticsService: AnalyticsService) {
    private val log: Logger = LoggerFactory.getLogger(AnalyticsController::class.java)
    @GetMapping("/wallet/{walletId}/realized")
    fun calculateRealizedProfitRates(@PathVariable walletId: Long): ResponseEntity<ProfitRateResponse?> {
        val response = analyticsService.calculateRealizedProfitRates(walletId)
        return ResponseEntity.ok<ProfitRateResponse?>(response)
    }

    @GetMapping("/wallet/{walletId}/unrealized")
    fun calculateUnRealizedProfitRates(@PathVariable walletId: Long): ResponseEntity<ProfitRateResponse?> {
        log.debug("실현 손익률 계산 요청 - 지갑 ID: {}", walletId)
        val response = analyticsService.calculateUnRealizedProfitRates(walletId)
        return ResponseEntity.ok<ProfitRateResponse?>(response)
    }
}
