package com.back.back9.domain.exchange.service

import com.back.back9.domain.exchange.entity.Exchange
import com.back.back9.domain.exchange.repository.ExchangeRepository
import com.back.back9.domain.websocket.service.DatabaseCoinListProvider
import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.dto.Candle
import com.back.back9.global.redis.service.RedisService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@DisplayName("ExchangeBackupService 단위 테스트")
class ExchangeBackupServiceTest {

    @Mock
    private lateinit var redisService: RedisService

    @Mock
    private lateinit var exchangeRepository: ExchangeRepository

    @Mock
    private lateinit var coinListProvider: DatabaseCoinListProvider

    @InjectMocks
    private lateinit var backupService: ExchangeBackupService

    companion object {
        private const val BTC_SYMBOL = "KRW-BTC"
        private const val ETH_SYMBOL = "KRW-ETH"
        private const val XRP_SYMBOL = "KRW-XRP" // 동기화 테스트용 심볼
        private val DUMMY_CANDLE_1 = Candle("2025-08-31T10:00:00", BigDecimal(100), BigDecimal(110), BigDecimal(90), BigDecimal(105), timestamp = 1L)
        private val DUMMY_CANDLE_2 = Candle("2025-08-31T10:01:00", BigDecimal(200), BigDecimal(220), BigDecimal(180), BigDecimal(210), timestamp = 2L)
    }

    // --- 데이터 백업 로직 테스트 ---

    @Test
    @DisplayName("성공적으로 데이터를 백업한다")
    fun backupSuccess() {
        // given: 백업할 데이터가 존재하는 상황을 준비합니다.
        val btcMin1Key = CandleInterval.MIN_1.redisKey(BTC_SYMBOL)
        val ethDayKey = CandleInterval.DAY.redisKey(ETH_SYMBOL)
        whenever(coinListProvider.getMarketCodes()).thenReturn(listOf(BTC_SYMBOL, ETH_SYMBOL))
        doAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            when (key) {
                btcMin1Key -> listOf(DUMMY_CANDLE_1, DUMMY_CANDLE_2)
                ethDayKey -> listOf(DUMMY_CANDLE_1)
                else -> emptyList()
            }
        }.`when`(redisService).getAndRemoveCandlesBefore(anyString(), anyLong())

        // when: 백업 로직을 실행합니다.
        backupService.backupDataFromRedisToDB()

        // then: DB 저장 메서드가 올바른 데이터와 함께 호출되었는지 검증합니다.
        val captor = argumentCaptor<List<Exchange>>()
        verify(exchangeRepository).saveAll(captor.capture())
        val backedUpEntities = captor.firstValue
        assertThat(backedUpEntities).hasSize(3)
    }

    @Test
    @DisplayName("백업할 데이터가 없으면 DB 저장을 시도하지 않는다")
    fun backupEmpty() {
        // given: 백업할 데이터가 없는 상황을 준비합니다.
        whenever(coinListProvider.getMarketCodes()).thenReturn(listOf(BTC_SYMBOL))
        doReturn(emptyList<Candle>()).`when`(redisService).getAndRemoveCandlesBefore(anyString(), anyLong())

        // when: 백업 로직을 실행합니다.
        backupService.backupDataFromRedisToDB()

        // then: DB 저장 메서드가 호출되지 않았음을 검증합니다.
        verify(exchangeRepository, never()).saveAll(any<List<Exchange>>())
    }

    @Test
    @DisplayName("일부 데이터 백업 실패 시에도 안정적으로 동작한다")
    fun backupResilient() {
        // given: 특정 코인(ETH)의 데이터 조회 시 예외가 발생하는 상황을 준비합니다.
        val btcMin1Key = CandleInterval.MIN_1.redisKey(BTC_SYMBOL)
        val ethDayKey = CandleInterval.DAY.redisKey(ETH_SYMBOL)
        whenever(coinListProvider.getMarketCodes()).thenReturn(listOf(BTC_SYMBOL, ETH_SYMBOL))
        doAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            when (key) {
                btcMin1Key -> listOf(DUMMY_CANDLE_1)
                ethDayKey  -> throw RuntimeException("Redis connection failed")
                else       -> emptyList()
            }
        }.`when`(redisService).getAndRemoveCandlesBefore(anyString(), anyLong())

        // when: 백업 로직을 실행합니다.
        backupService.backupDataFromRedisToDB()

        // then: 오류가 발생하지 않은 데이터만이라도 정상적으로 저장되는지 검증합니다.
        val captor = argumentCaptor<List<Exchange>>()
        verify(exchangeRepository).saveAll(captor.capture())
        val backedUpEntities = captor.firstValue
        assertThat(backedUpEntities).hasSize(1)
        assertThat(backedUpEntities.first().symbol).isEqualTo(BTC_SYMBOL)
    }

    // --- RDB-Provider 동기화 로직 테스트 ---

    @Test
    @DisplayName("동기화: Provider에 없는 코인의 RDB 데이터를 삭제한다")
    fun synchronizeDeletesStaleData() {
        // given: Provider에는 BTC만 있지만, RDB에는 BTC, ETH, XRP가 있는 상황을 준비합니다.
        whenever(coinListProvider.getMarketCodes()).thenReturn(listOf(BTC_SYMBOL))
        whenever(exchangeRepository.findDistinctSymbols()).thenReturn(listOf(BTC_SYMBOL, ETH_SYMBOL, XRP_SYMBOL))

        // when: 동기화 로직을 실행합니다.
        backupService.synchronizeRdbRecordsWithProvider()

        // then: 삭제 메서드가 ETH와 XRP 심볼과 함께 호출되었는지 검증합니다.
        val captor = argumentCaptor<List<String>>()
        verify(exchangeRepository).deleteBySymbolIn(captor.capture())

        val symbolsToDelete = captor.firstValue
        assertThat(symbolsToDelete).hasSize(2)
        assertThat(symbolsToDelete).containsExactlyInAnyOrder(ETH_SYMBOL, XRP_SYMBOL)
    }

    @Test
    @DisplayName("동기화: RDB와 Provider 목록이 동일하면 아무것도 삭제하지 않는다")
    fun synchronizeDoesNothingWhenSynced() {
        // given: Provider와 RDB의 코인 목록이 동일한 상황을 준비합니다.
        whenever(coinListProvider.getMarketCodes()).thenReturn(listOf(BTC_SYMBOL, ETH_SYMBOL))
        whenever(exchangeRepository.findDistinctSymbols()).thenReturn(listOf(BTC_SYMBOL, ETH_SYMBOL))

        // when: 동기화 로직을 실행합니다.
        backupService.synchronizeRdbRecordsWithProvider()

        // then: 삭제 메서드가 호출되지 않았음을 검증합니다.
        verify(exchangeRepository, never()).deleteBySymbolIn(any())
    }

    @Test
    @DisplayName("동기화: RDB에 오래된 데이터가 없으면 아무것도 삭제하지 않는다")
    fun synchronizeDoesNothingWhenRdbIsClean() {
        // given: Provider에 코인이 더 많거나 같은 상황을 준비합니다.
        whenever(coinListProvider.getMarketCodes()).thenReturn(listOf(BTC_SYMBOL, ETH_SYMBOL))
        whenever(exchangeRepository.findDistinctSymbols()).thenReturn(listOf(BTC_SYMBOL))

        // when: 동기화 로직을 실행합니다.
        backupService.synchronizeRdbRecordsWithProvider()

        // then: 삭제할 데이터가 없으므로 삭제 메서드가 호출되지 않았음을 검증합니다.
        verify(exchangeRepository, never()).deleteBySymbolIn(any())
    }
}
