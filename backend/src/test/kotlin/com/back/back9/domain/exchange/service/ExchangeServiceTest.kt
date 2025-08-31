package com.back.back9.domain.exchange.service

import com.back.back9.domain.exchange.vo.CandleFetchParameters
import com.back.back9.domain.websocket.service.DatabaseCoinListProvider
import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.dto.Candle
import com.back.back9.global.redis.service.RedisService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@DisplayName("ExchangeService 단위 테스트")
class ExchangeServiceTest {

    @Mock
    private lateinit var redisService: RedisService

    @Mock
    private lateinit var coinListProvider: DatabaseCoinListProvider

    @InjectMocks
    private lateinit var exchangeService: ExchangeService

    private val btcSymbol = "KRW-BTC"
    private val ethSymbol = "KRW-ETH"
    private val dummyCandle1 = Candle(
        "2025-08-31T10:00:00",
        BigDecimal(7000),
        BigDecimal(7100),
        BigDecimal(6900),
        BigDecimal(7050),
        timestamp = 1L
    )
    private val dummyCandle2 = Candle(
        "2025-08-31T10:01:00",
        BigDecimal(7050),
        BigDecimal(7150),
        BigDecimal(7000),
        BigDecimal(7100),
        timestamp = 2L
    )

    @Test
    @DisplayName("초기 캔들 조회 성공")
    fun fetchInitialSuccess() {
        // Given: Redis에 데이터가 존재하도록 설정합니다.
        val dummyCandles = listOf(dummyCandle1, dummyCandle2)
        whenever(redisService.getLatestCandles(any(), any())).thenReturn(dummyCandles)

        // When: 초기 캔들 조회 메서드를 호출합니다.
        val result = exchangeService.getInitialCandles(CandleInterval.MIN_1, btcSymbol)

        // Then: 반환된 DTO 리스트가 올바른지 검증합니다.
        Assertions.assertThat(result).hasSize(2)
        Assertions.assertThat(result[0].symbol).isEqualTo(btcSymbol)
        verify(redisService).getLatestCandles(any(), eq(CandleFetchParameters.INITIAL_CANDLE_COUNT))
    }

    @Test
    @DisplayName("초기 캔들 조회 (데이터 없음)")
    fun fetchInitialEmpty() {
        // Given: Redis가 빈 리스트를 반환하도록 설정합니다.
        whenever(redisService.getLatestCandles(any(), any())).thenReturn(emptyList())

        // When: 초기 캔들 조회 메서드를 호출합니다.
        val result = exchangeService.getInitialCandles(CandleInterval.MIN_1, btcSymbol)

        // Then: 빈 리스트가 반환되었는지 검증합니다.
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("이전 캔들 조회 성공")
    fun fetchPreviousSuccess() {
        // Given: Redis가 커서 이전의 데이터를 반환하도록 설정합니다.
        val dummyCandles = listOf(dummyCandle1, dummyCandle2)
        val cursorTimestamp = 3L
        whenever(redisService.getCandlesBefore(any(), eq(cursorTimestamp), any())).thenReturn(dummyCandles)

        // When: 이전 캔들 조회 메서드를 호출합니다.
        val result = exchangeService.getPreviousCandles(CandleInterval.MIN_1, btcSymbol, cursorTimestamp)

        // Then: 반환된 DTO 리스트가 올바른지 검증합니다.
        Assertions.assertThat(result).hasSize(2)
        verify(redisService).getCandlesBefore(
            any(),
            eq(cursorTimestamp),
            eq(CandleFetchParameters.PREVIOUS_CANDLE_COUNT)
        )
    }

    @Test
    @DisplayName("최신 시세 조회 성공")
    fun fetchLatestSuccess() {
        // Given: 모든 코인의 데이터와 이름이 존재하도록 설정합니다.
        whenever(coinListProvider.getMarketCodes()).thenReturn(listOf(btcSymbol, ethSymbol))
        whenever(redisService.getLatestCandle(CandleInterval.SEC.redisKey(btcSymbol))).thenReturn(dummyCandle1)
        whenever(redisService.getLatestCandle(CandleInterval.SEC.redisKey(ethSymbol))).thenReturn(dummyCandle2)
        whenever(coinListProvider.getNameBySymbol(btcSymbol)).thenReturn("비트코인")
        whenever(coinListProvider.getNameBySymbol(ethSymbol)).thenReturn("이더리움")

        // When: 최신 시세 프로퍼티에 접근합니다.
        val result = exchangeService.coinsLatest

        // Then: 모든 코인 정보가 포함된 DTO 리스트가 반환되었는지 검증합니다.
        Assertions.assertThat(result).hasSize(2)
        Assertions.assertThat(result[0].name).isEqualTo("비트코인")
        Assertions.assertThat(result[1].name).isEqualTo("이더리움")
    }

    @Test
    @DisplayName("최신 시세 조회 (부분 데이터)")
    fun fetchLatestPartial() {
        // Given: 일부 코인의 데이터가 누락된 상황을 설정합니다.
        whenever(coinListProvider.getMarketCodes()).thenReturn(listOf(btcSymbol, ethSymbol))
        whenever(redisService.getLatestCandle(CandleInterval.SEC.redisKey(btcSymbol))).thenReturn(dummyCandle1)
        whenever(redisService.getLatestCandle(CandleInterval.SEC.redisKey(ethSymbol))).thenReturn(null)
        whenever(coinListProvider.getNameBySymbol(btcSymbol)).thenReturn("비트코인")

        // When: 최신 시세 프로퍼티에 접근합니다.
        val result = exchangeService.coinsLatest

        // Then: 데이터가 있는 코인만 리스트에 포함되는지 검증합니다.
        Assertions.assertThat(result).hasSize(1)
        Assertions.assertThat(result[0].symbol).isEqualTo(btcSymbol)
    }

    @Test
    @DisplayName("개별 시세 조회 성공")
    fun scanCandleSuccess() {
        // Given: Redis에 특정 캔들 데이터가 존재하도록 설정합니다.
        whenever(redisService.getLatestCandle(any())).thenReturn(dummyCandle1)

        // When: 개별 시세 조회 메서드를 호출합니다.
        val result = exchangeService.getLatestCandleByScan(btcSymbol)

        // Then: 가격 정보가 포함된 DTO가 반환되었는지 검증합니다.
        Assertions.assertThat(result.symbol).isEqualTo(btcSymbol)
        Assertions.assertThat(result.price).isEqualTo(dummyCandle1.tradePrice)
    }

    @Test
    @DisplayName("개별 시세 조회 (데이터 없음)")
    fun scanCandleMissing() {
        // Given: Redis에 데이터가 존재하지 않도록 설정합니다.
        whenever(redisService.getLatestCandle(any())).thenReturn(null)

        // When: 개별 시세 조회 메서드를 호출합니다.
        val result = exchangeService.getLatestCandleByScan(btcSymbol)

        // Then: 가격이 0으로 채워진 DTO가 반환되었는지 검증합니다.
        Assertions.assertThat(result.symbol).isEqualTo(btcSymbol)
        Assertions.assertThat(result.price).isEqualByComparingTo(BigDecimal.ZERO)
    }
}