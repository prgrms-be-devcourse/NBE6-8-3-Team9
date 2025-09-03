package com.back.back9.domain.orders.orders.dto

import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.orders.orders.entity.OrdersStatus
import com.back.back9.domain.tradeLog.entity.TradeType
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class OrdersSearchRequest(
    val coinSymbol: String?,  // 코인 심볼 (선택)
    val tradeType: TradeType?,  // BUY, SELL (선택)
    val orderMethod: OrdersMethod?,  // LIMIT, MARKET (선택)
    val orderStatus: OrdersStatus?, // PENDING, COMPLETED, CANCELED (선택)

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @param:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val startDate: LocalDate?,  // 시작일

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @param:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val endDate: LocalDate?     // 종료일
)