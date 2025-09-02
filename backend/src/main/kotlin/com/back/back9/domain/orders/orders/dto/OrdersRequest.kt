package com.back.back9.domain.orders.orders.dto

import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.tradeLog.entity.TradeType
import java.math.BigDecimal

//프론트 요청 DTO
data class OrdersRequest(
    @JvmField val coinSymbol: String,  // 코인 심볼
    @JvmField val tradeType: TradeType,  // BUY, SELL
    @JvmField val ordersMethod: OrdersMethod,  // LIMIT, MARKET
    @JvmField val quantity: BigDecimal,  //수량
    @JvmField val price: BigDecimal //단가
)
