package com.back.back9.domain.trigger.dto

import com.back.back9.domain.orders.entity.OrdersMethod
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.trigger.entity.Direction
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 예약 생성 입력값.
 * - executePrice: 발화 시 주문에 기록할 가격(프론트 기준가)
 */
data class PriceTriggerCreateRequest(
    val walletId: Long,
    val coinId: Long,
    val direction: Direction,
    val threshold: BigDecimal,
    val tradeType: TradeType,
    val ordersMethod: OrdersMethod,
    val quantity: BigDecimal,
    val executePrice: BigDecimal,
    val expiresAt: LocalDateTime? = null
)