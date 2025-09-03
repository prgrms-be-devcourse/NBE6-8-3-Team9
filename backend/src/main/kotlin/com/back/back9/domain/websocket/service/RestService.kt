package com.back.back9.domain.websocket.service

import com.back.back9.domain.exchange.vo.CandleFetchParameters
import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.dto.Candle
import com.back.back9.global.redis.service.RedisService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import kotlin.math.min

@Service
class RestService(
    private val redisService: RedisService,
    webClientBuilder: WebClient.Builder,
    private val mapper: ObjectMapper,
    private val provider: DatabaseCoinListProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var webClient: WebClient = webClientBuilder
        .baseUrl("https://api.upbit.com/v1")
        .build()

    /**
     * 테스트 코드와 호환성을 위해 추가된 메서드
     * 현재 DB/Provider에 등록된 코인 목록을 대상으로 fetchUntilForMarkets 실행
     */
    fun fetchInterval(interval: CandleInterval, count: Int): Int {
        val markets = provider.getMarketCodes()
        return fetchUntilForMarkets(interval, count, markets)
    }

    fun fetchUntilForMarkets(interval: CandleInterval, requiredSize: Int, markets: List<String>): Int {
        var totalSaved = 0
        markets.forEach { market ->
            val currentCount = redisService.countCandles(interval.redisKey(market))
            if (currentCount < requiredSize) {
                val toFetch = (requiredSize - currentCount).toInt()
                totalSaved += fetchIntervalForMarket(interval, toFetch, market)
            }
        }
        return totalSaved
    }

    private fun fetchIntervalForMarket(interval: CandleInterval, count: Int, market: String): Int {
        var savedCount = 0
        var fetchedCount = 0
        while (fetchedCount < count) {
            val requestSize = min(CandleFetchParameters.MAX_API_REQUEST_COUNT, count - fetchedCount)
            if (requestSize <= 0) break

            val uri = "/candles/${interval.suffix}?market=$market&count=$requestSize"

            runCatching {
                val json = getWith429Backoff(uri)
                val array: JsonNode = mapper.readTree(json)
                savedCount += saveCandleArray(interval, market, array)
            }.onFailure { e ->
                log.error("[${interval.name}::$market] 데이터 수집 실패: ${e.message}", e)
            }
            fetchedCount += requestSize
            Thread.sleep(110)
        }
        return savedCount
    }

    private fun getWith429Backoff(uri: String): String {
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono<String>()
            .block() ?: ""
    }

    private fun saveCandleArray(interval: CandleInterval, market: String, array: JsonNode): Int {
        var saved = 0
        if (array.isArray) {
            array.forEach { node ->
                runCatching {
                    val candle = mapper.treeToValue(node, Candle::class.java)
                    redisService.saveCandle(interval.redisKey(market), candle)
                    saved++
                }.onFailure { e ->
                    log.error("개별 캔들 노드 파싱 실패: $node", e)
                }
            }
        }
        return saved
    }

    fun setWebClientForTest(client: WebClient) {
        this.webClient = client
    }
}
