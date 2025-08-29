package com.back.back9.domain.orders.dto

import com.back.back9.domain.orders.entity.Orders
import java.math.BigDecimal

@JvmRecord
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
        fun from(order: Orders): OrderResponse {
            val coin = order.getCoin()
            return OrderResponse(
                coin.getId(),
                coin.symbol,
                coin.koreanName,
                order.getOrdersMethod().name,
                order.getOrdersStatus().name,
                order.getTradeType().name,
                order.getPrice(),
                order.getQuantity(),
                order.getCreatedAt().toString()
            )
        }
    }
}
