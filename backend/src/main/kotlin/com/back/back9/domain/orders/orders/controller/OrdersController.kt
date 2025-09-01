package com.back.back9.domain.orders.orders.controller

import com.back.back9.domain.orders.orders.dto.OrderResponse
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.service.OrdersFacade
import com.back.back9.domain.orders.trigger.service.TriggerService
import lombok.extern.slf4j.Slf4j
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import org.slf4j.LoggerFactory
@RestController
@RequestMapping("/api/orders")
@Validated
@Slf4j
class OrdersController(private val orderFacade: OrdersFacade, private val triggerService: TriggerService) {
    private val log = LoggerFactory.getLogger(javaClass)
    @GetMapping("/wallet/{walletId}")
    fun getOrders(@PathVariable walletId: Long): ResponseEntity<List<OrderResponse?>?> {
        return ResponseEntity.ok(orderFacade.getOrders(walletId))
    }

    @PostMapping("/wallet/{walletId}")
    fun placeOrder(
        @PathVariable walletId: Long,
        @RequestBody request: OrdersRequest
    ): ResponseEntity<OrderResponse> {
        val response = orderFacade.placeOrder(walletId, request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/price-tick")
    fun priceTick(
        @RequestParam symbol: String,
        @RequestParam price: BigDecimal
    ): ResponseEntity<String> {
        triggerService.onPriceTick(symbol, price)
        return ResponseEntity.ok("Tick processed: $symbol -> $price")
    }

}
