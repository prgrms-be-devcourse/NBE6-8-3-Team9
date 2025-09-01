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

    /**
     * 모든 테스트가 시작되기 전, 단 한 번만 실행됩니다.
     * 외부 API(Upbit) 호출을 대체할 MockWebServer를 설정하고 시작합니다.
     */
    @BeforeAll
    fun setupServer() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build()
        restService.setWebClientForTest(webClient)
    }

    /**
     * 모든 테스트가 끝난 후, 단 한 번만 실행됩니다.
     * 사용했던 MockWebServer를 종료합니다.
     */
    @AfterAll
    fun tearDownServer() {
        mockWebServer.shutdown()
    }

    /**
     * 각 테스트 메서드가 실행되기 전에 항상 실행됩니다.
     * DB와 Redis의 데이터를 모두 삭제하여 각 테스트가 독립적인 환경에서 실행되도록 보장합니다.
     */
    @BeforeEach
    fun setUp() {
        coinRepository.deleteAll()
        redisService.clearAll()
        waitForRedisClear()
    }

    /**
     * 테스트 1: 초기 상태에서 특정 코인(BTC)을 DB에 저장하고,
     * Mock API로부터 해당 코인의 캔들 데이터를 받아와 Redis에 정상적으로 저장되는지 검증합니다.
     */
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

        val latestCandleJson = waitForRedisZSetValue(CandleInterval.MIN_1.redisKey("KRW-BTC")).first()
        val btcNode: JsonNode = objectMapper.readTree(latestCandleJson)
        assertThat(btcNode.get("trade_price").asDouble()).isCloseTo(70050000.0, Offset.offset(0.01))
    }

    /**
     * 테스트 2: DB에 코인이 순차적으로 추가될 때,
     * 코인 목록을 제공하는 CoinListProvider가 변경 사항을 올바르게 감지하고 캐시를 갱신하는지 확인합니다.
     */
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

    /**
     * 테스트 3: 기존 코인이 삭제되고 새로운 심볼의 코인이 추가되는 시나리오(사실상 이름/심볼 변경)에서,
     * CoinListProvider가 이를 인지하고 새로운 심볼에 대한 데이터만 정상적으로 수집하는지 검증합니다.
     */
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

        val latestCandleJson = waitForRedisZSetValue(CandleInterval.MIN_1.redisKey("KRW-BTC-NEW")).first()
        val btcNewNode: JsonNode = objectMapper.readTree(latestCandleJson)
        assertThat(btcNewNode.get("trade_price").asDouble()).isCloseTo(71050000.0, Offset.offset(0.01))
    }

    /**
     * 테스트 4: DB에서 코인이 삭제되었을 때,
     * CoinListProvider가 이를 감지하여 내부 코인 목록을 비우는지 확인합니다.
     */
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

    /**
     * 테스트 5: 새로운 코인(ETH)이 DB에 추가되었을 때, 해당 코인이 CoinListProvider에 의해 인식되고,
     * 이 신규 코인에 대한 캔들 데이터를 API로부터 정상적으로 가져와 Redis에 저장할 수 있는지 검증합니다.
     */
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

        val latestCandleJson = waitForRedisZSetValue(CandleInterval.MIN_1.redisKey("KRW-ETH")).first()
        val ethNode: JsonNode = objectMapper.readTree(latestCandleJson)
        assertThat(ethNode.get("trade_price").asDouble()).isCloseTo(3005000.0, Offset.offset(0.01))
    }

    /**
     * 헬퍼 메서드: 비동기적으로 처리되는 Redis 저장을 기다립니다.
     * Sorted Set(ZSet)에서 키가 나타나고, 예상 개수만큼 데이터가 쌓일 때까지 최대 5초간 대기합니다.
     */
    private fun waitForRedisZSetValue(key: String, expectedCount: Int = 1, timeoutMs: Long = 5000): List<String> {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val set = redisTemplate.opsForZSet().reverseRange(key, 0, -1)
            if (!set.isNullOrEmpty() && set.size >= expectedCount) return set.toList()
            Thread.sleep(50)
        }
        throw AssertionError("Redis key $key did not receive $expectedCount value(s) in ZSET within $timeoutMs ms")
    }

    /**
     * 헬퍼 메서드: Redis의 모든 키가 삭제될 때까지 최대 2초간 대기합니다.
     * @BeforeEach에서 clearAll() 호출 후 확실한 초기화를 위해 사용됩니다.
     */
    private fun waitForRedisClear(timeoutMs: Long = 2000) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val keys: Set<String>? = redisTemplate.keys("*")
            if (keys.isNullOrEmpty()) break
            Thread.sleep(50)
        }
    }
}