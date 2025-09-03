package com.back.back9.global.websocket.notification.service

import com.back.back9.domain.orders.orders.dto.OrderNotification
import com.back.back9.domain.orders.orders.entity.Orders
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
@Service
class NotificationService(private val messagingTemplate: SimpMessagingTemplate) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)
    fun sendOrderNotification(userId: Long, order: Orders) {
        val notification = OrderNotification(
            orderId = order.id!!,
            status = order.ordersStatus!!,
            message = "주문 #${order.id} 상태: ${order.ordersStatus}"
        )
        log.info("✅ 주문 #${order.id} 상태: ${order.ordersStatus}")

        // 사용자별 토픽으로 전송
        messagingTemplate.convertAndSend("/topic/orders.$userId", notification)
    }
}