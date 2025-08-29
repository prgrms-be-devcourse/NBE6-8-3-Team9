package com.back.back9.domain.wallet.repository

import com.back.back9.domain.user.entity.User
import com.back.back9.domain.wallet.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletRepository : JpaRepository<Wallet, Long> {
    fun existsByUserId(userId: Long): Boolean
    fun findByUser(user: User): Wallet?
    fun findByUserId(userId: Long): Wallet?
}
