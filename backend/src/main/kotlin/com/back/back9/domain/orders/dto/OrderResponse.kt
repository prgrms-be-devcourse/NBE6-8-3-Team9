package com.back.back9.domain.orders.dto

import com.back.back9.domain.orders.entity.Orders
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
    val createdAt: String?
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
            createdAt = order.createdAt?.toString()
        )
    }
}
