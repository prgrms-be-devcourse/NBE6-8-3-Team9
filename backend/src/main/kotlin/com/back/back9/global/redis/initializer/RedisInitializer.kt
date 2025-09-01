package com.back.back9.global.redis.initializer

import com.back.back9.domain.websocket.service.DatabaseCoinListProvider
import com.back.back9.global.redis.service.RedisService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class RedisInitializer(
    private val redisService: RedisService,
    private val coinListProvider: DatabaseCoinListProvider,
    private val candleDataInitializerService: CandleDataInitializerService // 서비스 주입
) {

    private val log = LoggerFactory.getLogger(RedisInitializer::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun afterStartup() {
        log.info("✅ Spring Boot 부팅 완료, Redis 데이터 초기화를 시작합니다.")
        redisService.clearAll()

        val marketCodes = coinListProvider.getMarketCodes()
        if (marketCodes.isEmpty()) {
            log.warn("⚠️ 초기화할 코인 목록이 비어있어 Redis 데이터 보충 작업을 건너뜁니다.")
            return
        }

        // 새로 만든 서비스를 호출하여 전체 코인 초기화
        candleDataInitializerService.initializeCandlesForMarkets(marketCodes)
    }
}
