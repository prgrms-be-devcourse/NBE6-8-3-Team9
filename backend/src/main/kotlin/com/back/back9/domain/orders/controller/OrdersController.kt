package com.back.back9.domain.orders.controller

import com.back.back9.domain.orders.dto.OrderResponse
import com.back.back9.domain.orders.dto.OrdersRequest
import com.back.back9.domain.orders.service.OrdersService
import lombok.extern.slf4j.Slf4j
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
@Validated
@Slf4j
class OrdersController(private val ordersService: OrdersService) {
    @GetMapping("/wallet/{wallet_id}")
    fun getOrders(@PathVariable wallet_id: Long?): ResponseEntity<List<OrderResponse?>?> {
        return ResponseEntity.ok<List<OrderResponse?>?>(ordersService.getOrdersByWalletId(wallet_id))
    }

    @PostMapping("/wallet/{wallet_id}")
    fun executeTrade(
        @PathVariable("wallet_id") walletId: Long,
        @RequestBody ordersRequest: OrdersRequest
    ): ResponseEntity<OrderResponse?> {
        val response = ordersService.executeTrade(walletId, ordersRequest)
        return ResponseEntity.ok<OrderResponse?>(response)
    }
}
