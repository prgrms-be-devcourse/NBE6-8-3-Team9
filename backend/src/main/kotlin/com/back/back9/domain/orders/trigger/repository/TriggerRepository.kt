package com.back.back9.domain.orders.trigger.repository

import com.back.back9.domain.orders.trigger.entity.Trigger
import com.back.back9.domain.orders.trigger.entity.TriggerStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime


interface TriggerRepository : JpaRepository<Trigger, Long> {

    @Query("""
    SELECT t
      FROM Trigger t
     WHERE t.status = :status
       AND (t.expiresAt IS NULL OR t.expiresAt > :now)
""")
    fun findAllPending(
        @Param("status") status: TriggerStatus,
        @Param("now") now: LocalDateTime
    ): List<Trigger>

    fun existsByCoinSymbolAndStatus(symbol: String, status: TriggerStatus): Boolean

}