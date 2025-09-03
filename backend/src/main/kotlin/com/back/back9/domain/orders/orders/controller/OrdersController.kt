package com.back.back9.domain.orders.orders.controller

import com.back.back9.domain.orders.orders.dto.OrderResponse
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.dto.OrdersSearchRequest
import com.back.back9.domain.orders.orders.service.OrdersFacade
import com.back.back9.domain.orders.orders.service.OrdersService
import com.back.back9.domain.orders.trigger.service.TriggerService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import java.time.LocalTime

@RestController
@RequestMapping("/api/orders")
@Validated
class OrdersController(
    private val orderFacade: OrdersFacade,
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
    ): ResponseEntity<OrderResponse> {
        val response = orderFacade.placeOrder(walletId, request)
        return ResponseEntity.ok(response)
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
