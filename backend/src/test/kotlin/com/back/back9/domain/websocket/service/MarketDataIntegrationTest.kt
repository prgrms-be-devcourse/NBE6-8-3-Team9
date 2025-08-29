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
import org.assertj.core.data.Offset
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
        // DB 초기화
        coinRepository.deleteAll()
        // Redis 초기화
        redisService.clearAll()
        waitForRedisClear()
    }

    @Test
    @DisplayName("1. 초기 데이터 세팅")
    fun testInitialDataSetup() {
        val btc = Coin("KRW-BTC", "비트코인", "Bitcoin")
        coinRepository.save(btc)
        provider.refreshCache()

        val btcJson = """[{"market":"KRW-BTC","candle_date_time_kst":"2025-08-29T10:20:00","opening_price":70000000.0,"high_price":70100000.0,"low_price":69900000.0,"trade_price":70050000.0,"timestamp":1756417200000,"candle_acc_trade_price":1000000.0,"candle_acc_trade_volume":0.014}]"""
        mockWebServer.enqueue(MockResponse().setBody(btcJson).setHeader("Content-Type", "application/json"))

        val saved = restService.fetchInterval(CandleInterval.MIN_1, 1)
        assertThat(saved).isEqualTo(1)

        val btcNode: JsonNode = objectMapper.readTree(waitForRedisValue(CandleInterval.MIN_1.redisKey("KRW-BTC")).last())
        assertThat(btcNode.get("trade_price").asDouble()).isCloseTo(70050000.0, Offset.offset(0.01))
    }

    @Test
    @DisplayName("2. 코인 목록 변화 감지")
    fun testCoinListChangeDetection() {
        val btc = Coin("KRW-BTC", "비트코인", "Bitcoin")
        coinRepository.save(btc)
        provider.refreshCache()

        val eth = Coin("KRW-ETH", "이더리움", "Ethereum")
        coinRepository.save(eth)
        provider.refreshCache()

        val marketCodes = provider.getMarketCodes()
        assertThat(marketCodes).containsExactlyInAnyOrder("KRW-BTC", "KRW-ETH")
    }

    @Test
    @DisplayName("3. 코인 이름/심볼 수정")
    fun testCoinRename() {
        val btc = Coin("KRW-BTC", "비트코인", "Bitcoin")
        coinRepository.save(btc)
        provider.refreshCache()

        coinRepository.deleteAll()
        val btcNew = Coin("KRW-BTC-NEW", "비트코인 수정", "BitcoinNew")
        coinRepository.save(btcNew)
        provider.refreshCache()

        val btcJson = """[{"market":"KRW-BTC-NEW","candle_date_time_kst":"2025-08-29T10:25:00","opening_price":71000000.0,"high_price":71100000.0,"low_price":70900000.0,"trade_price":71050000.0,"timestamp":1756417500000,"candle_acc_trade_price":1200000.0,"candle_acc_trade_volume":0.018}]"""
        mockWebServer.enqueue(MockResponse().setBody(btcJson).setHeader("Content-Type", "application/json"))

        val saved = restService.fetchInterval(CandleInterval.MIN_1, 1)
        assertThat(saved).isEqualTo(1)

        // 기존 데이터 삭제 확인
        assertThat(redisTemplate.hasKey(CandleInterval.MIN_1.redisKey("KRW-BTC"))).isFalse
        val btcNewNode: JsonNode = objectMapper.readTree(waitForRedisValue(CandleInterval.MIN_1.redisKey("KRW-BTC-NEW")).last())
        assertThat(btcNewNode.get("trade_price").asDouble()).isCloseTo(71050000.0, Offset.offset(0.01))
    }

    @Test
    @DisplayName("4. 코인 목록 삭제")
    fun testCoinDeletion() {
        val btc = Coin("KRW-BTC", "비트코인", "Bitcoin")
        coinRepository.save(btc)
        provider.refreshCache()

        coinRepository.deleteAll()
        provider.refreshCache()

        assertThat(provider.getMarketCodes()).isEmpty()
        assertThat(redisTemplate.keys("*")).isEmpty()
    }

    @Test
    @DisplayName("5. 신규 코인 추가")
    fun testCoinAddition() {
        val eth = Coin("KRW-ETH", "이더리움", "Ethereum")
        coinRepository.save(eth)
        provider.refreshCache()

        val ethJson = """[{"market":"KRW-ETH","candle_date_time_kst":"2025-08-29T10:20:00","opening_price":3000000.0,"high_price":3010000.0,"low_price":2990000.0,"trade_price":3005000.0,"timestamp":1756417200000,"candle_acc_trade_price":500000.0,"candle_acc_trade_volume":0.166}]"""
        mockWebServer.enqueue(MockResponse().setBody(ethJson).setHeader("Content-Type", "application/json"))

        val saved = restService.fetchInterval(CandleInterval.MIN_1, 1)
        assertThat(saved).isEqualTo(1)

        val ethNode: JsonNode = objectMapper.readTree(waitForRedisValue(CandleInterval.MIN_1.redisKey("KRW-ETH")).last())
        assertThat(ethNode.get("trade_price").asDouble()).isCloseTo(3005000.0, Offset.offset(0.01))
    }

    private fun waitForRedisValue(key: String, expectedCount: Int = 1, timeoutMs: Long = 5000): List<String> {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val list = redisTemplate.opsForList().range(key, 0, -1)
            if (!list.isNullOrEmpty() && list.size >= expectedCount) return list
            Thread.sleep(50)
        }
        throw AssertionError("Redis key $key did not receive $expectedCount value(s) within $timeoutMs ms")
    }

    private fun waitForRedisClear(timeoutMs: Long = 2000) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val keys: Set<String>? = redisTemplate.keys("*")
            if (keys.isNullOrEmpty()) break
            Thread.sleep(50)
        }
    }
}
