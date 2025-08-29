package com.back.back9.domain.wallet.entity

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.common.vo.money.MoneyConverter
import com.back.back9.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "coin_amount")
open class CoinAmount() : BaseEntity() {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    lateinit var wallet: Wallet

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_id")
    lateinit var coin: Coin

    @NotNull
    @Column(name = "quantity", precision = 19, scale = 8)
    lateinit var quantity: BigDecimal

    @NotNull
    @Column(name = "total_amount", precision = 19, scale = 8)
    @Convert(converter = MoneyConverter::class)
    lateinit var totalAmount: Money

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null

    constructor(
        wallet: Wallet,
        coin: Coin,
        quantity: BigDecimal,
        totalAmount: Money,
        updatedAt: OffsetDateTime?
    ) : this() {
        this.wallet = wallet
        this.coin = coin
        this.quantity = quantity
        this.totalAmount = totalAmount
        this.updatedAt = updatedAt
    }

    fun updateQuantityAndAmount(newQuantity: BigDecimal, newTotalAmount: Money) {
        this.quantity = newQuantity
        this.totalAmount = newTotalAmount
        this.updatedAt = OffsetDateTime.now()
    }

    fun addQuantityAndAmount(additionalQuantity: BigDecimal, additionalAmount: Money) {
        this.quantity = this.quantity.add(additionalQuantity)
        this.totalAmount = this.totalAmount.add(additionalAmount)
        this.updatedAt = OffsetDateTime.now()
    }

    fun subtractQuantityAndAmount(subtractQuantity: BigDecimal, subtractAmount: Money) {
        this.quantity = this.quantity.subtract(subtractQuantity)
        this.totalAmount = this.totalAmount.subtract(subtractAmount)
        this.updatedAt = OffsetDateTime.now()
    }

    fun getAverageBuyPrice(): Money {
        if (quantity.compareTo(BigDecimal.ZERO) == 0) return Money.zero()
        return totalAmount.divide(quantity)
    }

    @Deprecated("Use updateQuantityAndAmount(newQuantity, newTotalAmount) instead.")
    fun updateAmount(newAmount: BigDecimal) {
        this.totalAmount = Money.of(newAmount)
        this.updatedAt = OffsetDateTime.now()
    }

    @Deprecated("Use addQuantityAndAmount(additionalQuantity, additionalAmount) instead.")
    fun addAmount(additionalAmount: BigDecimal) {
        this.totalAmount = this.totalAmount.add(Money.of(additionalAmount))
        this.updatedAt = OffsetDateTime.now()
    }

    @Deprecated("Use subtractQuantityAndAmount(subtractQuantity, subtractAmount) instead.")
    fun subtractAmount(subtractAmount: BigDecimal) {
        this.totalAmount = this.totalAmount.subtract(Money.of(subtractAmount))
        this.updatedAt = OffsetDateTime.now()
    }

    companion object {
        @JvmStatic
        fun builder(): CoinAmountBuilder = CoinAmountBuilder()
    }

    class CoinAmountBuilder {
        private var wallet: Wallet? = null
        private var coin: Coin? = null
        private var quantity: BigDecimal? = null
        private var totalAmount: Money? = null
        private var updatedAt: OffsetDateTime? = null

        fun wallet(wallet: Wallet) = apply { this.wallet = wallet }
        fun coin(coin: Coin) = apply { this.coin = coin }
        fun quantity(quantity: BigDecimal) = apply { this.quantity = quantity }
        fun totalAmount(totalAmount: Money) = apply { this.totalAmount = totalAmount }
        fun updatedAt(updatedAt: OffsetDateTime?) = apply { this.updatedAt = updatedAt }

        fun build(): CoinAmount =
            CoinAmount(
                wallet = wallet ?: throw IllegalStateException("wallet is required"),
                coin = coin ?: throw IllegalStateException("coin is required"),
                quantity = quantity ?: BigDecimal.ZERO,
                totalAmount = totalAmount ?: Money.zero(),
                updatedAt = updatedAt
            )

        override fun toString(): String =
            "CoinAmount.CoinAmountBuilder(wallet=$wallet, coin=$coin, quantity=$quantity, totalAmount=$totalAmount, updatedAt=$updatedAt)"
    }
}
