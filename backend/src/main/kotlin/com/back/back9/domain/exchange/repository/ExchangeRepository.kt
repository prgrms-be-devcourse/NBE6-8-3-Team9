package com.back.back9.domain.exchange.repository

import com.back.back9.domain.exchange.entity.Exchange
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExchangeRepository : JpaRepository<Exchange, Long> {

    /** RDB의 Exchange 테이블에 저장된 모든 코인 심볼을 중복 없이 조회합니다. */
    @Query("SELECT DISTINCT e.symbol FROM Exchange e")
    fun findDistinctSymbols(): List<String>

    /** 주어진 심볼 리스트에 해당하는 모든 Exchange 데이터를 삭제합니다. */
    @Modifying
    @Query("DELETE FROM Exchange e WHERE e.symbol IN :symbols")
    fun deleteBySymbolIn(@Param("symbols") symbols: List<String>)
}