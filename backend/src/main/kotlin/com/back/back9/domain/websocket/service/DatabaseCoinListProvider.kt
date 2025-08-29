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

// DB에서 실제 코인 정보를 조회하고 캐싱하여 제공하는 Provider.

@Component
class DatabaseCoinListProvider(
    private val coinService: CoinService,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val logger = KotlinLogging.logger {}
    private var symbolToNameMap: Map<String, String> = mapOf()
    private var nameToSymbolMap: Map<String, String> = mapOf()

    @PostConstruct
    fun initialize() {
        refreshCache()
    }

    // DB 트랜잭션이 성공적으로 '커밋'된 후에만 이 메서드를 실행합니다.

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
        val deletedSymbols = oldSymbols - currentSymbols

        if (deletedSymbols.isNotEmpty()) {
            logger.info { "삭제된 코인 감지, 관련 Redis 데이터를 삭제합니다: $deletedSymbols" }
            val keysToDelete = deletedSymbols.flatMap { symbol ->
                val candleKeys = CandleInterval.entries.map { it.redisKey(symbol) }
                val latestKey = "$symbol:Latest"
                candleKeys + latestKey
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
