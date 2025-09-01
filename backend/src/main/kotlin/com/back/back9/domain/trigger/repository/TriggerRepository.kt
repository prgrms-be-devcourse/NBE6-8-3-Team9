package com.back.back9.domain.trigger.repository

import com.back.back9.domain.trigger.entity.Trigger
import com.back.back9.domain.trigger.entity.TriggerStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*


interface TriggerRepository : JpaRepository<Trigger, UUID> {

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
}