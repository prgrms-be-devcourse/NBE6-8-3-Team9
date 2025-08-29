package com.back.back9.domain.websocket.service

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.service.RedisService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarketDataIntegrationTest {

    @Autowired
    private lateinit var restService: RestService

    @Autowired
    private lateinit var coinRepository: CoinRepository

    @Autowired
    private lateinit var provider: DatabaseCoinListProvider

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var redisService: RedisService

    private lateinit var mockWebServer: MockWebServer

    private lateinit var webClient: WebClient

    @BeforeAll
    fun setupServer() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build()
        restService.setWebClientForTest(webClient)
    }

    @AfterAll
    fun tearDownServer() {
        mockWebServer.shutdown()
    }

    @BeforeEach
    fun setUp() {
        coinRepository.deleteAll()
        redisService.clearAll()

        // CI 환경에서 Redis 초기화 확인용 로그
        val keys = redisTemplate.keys("*")
        println("Redis keys after clearAll: $keys")
        assertThat(keys).isEmpty()
    }

    private fun waitForRedisValue(key: String, timeoutMs: Long = 2000): List<String> {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val list = redisTemplate.opsForList().range(key, 0, -1)
            if (!list.isNullOrEmpty()) return list
            Thread.sleep(50)
        }
        throw AssertionError("Redis key $key did not receive value within $timeoutMs ms")
    }

    @Test
    @DisplayName("1번: 코인 목록 로드 및 REST API 데이터 수집")
    fun test1_loadCoinsAndFetchRestData() {
        val btcCoin = Coin("KRW-BTC", "비트코인", "Bitcoin")
        val ethCoin = Coin("KRW-ETH", "이더리움", "Ethereum")
        coinRepository.saveAll(listOf(btcCoin, ethCoin))
        provider.refreshCache()

        val mockBtcCandleJson = """[{"market":"KRW-BTC","candle_date_time_kst":"2025-08-29T10:20:00","opening_price":70000000.0,"high_price":70100000.0,"low_price":69900000.0,"trade_price":70050000.0,"timestamp":1756417200000,"candle_acc_trade_price":1000000.0,"candle_acc_trade_volume":0.014}]"""
        val mockEthCandleJson = """[{"market":"KRW-ETH","candle_date_time_kst":"2025-08-29T10:20:00","opening_price":3000000.0,"high_price":3010000.0,"low_price":2990000.0,"trade_price":3005000.0,"timestamp":1756417200000,"candle_acc_trade_price":500000.0,"candle_acc_trade_volume":0.166}]"""
        mockWebServer.enqueue(MockResponse().setBody(mockBtcCandleJson).setHeader("Content-Type", "application/json"))
        mockWebServer.enqueue(MockResponse().setBody(mockEthCandleJson).setHeader("Content-Type", "application/json"))

        val marketCodes = provider.getMarketCodes()
        assertThat(marketCodes).containsExactlyInAnyOrder("KRW-BTC", "KRW-ETH")

        val savedCount = restService.fetchInterval(CandleInterval.MIN_1, 1)
        assertThat(savedCount).isEqualTo(2)

        val btcRedisKey = CandleInterval.MIN_1.redisKey("KRW-BTC")
        val btcRedisData = waitForRedisValue(btcRedisKey)
        val btcNode: JsonNode = objectMapper.readTree(btcRedisData.first())
        assertThat(btcNode.get("trade_price").asDouble()).isCloseTo(70050000.0, org.assertj.core.data.Offset.offset(0.01))

        val ethRedisKey = CandleInterval.MIN_1.redisKey("KRW-ETH")
        val ethRedisData = waitForRedisValue(ethRedisKey)
        val ethNode: JsonNode = objectMapper.readTree(ethRedisData.first())
        assertThat(ethNode.get("trade_price").asDouble()).isCloseTo(3005000.0, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    @DisplayName("2번: 코인 목록 변동 감지 테스트")
    fun test2_detectCoinListChanges() {
        val btcCoin = Coin("KRW-BTC", "비트코인", "Bitcoin")
        coinRepository.save(btcCoin)
        provider.refreshCache()

        val newCoin = Coin("KRW-ETH", "이더리움", "Ethereum")
        coinRepository.save(newCoin)
        provider.refreshCache()

        val marketCodes = provider.getMarketCodes()
        assertThat(marketCodes).containsExactlyInAnyOrder("KRW-BTC", "KRW-ETH")
    }

    @Test
    @DisplayName("3번: 코인 변동 반영 후 데이터 수집 및 Redis 저장")
    fun test3_fetchDataAfterCoinChange() {
        val btcCoin = Coin("KRW-BTC", "비트코인", "Bitcoin")
        coinRepository.save(btcCoin)
        provider.refreshCache()

        val ethCoin = Coin("KRW-ETH", "이더리움", "Ethereum")
        coinRepository.save(ethCoin)
        provider.refreshCache()

        val mockBtcCandleJson = """[{"market":"KRW-BTC","candle_date_time_kst":"2025-08-29T10:20:00","opening_price":70000000.0,"high_price":70100000.0,"low_price":69900000.0,"trade_price":70050000.0,"timestamp":1756417200000,"candle_acc_trade_price":1000000.0,"candle_acc_trade_volume":0.014}]"""
        val mockEthCandleJson = """[{"market":"KRW-ETH","candle_date_time_kst":"2025-08-29T10:20:00","opening_price":3000000.0,"high_price":3010000.0,"low_price":2990000.0,"trade_price":3005000.0,"timestamp":1756417200000,"candle_acc_trade_price":500000.0,"candle_acc_trade_volume":0.166}]"""
        mockWebServer.enqueue(MockResponse().setBody(mockBtcCandleJson).setHeader("Content-Type", "application/json"))
        mockWebServer.enqueue(MockResponse().setBody(mockEthCandleJson).setHeader("Content-Type", "application/json"))

        val savedCount = restService.fetchInterval(CandleInterval.MIN_1, 1)
        assertThat(savedCount).isEqualTo(2)

        val btcNode: JsonNode = objectMapper.readTree(waitForRedisValue(CandleInterval.MIN_1.redisKey("KRW-BTC")).first())
        assertThat(btcNode.get("trade_price").asDouble()).isCloseTo(70050000.0, org.assertj.core.data.Offset.offset(0.01))

        val ethNode: JsonNode = objectMapper.readTree(waitForRedisValue(CandleInterval.MIN_1.redisKey("KRW-ETH")).first())
        assertThat(ethNode.get("trade_price").asDouble()).isCloseTo(3005000.0, org.assertj.core.data.Offset.offset(0.01))
    }
}