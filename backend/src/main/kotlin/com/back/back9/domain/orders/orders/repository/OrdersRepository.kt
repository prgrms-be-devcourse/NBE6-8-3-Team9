package com.back.back9.domain.orders.orders.repository

import com.back.back9.domain.orders.orders.entity.Orders
import org.springframework.data.jpa.repository.JpaRepository

interface OrdersRepository : JpaRepository<Orders?, Long?> {
    fun findByWalletId(walletId: Long?): MutableList<Orders>
}
