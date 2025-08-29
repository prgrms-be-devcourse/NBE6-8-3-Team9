package com.back.back9.domain.orders.dto

import com.back.back9.domain.orders.entity.OrdersMethod
import com.back.back9.domain.tradeLog.entity.TradeType
import java.math.BigDecimal

//프론트 요청 DTO
@JvmRecord
data class OrdersRequest(
    val coinSymbol: String?,  // 코인 심볼
    val tradeType: TradeType?,  // BUY, SELL
    val ordersMethod: OrdersMethod?,  // LIMIT, MARKET
    val quantity: BigDecimal?,  //수량
    val price: BigDecimal? //단가
)
