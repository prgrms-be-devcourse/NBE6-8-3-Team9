package com.back.back9.global.redis.initializer

import com.back.back9.domain.exchange.vo.CandleInitializationTargets.intervalTargetCount
import com.back.back9.domain.websocket.service.RestService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import kotlin.concurrent.thread

@Service
class CandleDataInitializerService(
    private val restService: RestService
) {
    private val log = LoggerFactory.getLogger(CandleDataInitializerService::class.java)

    /**
     * 지정된 마켓 코드 목록에 대해 캔들 데이터 초기화를 수행합니다.
     * API 요청이 오래 걸릴 수 있으므로, 백그라운드 스레드에서 비동기로 실행됩니다.
     */
    fun initializeCandlesForMarkets(marketCodes: List<String>) {
        if (marketCodes.isEmpty()) {
            log.info("초기화할 신규 코인 목록이 없어 작업을 건너뜁니다.")
            return
        }

        log.info("🕒 백그라운드에서 신규 코인 {}에 대한 캔들 초기화를 시작합니다.", marketCodes)

        thread(start = true, isDaemon = true, name = "candle-initializer-${marketCodes.joinToString("-")}") {
            intervalTargetCount.forEach { (interval, target) ->
                try {
                    // --- [수정] ---
                    // 잘못 호출되었던 fetchUntil을 fetchUntilForMarkets로 변경합니다.
                    val inserted = restService.fetchUntilForMarkets(interval, target, marketCodes)

                    if (inserted > 0) {
                        log.info("✅ [${interval.name}] 캔들 ${inserted}개 등록 완료 (대상: ${marketCodes})")
                    }
                } catch (e: HttpClientErrorException.TooManyRequests) {
                    log.warn("⏸️ [${interval.name}] 429 Too Many Requests 발생: 3분간 대기 후 계속합니다.", e)
                    sleep()
                } catch (e: Exception) {
                    log.error("❌ [${interval.name}] 캔들 수집 중 심각한 오류 발생 (대상: ${marketCodes})", e)
                }
            }
            log.info("🎉 신규 코인 {}에 대한 전체 캔들 초기화 완료", marketCodes)
        }
    }

    private fun sleep() {
        try {
            Thread.sleep(3 * 60 * 1000L) // 3 minutes
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("캔들 초기화 대기 스레드가 중단되었습니다.", e)
        }
    }
}
