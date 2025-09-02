package com.back.back9.global.redis.initializer

import com.back.back9.domain.coin.service.CoinDataChangedEvent
import com.back.back9.domain.exchange.vo.CandleInitializationTargets.intervalTargetCount
import com.back.back9.domain.websocket.service.DatabaseCoinListProvider
import com.back.back9.domain.websocket.service.RestService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.client.HttpClientErrorException
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@Service
class CandleDataInitializerService(
    private val restService: RestService,
    // 최신 코인 목록을 얻기 위해 DatabaseCoinListProvider를 주입받습니다.
    private val coinListProvider: DatabaseCoinListProvider
) {
    private val log = LoggerFactory.getLogger(CandleDataInitializerService::class.java)
    // 이미 데이터 초기화가 완료된 코인 목록을 저장하여 중복 작업을 방지합니다.
    private val processedSymbols = ConcurrentHashMap.newKeySet<String>()

    /**
     * 애플리케이션 시작 시 DB에 있는 모든 코인에 대한 초기 데이터 적재를 수행합니다.
     */
    @PostConstruct
    fun initializeAllCandlesOnStartup() {
        log.info("애플리케이션 시작. 전체 코인에 대한 캔들 데이터 초기화를 시작합니다.")
        // Provider를 통해 현재 DB에 저장된 모든 코인 목록을 가져옵니다.
        val allMarketCodes = coinListProvider.getMarketCodes()

        if (allMarketCodes.isNotEmpty()) {
            initializeCandlesForMarkets(allMarketCodes)
        } else {
            log.info("DB에 등록된 코인이 없어 캔들 데이터 초기화를 건너뜁니다.")
        }
    }

    /**
     * Coin 데이터 변경 이벤트를 수신하여, 새로 추가된 코인에 대해서만 데이터 초기화를 진행합니다.
     */
    @Suppress("UNUSED_PARAMETER")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinDataChanged(event: CoinDataChangedEvent) {
        val currentSymbols = coinListProvider.getMarketCodes().toSet()
        val newSymbols = currentSymbols - processedSymbols

        if (newSymbols.isNotEmpty()) {
            log.info("신규 코인 감지: $newSymbols. 데이터 초기화를 시작합니다.")
            initializeCandlesForMarkets(newSymbols.toList())
        }
    }
    /**
     * 주어진 코인(마켓) 목록에 대해 외부 API를 호출하여 캔들 데이터를 가져오고 Redis에 저장합니다.
     * API 요청은 시간이 오래 걸릴 수 있으므로, 별도의 백그라운드 스레드에서 비동기로 실행됩니다.
     */
    fun initializeCandlesForMarkets(marketCodes: List<String>) {
        if (marketCodes.isEmpty()) {
            return
        }

        // 작업을 시작하기 전, 처리 목록에 추가하여 중복 실행을 방지합니다.
        processedSymbols.addAll(marketCodes)

        log.info("🕒 백그라운드에서 코인 {}에 대한 캔들 초기화를 시작합니다.", marketCodes)

        // 비동기 작업을 위한 새 스레드 생성
        thread(start = true, isDaemon = true, name = "candle-initializer-${marketCodes.joinToString("-")}") {
            // 정의된 각 시간 간격(1초, 1분 등)과 목표 개수만큼 데이터를 가져옵니다.
            intervalTargetCount.forEach { (interval, target) ->
                try {
                    // RestService를 통해 실제 데이터 fetching 작업을 수행합니다.
                    val inserted = restService.fetchUntilForMarkets(interval, target, marketCodes)
                    if (inserted > 0) {
                        log.info("✅ [${interval.name}] 캔들 ${inserted}개 등록 완료 (대상: ${marketCodes})")
                    }
                } catch (e: HttpClientErrorException.TooManyRequests) {
                    log.warn("⏸️ [${interval.name}] API 요청 제한(429) 발생: 3분간 대기 후 계속합니다.", e)
                    sleep()
                } catch (e: Exception) {
                    log.error("❌ [${interval.name}] 캔들 수집 중 심각한 오류 발생 (대상: ${marketCodes})", e)
                }
            }
            log.info("🎉 코인 {}에 대한 전체 캔들 초기화 완료", marketCodes)
        }
    }

    /**
     * API 요청 제한(429 Error) 시 일정 시간 대기하는 유틸리티 메서드입니다.
     */
    private fun sleep() {
        try {
            Thread.sleep(3 * 60 * 1000L) // 3분
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("캔들 초기화 대기 스레드가 중단되었습니다.", e)
        }
    }
}
