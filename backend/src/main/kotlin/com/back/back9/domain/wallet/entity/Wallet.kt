package com.back.back9.domain.wallet.entity

import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.common.vo.money.MoneyConverter
import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.user.entity.User
import com.back.back9.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

@Entity
@Table(name = "wallet")
open class Wallet() : BaseEntity() {

    @NotNull
    @OneToOne
    @JoinColumn(name = "user_id")
    lateinit var user: User

    @OneToMany
    @JoinColumn(name = "tradelog_id")
    var tradeLog: MutableList<TradeLog>? = null

    @OneToMany(mappedBy = "wallet", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var coinAmounts: MutableList<CoinAmount> = mutableListOf()

    @NotNull
    lateinit var address: String

    @Convert(converter = MoneyConverter::class)
    @NotNull
    var balance: Money = Money.of(500_000_000L)

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null

    constructor(
        user: User,
        tradeLog: MutableList<TradeLog>?,
        coinAmounts: MutableList<CoinAmount>,
        address: String,
        balance: Money,
        updatedAt: OffsetDateTime?
    ) : this() {
        this.user = user
        this.tradeLog = tradeLog
        this.coinAmounts = coinAmounts
        this.address = address
        this.balance = balance
        this.updatedAt = updatedAt
    }

    fun charge(amount: Money) {
        this.balance = this.balance.add(amount)
        this.updatedAt = OffsetDateTime.now()
    }

    fun deduct(amount: Money) {
        if (!this.balance.isGreaterThanOrEqual(amount)) {
            throw IllegalArgumentException("잔액이 부족합니다.")
        }
        this.balance = this.balance.subtract(amount)
        this.updatedAt = OffsetDateTime.now()
    }

    companion object {
        @JvmStatic
        fun builder(): WalletBuilder = WalletBuilder()
    }

    class WalletBuilder {
        private var user: User? = null
        private var tradeLog: MutableList<TradeLog>? = null
        private var coinAmounts: MutableList<CoinAmount> = mutableListOf()
        private var address: String? = null
        private var balance: Money = Money.of(500_000_000L)
        private var updatedAt: OffsetDateTime? = null

        fun user(user: User) = apply { this.user = user }
        fun tradeLog(tradeLog: MutableList<TradeLog>?) = apply { this.tradeLog = tradeLog }
        fun coinAmounts(coinAmounts: MutableList<CoinAmount>) = apply { this.coinAmounts = coinAmounts }
        fun address(address: String) = apply { this.address = address }
        fun balance(balance: Money) = apply { this.balance = balance }
        fun updatedAt(updatedAt: OffsetDateTime?) = apply { this.updatedAt = updatedAt }

        fun build(): Wallet =
            Wallet(
                user = user ?: throw IllegalStateException("user is required"),
                tradeLog = tradeLog,
                coinAmounts = coinAmounts,
                address = address ?: throw IllegalStateException("address is required"),
                balance = balance,
                updatedAt = updatedAt
            )

        override fun toString(): String =
            "Wallet.WalletBuilder(user=$user, tradeLog=$tradeLog, coinAmounts=$coinAmounts, address=$address, balance=$balance, updatedAt=$updatedAt)"
    }
}
