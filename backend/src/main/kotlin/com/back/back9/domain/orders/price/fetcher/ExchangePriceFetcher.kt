package com.back.back9.domain.orders.price.fetcher

import com.back.back9.domain.exchange.service.ExchangeService
import com.back.back9.domain.orders.trigger.entity.TriggerStatus
import com.back.back9.domain.orders.trigger.repository.TriggerRepository
import com.back.back9.domain.orders.trigger.service.TriggerService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ExchangePriceFetcher(
    private val exchangeService: ExchangeService,
    private val triggerService: TriggerService,
    private val triggerRepository: TriggerRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val monitoringSymbols = ConcurrentHashMap.newKeySet<String>()

    fun addMonitoring(symbol: String) {
        monitoringSymbols.add(symbol)
        log.info("▶ 모니터링 등록: $symbol")
    }

    private fun removeMonitoring(symbol: String) {
        monitoringSymbols.remove(symbol)
        log.info("■ 모니터링 해제: $symbol")
    }

    @Scheduled(fixedRate = 10000) // 10초마다 실행
    fun tick() {
        monitoringSymbols.toList().forEach { symbol ->
            val candle = exchangeService.getLatestCandleByScan(symbol)
            triggerService.onPriceTick(symbol, candle.price)
            log.info("Tick processed: $symbol -> ${candle.price}")

            // PENDING 없으면 모니터링 해제
            val stillPending = triggerRepository.existsByCoinSymbolAndStatus(symbol, TriggerStatus.PENDING)
            if (!stillPending) {
                removeMonitoring(symbol)
            }
        }
    }
}