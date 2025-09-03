package com.back.back9.domain.orders.orders.dto

import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.trigger.entity.Trigger
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

data class OrderResponse(
    val id: Long,
    val coinId: Long?,
    val coinSymbol: String?,
    val coinName: String?,
    val orderMethod: String?,
    val orderStatus: String?,
    val tradeType: String?,
    val price: BigDecimal?,
    val quantity: BigDecimal?,
    val createDate: String?,
    val updateDate: String?,
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")

        fun from(order: Orders): OrderResponse = OrderResponse(
            id = order.id!!,
            coinId = order.coin?.id,
            coinSymbol = order.coin?.symbol,
            coinName = order.coin?.koreanName,
            orderMethod = order.ordersMethod?.name,
            orderStatus = order.ordersStatus?.name,
            tradeType = order.tradeType?.name,
            price = order.price,
            quantity = order.quantity,
            createDate = order.createdAt?.format(formatter),
            updateDate = order.modifiedAt?.format(formatter),
        )

        fun fromLimit(trigger: Trigger): OrderResponse = OrderResponse(
            id = trigger.id!!,
            coinId = trigger.coin?.id,
            coinSymbol = trigger.coin?.symbol,
            coinName = trigger.coin?.koreanName,
            orderMethod = trigger.ordersMethod?.name,
            orderStatus = trigger.status.name,
            tradeType = trigger.tradeType?.name,
            price = trigger.executePrice,
            quantity = trigger.quantity,
            createDate = trigger.createdAt?.format(formatter),
            updateDate = trigger.modifiedAt?.format(formatter),
        )
    }
}
