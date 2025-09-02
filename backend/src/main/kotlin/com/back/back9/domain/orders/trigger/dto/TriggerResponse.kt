package com.back.back9.domain.orders.trigger.dto

import com.back.back9.domain.orders.trigger.entity.Trigger
import java.math.BigDecimal

data class TriggerResponse(
    val id: Long?,
    val coinSymbol: String?,
    val direction: String?,        // nullable
    val threshold: BigDecimal?,
    val quantity: BigDecimal?,
    val executePrice: BigDecimal?,
    val status: String,
    val createdAt: String?
) {
    companion object {
        fun from(trigger: Trigger): TriggerResponse =
            TriggerResponse(
                id = trigger.id,
                coinSymbol = trigger.coin?.symbol,
                direction = trigger.direction?.name,   // OK
                threshold = trigger.threshold,
                quantity = trigger.quantity,
                executePrice = trigger.executePrice,
                status = trigger.status.name,
                createdAt = trigger.createdAt?.toString()
            )
    }
}