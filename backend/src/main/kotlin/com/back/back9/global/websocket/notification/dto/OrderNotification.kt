package com.back.back9.global.websocket.notification.dto

import com.back.back9.domain.orders.orders.entity.OrdersStatus

data class OrderNotification(
    val orderId: Long,
    val status: OrdersStatus,
    val message: String
)
