package com.back.back9.domain.orders.orders.repository

import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.orders.orders.entity.OrdersStatus
import com.back.back9.domain.tradeLog.entity.TradeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface OrdersRepository : JpaRepository<Orders, Long?> {
    fun findByWalletId(walletId: Long?): MutableList<Orders>
    fun findById(id: Long?): Orders?
    @Query("""
SELECT o FROM Orders o
WHERE o.wallet.id = :walletId
  AND (:coinSymbol IS NULL OR o.coin.symbol = :coinSymbol)
  AND (:tradeType  IS NULL OR o.tradeType    = :tradeType)
  AND (:orderMethod IS NULL OR o.ordersMethod = :orderMethod)
  AND (:orderStatus IS NULL OR o.ordersStatus = :orderStatus)
  AND o.createdAt >= COALESCE(:startDate, o.createdAt)
  AND o.createdAt <= COALESCE(:endDate,   o.createdAt)
""")
    fun findByOrdersFilter(

        @Param("walletId") walletId: Long,
        @Param("coinSymbol") coinSymbol: String?,
        @Param("tradeType") tradeType: TradeType?,
        @Param("orderMethod") orderMethod: OrdersMethod?,
        @Param("orderStatus") orderStatus: OrdersStatus?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        pageable: Pageable
    ): Page<Orders>


}
