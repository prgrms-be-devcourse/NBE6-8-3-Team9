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
    private val coinListProvider: DatabaseCoinListProvider,
    webClientBuilder: WebClient.Builder,
    private val mapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var webClient: WebClient = webClientBuilder
        .baseUrl("https://api.upbit.com/v1")
        .build()

    fun fetchInterval(interval: CandleInterval, count: Int): Int {
        val markets: List<String> = coinListProvider.getMarketCodes()
        var totalSaved = 0

        markets.forEach { market ->
            var i = 0
            while (i < count) {
                val size = min(CandleFetchParameters.MAX_API_REQUEST_COUNT, count - i)
                val uri = "/candles/${interval.suffix}?market=$market&count=$size"

                runCatching {
                    val json = getWith429Backoff(uri)
                    val array: JsonNode = mapper.readTree(json)

                    val saved = saveCandleArray(interval, market, array)
                    totalSaved += saved

                    if (interval == CandleInterval.SEC && i == 0 && array.isArray && array.size() > 0) {
                        saveLatestCandle(market, array.get(0))
                    }
                    i += CandleFetchParameters.MAX_API_REQUEST_COUNT
                }.onFailure { e ->
                    val message = e.message ?: "unknown"
                    log.error("❌ [${interval}::${market}] 데이터 수집 실패: $message", e)
                    i += CandleFetchParameters.MAX_API_REQUEST_COUNT
                }
            }
        }
        log.info("✅ [interval:${interval.name}] 데이터 $totalSaved 개 저장 완료.")
        return totalSaved
    }

    fun fetchUntil(interval: CandleInterval, requiredSize: Int): Int {
        var total = 0
        coinListProvider.getMarketCodes().forEach { market ->
            val current = redisService.countCandles(interval.redisKey(market))
            if (current < requiredSize) {
                val toFetch = requiredSize - current
                total += fetchInterval(interval, toFetch.toInt())
            }
        }
        return total
    }

    private fun getWith429Backoff(uri: String): String {
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono<String>()
            .block() ?: ""
    }

    fun setWebClientForTest(client: WebClient) {
        this.webClient = client
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
                    log.error(" individual candle node parsing failed: $node", e)
                }
            }
        }
        return saved
    }

    private fun saveLatestCandle(market: String, node: JsonNode) {
        runCatching {
            val candle = mapper.treeToValue(node, Candle::class.java)
            redisService.saveLatestCandle(market, candle)
        }.onFailure { e ->
            log.error("latest candle node parsing failed: $node", e)
        }
    }
}
