package com.back.back9.domain.orders.orders.controller

import com.back.back9.domain.orders.orders.dto.OrderResponse
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.dto.OrdersSearchRequest
import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.orders.orders.service.OrdersService
import com.back.back9.domain.orders.price.fetcher.ExchangePriceFetcher
import com.back.back9.domain.orders.trigger.service.TriggerService
import com.back.back9.domain.orders.trigger.support.RedisKeys
import com.back.back9.domain.tradeLog.entity.TradeType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.LocalTime

@RestController
@RequestMapping("/api/orders")
@Validated
class OrdersController(
    private val ordersService: OrdersService,
    private val redis: StringRedisTemplate,
    private val exchangePriceFetcher: ExchangePriceFetcher,
    private val triggerService: TriggerService,
    private val orderService : OrdersService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @GetMapping("/wallet/{walletId}")
    fun getOrders(
        @PathVariable walletId: Long,
        @ModelAttribute request: OrdersSearchRequest,
        pageable: Pageable
    ): ResponseEntity<List<OrderResponse>> {
        val startDateTime = request.startDate?.atStartOfDay()
        val endDateTime = request.endDate?.atTime(LocalTime.MAX)

        val items = orderService.getOrdersByFilter(
            walletId = walletId,
            coinSymbol = request.coinSymbol,
            tradeType = request.tradeType,
            orderMethod = request.orderMethod,
            orderStatus = request.orderStatus,
            startDate = startDateTime,
            endDate = endDateTime,
            pageable = pageable
        )

        val result = items.filterNotNull().map { it }

        log.info(
            "주문 조회 - 지갑 ID: {}, 거래 유형: {}, 주문 방식: {}, 코인 심볼: {}, 시작일: {}, 종료일: {}, 페이지: {}",
            walletId, request.tradeType, request.orderMethod, request.coinSymbol, request.startDate, request.endDate, pageable.pageNumber
        )

        return ResponseEntity.ok(result)
    }

    @PostMapping("/wallet/{walletId}")
    fun placeOrder(
        @PathVariable walletId: Long,
        @RequestBody request: OrdersRequest
    ): OrderResponse {
    return when (request.ordersMethod) {
            OrdersMethod.MARKET -> {
                log.info("placeOrder 실행 → MARKET 주문 처리 (walletId=$walletId, coin=${request.coinSymbol})")
                val order = ordersService.executeOrder(walletId, request)
                OrderResponse.from(order)
            }

            OrdersMethod.LIMIT -> {
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

    @PostMapping("/cancel")
    fun cancelOrders(@RequestBody request: CancelOrdersRequest): ResponseEntity<Map<String, Any>> {
        val results = orderService.cancelOrders(request.orderIds)

        return ResponseEntity.ok(
            mapOf(
                "isSuccess" to true,
                "message" to "${results.size}건의 주문이 취소되었습니다.",
                "canceledOrders" to results
            )
        )
    }

    data class CancelOrdersRequest(
        val orderIds: List<Long>
    )

    @PostMapping("/price-tick")
    fun priceTick(
        @RequestParam symbol: String,
        @RequestParam price: BigDecimal
    ): ResponseEntity<String> {
        triggerService.onPriceTick(symbol, price)
        return ResponseEntity.ok("Tick processed: $symbol -> $price")
    }
    @PostMapping("/mock")
    fun createMockOrders(): ResponseEntity<String> {
        orderService.createMockOrders()
        return ResponseEntity.ok("Mock trade logs created.")
    }

}
