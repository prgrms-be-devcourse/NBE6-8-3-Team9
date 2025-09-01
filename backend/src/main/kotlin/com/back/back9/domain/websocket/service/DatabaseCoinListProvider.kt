package com.back.back9.domain.websocket.service

import com.back.back9.domain.coin.service.CoinDataChangedEvent
import com.back.back9.domain.coin.service.CoinService
import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.initializer.CandleDataInitializerService // 서비스 주입
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DatabaseCoinListProvider(
    private val coinService: CoinService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val candleDataInitializerService: CandleDataInitializerService // 서비스 주입
) {
    private val logger = KotlinLogging.logger {}
    private var symbolToNameMap: Map<String, String> = mapOf()
    private var nameToSymbolMap: Map<String, String> = mapOf()

    @PostConstruct
    fun initialize() {
        refreshCache()
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinDataChanged(event: CoinDataChangedEvent) {
        logger.info { "코인 데이터 변경 이벤트 수신, 캐시 및 Redis 데이터를 동기화합니다. [이벤트: $event]" }
        refreshCache()
    }

    fun refreshCache() {
        val oldSymbols = symbolToNameMap.keys

        val allCoins = coinService.findAll()
        this.symbolToNameMap = allCoins
            .mapNotNull { coin -> coin.koreanName?.let { coin.symbol to it } }
            .toMap()
        this.nameToSymbolMap = allCoins
            .mapNotNull { coin -> coin.koreanName?.let { it to coin.symbol } }
            .toMap()

        val currentSymbols = symbolToNameMap.keys

        // --- 핵심 로직 추가 시작 ---

        // 1. 새로 추가된 코인 식별
        val newSymbols = currentSymbols - oldSymbols
        if (newSymbols.isNotEmpty()) {
            logger.info { "새로운 코인 감지, 초기 캔들 데이터 저장을 시작합니다: $newSymbols" }
            // 2. 새로 추가된 코인에 대해서만 초기화 서비스 호출
            candleDataInitializerService.initializeCandlesForMarkets(newSymbols.toList())
        }

        // 3. 삭제된 코인 처리 (기존 로직)
        val deletedSymbols = oldSymbols - currentSymbols
        if (deletedSymbols.isNotEmpty()) {
            logger.info { "삭제된 코인 감지, 관련 Redis 데이터를 삭제합니다: $deletedSymbols" }
            val keysToDelete = deletedSymbols.flatMap { symbol ->
                CandleInterval.entries.map { it.redisKey(symbol) }
            }
            if (keysToDelete.isNotEmpty()) {
                redisTemplate.delete(keysToDelete)
                logger.info { "총 ${keysToDelete.size}개의 Redis 키를 삭제했습니다." }
            }
        }
        // --- 핵심 로직 추가 끝 ---
    }

    fun getMarketCodes(): List<String> = symbolToNameMap.keys.toList()

    fun getNameBySymbol(symbol: String): String? = symbolToNameMap[symbol]
}
