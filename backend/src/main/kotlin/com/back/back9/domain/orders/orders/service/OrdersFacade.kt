package com.back.back9.domain.orders.orders.service

import com.back.back9.domain.orders.orders.dto.OrderResponse
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.entity.OrdersMethod.*
import com.back.back9.domain.orders.price.fetcher.ExchangePriceFetcher
import com.back.back9.domain.orders.trigger.service.TriggerService
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class OrdersFacade(
    private val ordersService: OrdersService,
    private val triggerService: TriggerService,
    private val exchangePriceFetcher: ExchangePriceFetcher
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun getOrders(walletId: Long): List<OrderResponse?> =
        ordersService.getOrdersByWalletId(walletId)

    fun placeOrder(walletId: Long, request: OrdersRequest): OrderResponse {
        return when (request.ordersMethod) {
            MARKET -> {
                log.info("placeOrder 실행 → MARKET 주문 처리 (walletId=$walletId, coin=${request.coinSymbol})")
                val response = triggerService.registerFromOrder(walletId, request)

                ordersService.executeMarketOrder(walletId, request)
            }
            LIMIT -> {
                log.info("placeOrder 실행 → LIMIT 주문 처리 (walletId=$walletId, coin=${request.coinSymbol})")
                val response = triggerService.registerFromOrder(walletId, request)
                // 모니터링 등록
                exchangePriceFetcher.addMonitoring(request.coinSymbol)

                response
            }
            null -> {
                log.warn("placeOrder 실행 → ordersMethod가 null (walletId=$walletId)")
                TODO()
            }
        }
    }
}