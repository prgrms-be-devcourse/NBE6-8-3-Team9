package com.back.back9.websocket.service

import com.back.back9.domain.coin.service.CoinDataChangedEvent
import com.back.back9.domain.coin.service.CoinService
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import mu.KotlinLogging

// DB에서 실제 코인 정보를 조회하고 캐싱하여 제공하는 Provider.

@Component
class DatabaseCoinListProvider(
    private val coinService: CoinService
) {
    private val logger = KotlinLogging.logger {}
    private lateinit var symbolToNameMap: Map<String, String>
    private lateinit var nameToSymbolMap: Map<String, String>

    @PostConstruct
    fun initialize() {
        refreshCache()
    }

    // DB 트랜잭션이 성공적으로 '커밋'된 후에만 이 메서드를 실행합니다.

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinDataChanged(event: CoinDataChangedEvent) {
        logger.info { "코인 데이터 변경 이벤트 수신, 캐시를 새로고침합니다. [이벤트: $event]" }
        refreshCache()
    }

    fun refreshCache() {
        val allCoins = coinService.findAll()
        this.symbolToNameMap = allCoins
            .mapNotNull { coin -> coin.koreanName?.let { coin.symbol to it } }
            .toMap()
        this.nameToSymbolMap = allCoins
            .mapNotNull { coin -> coin.koreanName?.let { it to coin.symbol } }
            .toMap()
    }

    fun getMarketCodes(): List<String> = symbolToNameMap.keys.toList()

    fun getNameBySymbol(symbol: String): String? = symbolToNameMap[symbol]
}
