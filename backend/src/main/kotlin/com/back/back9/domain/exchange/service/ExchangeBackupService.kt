package com.back.back9.domain.exchange.service

import com.back.back9.domain.exchange.entity.Exchange
import com.back.back9.domain.exchange.repository.ExchangeRepository
import com.back.back9.domain.websocket.service.DatabaseCoinListProvider
import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.dto.Candle
import com.back.back9.global.redis.service.RedisService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ExchangeBackupService(
    private val redisService: RedisService,
    private val exchangeRepository: ExchangeRepository,
    private val coinListProvider: DatabaseCoinListProvider
) {

    companion object {
        private val log = LoggerFactory.getLogger(ExchangeBackupService::class.java)
    }

    @Transactional
    fun backupDataFromRedisToDB() {
        log.info("🚀 Redis -> RDB 데이터 백업 작업을 시작합니다.")

        val cutoffTimestamp = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val markets = coinListProvider.getMarketCodes()
        // [수정] CandleInterval.SEC (최신 데이터)를 제외한 나머지 주기만 백업 대상으로 지정합니다.
        val intervalsToBackup = CandleInterval.entries.filter { it != CandleInterval.SEC }
        val totalBackupList = mutableListOf<Exchange>()

        for (market in markets) {
            // [수정] 필터링된 주기 목록을 사용합니다.
            for (interval in intervalsToBackup) {
                try {
                    val key = interval.redisKey(market)
                    val candlesToBackup = redisService.getAndRemoveCandlesBefore(key, cutoffTimestamp)

                    if (candlesToBackup.isNotEmpty()) {
                        val exchanges = candlesToBackup.map { candle ->
                            convertToExchangeEntity(candle, market)
                        }
                        totalBackupList.addAll(exchanges)
                    }
                } catch (e: Exception) {
                    log.error("❌ 키 '{}' 백업 중 오류가 발생했습니다. 이 키는 건너뜁니다.", interval.redisKey(market), e)
                }
            }
        }

        if (totalBackupList.isNotEmpty()) {
            exchangeRepository.saveAll(totalBackupList)
            log.info("✅ 총 {}개의 캔들 데이터를 RDB에 성공적으로 백업했습니다.", totalBackupList.size)
        } else {
            log.info("ℹ️ 백업할 데이터가 없습니다. 작업을 종료합니다.")
        }
    }

    /** RDB에 백업된 코인 목록과 Provider의 최신 코인 목록을 동기화하는 기능입니다. */
    @Transactional
    fun synchronizeRdbRecordsWithProvider() {
        log.info("🔍 RDB와 Provider의 코인 목록 동기화 작업을 시작합니다.")

        // 1. Provider로부터 현재 활성화된 코인 심볼 목록을 가져옵니다.
        val activeSymbols = coinListProvider.getMarketCodes().toSet()

        // 2. RDB에 저장된 모든 코인 심볼 목록을 가져옵니다.
        val backedUpSymbols = exchangeRepository.findDistinctSymbols().toSet()

        // 3. RDB에는 있지만 Provider에는 없는, 즉 삭제된 코인 심볼 목록을 찾습니다.
        val symbolsToDelete = backedUpSymbols - activeSymbols

        if (symbolsToDelete.isNotEmpty()) {
            log.warn("🗑️ Provider에 존재하지 않는 코인의 RDB 데이터를 삭제합니다: {}", symbolsToDelete)
            // 4. 삭제 대상 심볼에 해당하는 모든 과거 데이터를 RDB에서 삭제합니다.
            exchangeRepository.deleteBySymbolIn(symbolsToDelete.toList())
            log.info("✅ 총 {}개 코인의 과거 데이터를 RDB에서 성공적으로 삭제했습니다.", symbolsToDelete.size)
        } else {
            log.info("ℹ️ RDB와 Provider의 코인 목록이 이미 동기화되어 있습니다.")
        }
    }

    private fun convertToExchangeEntity(candle: Candle, market: String): Exchange {
        return Exchange(
            symbol = market,
            candleTime = LocalDateTime.parse(candle.candleDateTimeKst),
            open = candle.openingPrice,
            high = candle.highPrice,
            low = candle.lowPrice,
            close = candle.tradePrice,
            volume = candle.candleAccTradeVolume,
            timestamp = candle.timestamp
        )
    }
}