package com.back.back9.global.redis.initializer

import com.back.back9.domain.exchange.vo.CandleInitializationTargets.intervalTargetCount
import com.back.back9.domain.websocket.service.DatabaseCoinListProvider
import com.back.back9.domain.websocket.service.RestService
import com.back.back9.global.redis.service.RedisService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import kotlin.concurrent.thread

@Component
class RedisInitializer(
    private val redisService: RedisService,
    private val restService: RestService,
    private val coinListProvider: DatabaseCoinListProvider
) {

    private val log = LoggerFactory.getLogger(RedisInitializer::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun afterStartup() {
        log.info("✅ Spring Boot 부팅 완료, Redis 데이터 초기화를 시작합니다.")
        log.info("... (WebSocket 연결은 UpbitWebSocketClient가 자동으로 시작합니다)")

        redisService.clearAll()

        val marketCodes = coinListProvider.getMarketCodes()
        if (marketCodes.isEmpty()) {
            log.warn("⚠️ 초기화할 코인 목록이 비어있어 Redis 데이터 보충 작업을 건너뜁니다.")
            return
        }

        thread(start = true, isDaemon = true) {
            initializeCandles()
        }
    }

    private fun initializeCandles() {
        log.info("🕒 백그라운드에서 캔들 초기화 시작")

        intervalTargetCount.forEach { (interval, target) ->
            try {
                val inserted = restService.fetchUntil(interval, target)
                log.info("✅ {} 캔들 {}개 등록 완료 (목표: {})", interval.name, inserted, target)
            } catch (_: HttpClientErrorException.TooManyRequests) {
                log.warn("⏸️ 429 Too Many Requests 발생: 3분간 전체 수집 중단 후 재시도")
                sleep()
            } catch (_: Exception) { // 사용하지 않는 변수는 '_'로
                log.error("❌ {} 캔들 수집 중 심각한 오류 발생", interval.name)
            }
        }

        log.info("🎉 전체 캔들 초기화 완료")
    }

    private fun sleep() {
        try {
            Thread.sleep(3 * 60 * 1000L) // 3분
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
