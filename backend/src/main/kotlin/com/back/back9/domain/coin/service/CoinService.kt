package com.back.back9.domain.coin.service

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.global.error.ErrorCode
import com.back.back9.global.error.ErrorException
import org.springframework.stereotype.Service

@Service
class CoinService(
    private val coinRepository: CoinRepository
) {
    // 코인 전체 조회
    fun findAll(): List<Coin> {
        return coinRepository.findAll()
    }

    // 코인 단건 조회
    fun findById(id: Long): Coin =
        coinRepository.findById(id)
            .orElseThrow { ErrorException(ErrorCode.COIN_NOT_FOUND, id) }


    // 코인 삭제
    fun delete(coin: Coin) {
        coinRepository.delete(coin)
    }

    // 코인 개수
    fun count(): Long {
        return coinRepository.count()
    }

    // 코인 추가
    fun add(symbol: String, koreanName: String, englishName: String): Coin {
        val coin: Coin = Coin(symbol,koreanName, englishName)
        return coinRepository.save(coin)
    }

    // 코인 수정
    fun modify(coin: Coin, symbol: String?, koreanName: String?, englishName: String?) {
        symbol?.let { coin.symbol = it }
        koreanName?.let { coin.koreanName = it }
        englishName?.let { coin.englishName = it }
        coinRepository.save(coin)
    }

    // 마지막 코인 조회
    fun findLastest(): Coin? {
        return coinRepository.findFirstByOrderByIdDesc()
    }

    // 심볼로 코인 조회
    fun findBySymbol(symbol: String): Coin? {
        return coinRepository.findBySymbol(symbol)
            .orElse(null)
    }
}