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

    /**
     * 테스트 시작 전 MockWebServer를 설정하고
     * RestService에 Mock WebClient 주입
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
     * 테스트 종료 후 MockWebServer 종료
     */
    @AfterAll
    fun tearDownServer() {
        mockWebServer.shutdown()
    }

    /**
     * 각 테스트 실행 전 Redis와 DB 초기화
     */
    @BeforeEach
    fun setUp() {
        // DB 초기화
        coinRepository.deleteAll()

        // Redis 초기화 (RedisService의 clearAll 사용)
        redisService.clearAll()
    }

    /**
     * 1번 테스트: 초기 코인 목록 세팅 및 REST API 데이터 수집 검증
     */
    @Test
    @DisplayName("1번: 코인 목록 로드 및 REST API 데이터 수집")
    fun test1_loadCoinsAndFetchRestData() {
        // 1. 코인 목록 DB에 저장
        val btcCoin = Coin("KRW-BTC", "비트코인", "Bitcoin")
        val ethCoin = Coin("KRW-ETH", "이더리움", "Ethereum")
        coinRepository.saveAll(listOf(btcCoin, ethCoin))
        provider.refreshCache()

        // 2. MockWebServer에 API 응답 설정
        val mockBtcCandleJson =
            """[ { "market":"KRW-BTC", "candle_date_time_kst":"2025-08-29T10:20:00", "opening_price":70000000.0, "high_price":70100000.0, "low_price":69900000.0, "trade_price":70050000.0, "timestamp":1756417200000, "candle_acc_trade_price":1000000.0, "candle_acc_trade_volume":0.014 } ]"""
        val mockEthCandleJson =
            """[ { "market":"KRW-ETH", "candle_date_time_kst":"2025-08-29T10:20:00", "opening_price":3000000.0, "high_price":3010000.0, "low_price":2990000.0, "trade_price":3005000.0, "timestamp":1756417200000, "candle_acc_trade_price":500000.0, "candle_acc_trade_volume":0.166 } ]"""
        mockWebServer.enqueue(MockResponse().setBody(mockBtcCandleJson).setHeader("Content-Type", "application/json"))
        mockWebServer.enqueue(MockResponse().setBody(mockEthCandleJson).setHeader("Content-Type", "application/json"))

        // 3. CoinListProvider 검증
        val marketCodes = provider.getMarketCodes()
        assertThat(marketCodes).containsExactlyInAnyOrder("KRW-BTC", "KRW-ETH")
        assertThat(provider.getNameBySymbol("KRW-BTC")).isEqualTo("비트코인")
        assertThat(provider.getNameBySymbol("KRW-ETH")).isEqualTo("이더리움")

        // 4. RestService를 통해 데이터 수집 및 Redis 저장
        val savedCount = restService.fetchInterval(CandleInterval.MIN_1, 1)
        assertThat(savedCount).isEqualTo(2)

        // 5. Redis 저장 데이터 검증
        val btcRedisKey = CandleInterval.MIN_1.redisKey("KRW-BTC")
        val btcRedisData = redisTemplate.opsForList().range(btcRedisKey, 0, -1)
        val btcNode: JsonNode = objectMapper.readTree(btcRedisData!!.first())
        assertThat(btcNode.get("trade_price").asDouble()).isEqualTo(70050000.0)

        val ethRedisKey = CandleInterval.MIN_1.redisKey("KRW-ETH")
        val ethRedisData = redisTemplate.opsForList().range(ethRedisKey, 0, -1)
        val ethNode: JsonNode = objectMapper.readTree(ethRedisData!!.first())
        assertThat(ethNode.get("trade_price").asDouble()).isEqualTo(3005000.0)
    }

    /**
     * 2번 테스트: 코인 목록 변동 감지 검증
     */
    @Test
    @DisplayName("2번: 코인 목록 변동 감지 테스트")
    fun test2_detectCoinListChanges() {
        // 1. 초기 코인 DB 세팅
        val btcCoin = Coin("KRW-BTC", "비트코인", "Bitcoin")
        coinRepository.save(btcCoin)
        provider.refreshCache()

        // 2. 새로운 코인 추가
        val newCoin = Coin("KRW-ETH", "이더리움", "Ethereum")
        coinRepository.save(newCoin)
        provider.refreshCache()

        // 3. 변동 감지 확인
        val marketCodes = provider.getMarketCodes()
        assertThat(marketCodes).containsExactlyInAnyOrder("KRW-BTC", "KRW-ETH")
    }

    /**
     * 3번 테스트: 코인 변동 반영 후 데이터 수집 및 Redis 저장
     */
    @Test
    @DisplayName("3번: 코인 변동 반영 후 데이터 수집 및 Redis 저장")
    fun test3_fetchDataAfterCoinChange() {
        // 1. 기존 코인 목록 세팅
        val btcCoin = Coin("KRW-BTC", "비트코인", "Bitcoin")
        coinRepository.save(btcCoin)
        provider.refreshCache()

        // 2. 새로운 코인 추가 (변동)
        val ethCoin = Coin("KRW-ETH", "이더리움", "Ethereum")
        coinRepository.save(ethCoin)
        provider.refreshCache()

        // 3. MockWebServer 응답 설정
        val mockBtcCandleJson =
            """[ { "market":"KRW-BTC", "candle_date_time_kst":"2025-08-29T10:20:00", "opening_price":70000000.0, "high_price":70100000.0, "low_price":69900000.0, "trade_price":70050000.0, "timestamp":1756417200000, "candle_acc_trade_price":1000000.0, "candle_acc_trade_volume":0.014 } ]"""
        val mockEthCandleJson =
            """[ { "market":"KRW-ETH", "candle_date_time_kst":"2025-08-29T10:20:00", "opening_price":3000000.0, "high_price":3010000.0, "low_price":2990000.0, "trade_price":3005000.0, "timestamp":1756417200000, "candle_acc_trade_price":500000.0, "candle_acc_trade_volume":0.166 } ]"""
        mockWebServer.enqueue(MockResponse().setBody(mockBtcCandleJson).setHeader("Content-Type", "application/json"))
        mockWebServer.enqueue(MockResponse().setBody(mockEthCandleJson).setHeader("Content-Type", "application/json"))

        // 4. RestService로 데이터 수집
        val savedCount = restService.fetchInterval(CandleInterval.MIN_1, 1)
        assertThat(savedCount).isEqualTo(2)

        // 5. Redis 저장 데이터 검증
        val btcNode: JsonNode = objectMapper.readTree(
            redisTemplate.opsForList().range(CandleInterval.MIN_1.redisKey("KRW-BTC"), 0, -1)!!.first()
        )
        assertThat(btcNode.get("trade_price").asDouble()).isEqualTo(70050000.0)

        val ethNode: JsonNode = objectMapper.readTree(
            redisTemplate.opsForList().range(CandleInterval.MIN_1.redisKey("KRW-ETH"), 0, -1)!!.first()
        )
        assertThat(ethNode.get("trade_price").asDouble()).isEqualTo(3005000.0)
    }
}
