package com.back.back9.global.redis.service

import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.dto.Candle
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class RedisService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(RedisService::class.java)

    /** 캔들 데이터를 Redis의 Sorted Set에 저장합니다. score는 candle의 timestamp입니다. */
    fun saveCandle(key: String, candle: Candle) {
        try {
            val candleJson = objectMapper.writeValueAsString(candle)
            redisTemplate.opsForZSet().add(key, candleJson, candle.timestamp.toDouble())
        } catch (e: JsonProcessingException) {
            log.error("Candle 객체를 JSON으로 변환하는 데 실패했습니다. Key: {}, Candle: {}", key, candle, e)
        }
    }

    /** JsonNode 형태의 캔들 데이터를 저장합니다. */
    fun saveCandle(interval: CandleInterval, market: String, candleNode: JsonNode) {
        try {
            val candle = objectMapper.treeToValue(candleNode, Candle::class.java)
            saveCandle(interval.redisKey(market), candle)
        } catch (e: JsonProcessingException) {
            log.error("JsonNode를 Candle 객체로 변환하는 데 실패했습니다. Market: {}, Node: {}", market, candleNode, e)
        }
    }

    /** 최신 캔들을 저장합니다. */
    fun saveLatestCandle(market: String, candle: Candle) {
        val key = CandleInterval.SEC.redisKey(market)
        saveCandle(key, candle)
    }

    /** 최신 캔들을 JsonNode로 저장합니다. */
    fun saveLatestCandle(market: String, candleNode: JsonNode) {
        try {
            val candle = objectMapper.treeToValue(candleNode, Candle::class.java)
            saveLatestCandle(market, candle)
        } catch (e: JsonProcessingException) {
            log.error("JsonNode를 Candle 객체로 변환하는 데 실패했습니다. Market: {}, Node: {}", market, candleNode, e)
        }
    }

    /** 지정된 키에서 최신 'count'개의 캔들을 조회합니다. */
    fun getLatestCandles(key: String, count: Int): List<Candle> {
        val candleJsonSet = redisTemplate.opsForZSet()
            .reverseRange(key, 0, count.toLong() - 1)
        return parseCandleSet(key, candleJsonSet)
    }

    /** 커서(timestamp) 이전의 캔들을 조회합니다. */
    fun getCandlesBefore(key: String, cursorTimestamp: Long, count: Int): List<Candle> {
        val candleJsonSet = redisTemplate.opsForZSet()
            .reverseRangeByScore(key, Double.NEGATIVE_INFINITY, (cursorTimestamp - 1).toDouble(), 0, count.toLong())
        return parseCandleSet(key, candleJsonSet)
    }

    /** 가장 최신 캔들 1개를 조회합니다. */
    fun getLatestCandle(key: String): Candle? {
        val candleJson = redisTemplate.opsForZSet()
            .reverseRange(key, 0, 0)
            ?.firstOrNull() ?: return null
        return try {
            objectMapper.readValue(candleJson, Candle::class.java)
        } catch (e: JsonProcessingException) {
            log.error("JSON을 Candle 객체로 변환하는 데 실패했습니다. Key: {}, JSON: {}", key, candleJson, e)
            null
        }
    }

    /** JSON Set을 Candle 리스트로 변환합니다. */
    private fun parseCandleSet(key: String, candleJsonSet: Set<String>?): List<Candle> {
        if (candleJsonSet.isNullOrEmpty()) return emptyList()
        return candleJsonSet.mapNotNull { json ->
            try {
                objectMapper.readValue(json, Candle::class.java)
            } catch (e: JsonProcessingException) {
                log.error("JSON을 Candle 객체로 변환하는 데 실패했습니다. Key: {}, JSON: {}", key, json, e)
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    /** 캔들 개수를 반환합니다. */
    fun countCandles(key: String): Long =
        redisTemplate.opsForZSet().zCard(key) ?: 0L

    /** Redis의 모든 키를 삭제합니다. */
    fun clearAll() {
        val keys = redisTemplate.keys("*")
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
            log.info("Redis의 모든 데이터를 삭제했습니다. (총 {}개 키)", keys.size)
        }
    }

    /** 특정 timestamp 이전의 데이터를 조회 후 삭제합니다. */
    fun getAndRemoveCandlesBefore(key: String, endTimestamp: Long): List<Candle> {
        val minScore = Double.NEGATIVE_INFINITY
        val maxScore = endTimestamp.toDouble()
        val candleJsonSet = redisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore)
            ?: return emptyList()

        redisTemplate.opsForZSet().removeRangeByScore(key, minScore, maxScore)

        log.info("Key '{}'에서 {} 이전 데이터 {}개를 백업하고 삭제했습니다.", key, endTimestamp, candleJsonSet.size)

        return parseCandleSet(key, candleJsonSet)
    }
}
