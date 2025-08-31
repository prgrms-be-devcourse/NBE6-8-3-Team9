package com.back.back9.domain.exchange.service

import com.back.back9.domain.exchange.dto.CandleResponseDTO
import com.back.back9.domain.exchange.dto.CoinPriceResponseDTO
import com.back.back9.domain.exchange.vo.CandleFetchParameters
import com.back.back9.domain.websocket.service.DatabaseCoinListProvider
import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.dto.Candle
import com.back.back9.global.redis.service.RedisService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class ExchangeService(
    private val redisService: RedisService,
    private val coinListProvider: DatabaseCoinListProvider
) {

    fun getInitialCandles(interval: CandleInterval, symbol: String): List<CandleResponseDTO> {
        val key = interval.redisKey(symbol)
        val candles = redisService.getLatestCandles(key, CandleFetchParameters.INITIAL_CANDLE_COUNT)
        return candles.map { candle -> convertToCandleResponseDTO(candle, symbol) }
    }

    fun getPreviousCandles(interval: CandleInterval, market: String, cursorTimestamp: Long): List<CandleResponseDTO> {
        val key = interval.redisKey(market)
        val candles = redisService.getCandlesBefore(key, cursorTimestamp, CandleFetchParameters.PREVIOUS_CANDLE_COUNT)
        return candles.map { candle -> convertToCandleResponseDTO(candle, market) }
    }

    val coinsLatest: List<CandleResponseDTO>
        get() {
            return coinListProvider.getMarketCodes().mapNotNull { coinSymbol ->
                val key = CandleInterval.SEC.redisKey(coinSymbol)
                redisService.getLatestCandle(key)?.let { candle ->
                    val dto = convertToCandleResponseDTO(candle, coinSymbol)
                    // `apply` 블록 안에서는 `dto` 객체가 컨텍스트(this)가 되므로,
                    // 프로퍼티에 바로 접근할 수 있습니다.
                    dto.apply {
                        // [수정] CandleResponseDTO.setName이 아니라 인스턴스의 name 프로퍼티에 값을 할당합니다.
                        name = coinListProvider.getNameBySymbol(coinSymbol)
                    }
                }
            }
        }

    fun getLatestCandleByScan(coinName: String): CoinPriceResponseDTO {
        val key = CandleInterval.SEC.redisKey(coinName)
        return redisService.getLatestCandle(key)?.let { candle ->
            CoinPriceResponseDTO(
                coinName,
                candle.tradePrice,
                LocalDateTime.now().withNano(0)
            )
        } ?: CoinPriceResponseDTO(coinName, BigDecimal.ZERO, LocalDateTime.now())
    }

    private fun convertToCandleResponseDTO(candle: Candle, symbol: String): CandleResponseDTO {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(candle.timestamp), ZoneId.systemDefault())
        return CandleResponseDTO(
            candle.timestamp, dateTime, symbol,
            candle.openingPrice, candle.highPrice, candle.lowPrice,
            candle.tradePrice, candle.candleAccTradeVolume
        )
    }
}