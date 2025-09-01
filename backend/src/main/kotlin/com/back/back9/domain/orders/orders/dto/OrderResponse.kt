package com.back.back9.domain.orders.orders.dto

import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.trigger.entity.Trigger
import java.math.BigDecimal

data class OrderResponse(
    val coinId: Long?,
    val coinSymbol: String?,
    val coinName: String?,
    val orderMethod: String?,
    val orderStatus: String?,
    val tradeType: String?,
    val price: BigDecimal?,
    val quantity: BigDecimal?,
) {
    companion object {
        fun from(order: Orders): OrderResponse = OrderResponse(
            coinId = order.coin?.id,
            coinSymbol = order.coin?.symbol,
            coinName = order.coin?.koreanName,
            orderMethod = order.ordersMethod?.name,
            orderStatus = order.ordersStatus?.name,
            tradeType = order.tradeType?.name,
            price = order.price,
            quantity = order.quantity,
        )
        fun fromLimit(trigger: Trigger): OrderResponse = OrderResponse(
            coinId = trigger.coin?.id,
            coinSymbol = trigger.coin?.symbol,
            coinName = trigger.coin?.koreanName,
            orderMethod = trigger.ordersMethod?.name,
            orderStatus = trigger.status.name,
            tradeType = trigger.tradeType?.name,
            price = trigger.executePrice,
            quantity = trigger.quantity,
        )
    }
}
