package com.back.back9.domain.orders.orders.service

import com.back.back9.domain.orders.orders.dto.OrderResponse
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.orders.entity.OrdersMethod.*
import com.back.back9.domain.orders.price.fetcher.ExchangePriceFetcher
import com.back.back9.domain.orders.trigger.service.TriggerService
import com.back.back9.domain.orders.trigger.support.RedisKeys
import com.back.back9.domain.tradeLog.entity.TradeType
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate

@Service
class OrdersFacade(
    private val ordersService: OrdersService,
    private val triggerService: TriggerService,
    private val redis: StringRedisTemplate,
    private val exchangePriceFetcher: ExchangePriceFetcher
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun getOrders(walletId: Long): List<OrderResponse?> =
        ordersService.getOrdersByWalletId(walletId)

    fun cancelOrder(orderId: Long): OrderResponse {
        return ordersService.cancelOrder(orderId)
    }

    fun placeOrder(walletId: Long, request: OrdersRequest): OrderResponse {
        return when (request.ordersMethod) {
            MARKET -> {
                log.info("placeOrder 실행 → MARKET 주문 처리 (walletId=$walletId, coin=${request.coinSymbol})")
                val order = ordersService.executeOrder(walletId, request)
                OrderResponse.from(order)
            }

            LIMIT -> {
                val latestKey = RedisKeys.latestPrice(request.coinSymbol)
                val latestStr = redis.opsForValue().get(latestKey)
                val latestPrice = latestStr?.toBigDecimalOrNull()

                if (latestPrice != null) {
                    // BUY: 현재가 <= 예약가 → 바로 체결
                    if (request.tradeType == TradeType.BUY && latestPrice <= request.price) {
                        val order = ordersService.executeOrder(walletId, request)
                        return OrderResponse.from(order)
                    }
                    // SELL: 현재가 >= 예약가 → 바로 체결
                    if (request.tradeType == TradeType.SELL && latestPrice >= request.price) {
                        val order = ordersService.executeOrder(walletId, request)
                        return OrderResponse.from(order)
                    }
                }
                //1.주문 생성
                val order: Orders = ordersService.createOrder(walletId, request)
                //2. 트리거 생성
                triggerService.registerFromOrder(walletId, order)
                exchangePriceFetcher.addMonitoring(request.coinSymbol)
                return OrderResponse.from(order)
            }

        }
    }
}