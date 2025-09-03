package com.back.back9.domain.websocket.service

import com.back.back9.domain.coin.service.CoinDataChangedEvent
import com.back.back9.domain.coin.service.CoinService
import com.back.back9.domain.websocket.vo.CandleInterval
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
) {
    private val logger = KotlinLogging.logger {}
    @Volatile private var symbolToNameMap: Map<String, String> = mapOf()
    @Volatile private var nameToSymbolMap: Map<String, String> = mapOf()

    @PostConstruct
    fun initialize() {
        refreshCache(isInitialLoad = true)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinDataChanged(event: CoinDataChangedEvent) {
        logger.info { "코인 데이터 변경 이벤트 수신, 캐시 및 Redis 데이터를 동기화합니다. [이벤트: $event]" }
        refreshCache(isInitialLoad = false)
    }

    // [수정] 테스트 코드에서 접근할 수 있도록 private 키워드를 제거합니다.
    fun refreshCache(isInitialLoad: Boolean) {
        val oldSymbols = symbolToNameMap.keys

        val allCoins = coinService.findAll()
        this.symbolToNameMap = allCoins
            .mapNotNull { coin -> coin.koreanName?.let { coin.symbol to it } }
            .toMap()
        this.nameToSymbolMap = allCoins
            .mapNotNull { coin -> coin.koreanName?.let { it to coin.symbol } }
            .toMap()

        logger.info("코인 목록 캐시를 갱신했습니다. 현재 추적 중인 코인: ${allCoins.size}개")

        if (isInitialLoad) return

        val currentSymbols = symbolToNameMap.keys

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
    }

    fun getMarketCodes(): List<String> = symbolToNameMap.keys.toList()

    fun getNameBySymbol(symbol: String): String? = symbolToNameMap[symbol]
}
