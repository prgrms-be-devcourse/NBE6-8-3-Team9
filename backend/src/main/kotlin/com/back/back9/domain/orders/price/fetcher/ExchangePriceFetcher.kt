package com.back.back9.domain.orders.price.fetcher

import com.back.back9.domain.exchange.service.ExchangeService
import com.back.back9.domain.orders.trigger.entity.TriggerStatus
import com.back.back9.domain.orders.trigger.repository.TriggerRepository
import com.back.back9.domain.orders.trigger.service.TriggerService
import com.back.back9.domain.orders.trigger.support.RedisKeys
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ExchangePriceFetcher(
    private val triggerService: TriggerService,
    private val triggerRepository: TriggerRepository,
    private val redis: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val monitoringSymbols = ConcurrentHashMap.newKeySet<String>()

    fun addMonitoring(symbol: String) {
        monitoringSymbols.add(symbol)
        log.info("▶ 모니터링 등록: $symbol")
    }

    fun removeMonitoring(symbol: String) {
        monitoringSymbols.remove(symbol)
        log.info("■ 모니터링 해제: $symbol")
    }

    @Scheduled(fixedRate = 1_000) // 1초마다 실행
    fun tick() {
        monitoringSymbols.toList().forEach { symbol ->
            val latestKey = RedisKeys.latestPrice(symbol)
            val latestStr = redis.opsForValue().get(latestKey)
            val latestPrice = latestStr?.toBigDecimalOrNull()

            if (latestPrice != null) {
                triggerService.onPriceTick(symbol, latestPrice)
//                log.info("Tick processed (from Redis): $symbol -> $latestPrice")
            } else {
                log.warn("⚠️ 최신 시세 없음: $symbol (Redis에서 값 없음)")
            }

            // PENDING 없으면 모니터링 해제
            val stillPending = triggerRepository.existsByCoinSymbolAndStatus(symbol, TriggerStatus.PENDING)
            if (!stillPending) {
                removeMonitoring(symbol)
            }
        }
    }
}