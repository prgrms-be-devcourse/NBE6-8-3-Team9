package com.back.back9.domain.tradeLog.repository

import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.tradeLog.entity.TradeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface TradeLogRepository : JpaRepository<TradeLog, Long> {

    fun findFirstByOrderByIdDesc(): Optional<TradeLog>

    fun findByWalletId(walletId: Long): MutableList<TradeLog>

    fun findByWalletId(walletId: Long, pageable: Pageable): Page<TradeLog>

    @Query(
        """
        SELECT t FROM TradeLog t
        WHERE t.wallet.id = :walletId
          AND t.type = COALESCE(:type, t.type)
          AND (:coinId IS NULL OR t.coin.id = :coinId)
          AND t.createdAt >= COALESCE(:startDate, t.createdAt)
          AND t.createdAt <= COALESCE(:endDate, t.createdAt)
        """
    )
    fun findByWalletIdFilter(
        @Param("walletId") walletId: Long,
        @Param("type") type: TradeType?,
        @Param("coinId") coinId: Int?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        pageable: Pageable?
    ): Page<TradeLog>
}

