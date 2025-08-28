package com.back.back9.domain.coin.repository

import com.back.back9.domain.coin.entity.Coin
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CoinRepository : JpaRepository<Coin, Long> {
    fun findFirstByOrderByIdDesc(): Coin?

    fun findBySymbol(symbol: String): Optional<Coin>
}
