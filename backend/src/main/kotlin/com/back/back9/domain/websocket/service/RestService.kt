package com.back.back9.domain.websocket.service

import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.service.RedisService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.min

@Service
class RestService(
    private val redisService: RedisService,
    private val coinListProvider: DatabaseCoinListProvider,
    webClientBuilder: WebClient.Builder,            // ✅ Builder만 주입 (Config 파일 불필요)
    private val mapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val fallbackSet = ConcurrentHashMap.newKeySet<CandleInterval>()
    private var webClient: WebClient = webClientBuilder
        .baseUrl("https://api.upbit.com/v1")
        .build()

    @Scheduled(fixedRate = 600_000)
    fun checkScheduler() {
        log.info("Scheduler 동작중")
    }

    @Scheduled(cron = "0 0/30 * * * *")
    fun fetchEvery30Min() = fetchInterval(CandleInterval.MIN_30, 200)

    @Scheduled(cron = "0 0 2 1 1 *")
    fun fetchYearly() = fetchInterval(CandleInterval.YEAR, 10)

    @Scheduled(fixedRate = 60_000)
    fun fetchFallbackCandles() {
        fallbackSet.forEach { interval ->
            log.info("🔥 Fallback 데이터를 수집합니다: $interval")
            fetchInterval(interval, 1)
        }
    }

    fun fetchInterval(interval: CandleInterval, count: Int): Int {
        val markets: List<String> = coinListProvider.getMarketCodes()
        var totalSaved = 0

        markets.forEach { market ->
            var i = 0
            while (i < count) {
                delay(150)
                val size = min(MAX_PER_REQUEST, count - i)
                val uri = "/candles/${interval.suffix}?market=$market&count=$size"

                runCatching {
                    val json = getWith429Backoff(uri)
                    val array: JsonNode = mapper.readTree(json)

                    val saved = redisService.saveCandleArray(interval, market, array)
                    totalSaved += saved

                    if (interval == CandleInterval.SEC && i == 0 && array.isArray && array.size() > 0) {
                        redisService.saveLatestCandle(market, array.get(0))
                    }
                    i += MAX_PER_REQUEST
                }.onFailure { e ->
                    val message = e.message ?: "unknown"
                    log.error("❌ [${interval}::${market}] 데이터 수집 실패: $message", e)
                    // 알 수 없는 오류면 다음 페이지로 넘어가며 계속 진행
                    i += MAX_PER_REQUEST
                }
            }
        }
        log.info("✅ [interval:${interval.name}] 데이터 $totalSaved 개 저장 완료.")
        return totalSaved
    }

    fun fetchUntil(interval: CandleInterval, requiredSize: Int): Int {
        log.info("✅ [interval:${interval.name}] $requiredSize 개까지 데이터 보충을 시작합니다.")
        var total = 0
        coinListProvider.getMarketCodes().forEach { market ->
            val current = redisService.countCandles(interval, market)
            if (current < requiredSize) {
                val toFetch = requiredSize - current
                log.info("... $market 마켓 데이터 ${toFetch}개 보충 필요")
                total += fetchInterval(interval, toFetch)
            }
        }
        log.info("✅ 데이터 보충 완료. 총 $total 개 저장.")
        return total
    }

    /**
     * 429(Too Many Requests) 발생 시 지수 백오프로 재시도해서 JSON 문자열을 동기 반환
     */
    private fun getWith429Backoff(uri: String): String {
        var attempt = 0
        var delayMs = 1000L
        while (true) {
            val responseMono: Mono<String> = webClient.get()
                .uri(uri)
                .exchangeToMono { res ->
                    when {
                        res.statusCode().is2xxSuccessful -> res.bodyToMono()
                        res.statusCode() == HttpStatus.TOO_MANY_REQUESTS -> {
                            Mono.error(TooManyRequestsException("429 from Upbit for $uri"))
                        }
                        else -> res.bodyToMono<String>()
                            .defaultIfEmpty("")
                            .flatMap { body ->
                                Mono.error(RuntimeException("HTTP ${res.statusCode().value()} for $uri: $body"))
                            }
                    }
                }

            try {
                return responseMono.block()!!
            } catch (e: TooManyRequestsException) {
                attempt++
                log.info(e.message ?: "unknown")
                log.warn("🕒 429 Too Many Requests - 백오프 ${delayMs}ms 후 재시도 (attempt=$attempt)")
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(60_000L)
            }
        }
    }

    private fun delay(ms: Long) = TimeUnit.MILLISECONDS.sleep(ms)

    companion object {
        private const val MAX_PER_REQUEST = 200
        private class TooManyRequestsException(msg: String) : RuntimeException(msg)
    }

    fun setWebClientForTest(client: WebClient) {
        this.webClient = client
    }
}
