package com.back.back9.domain.orders.entity

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class Orders : BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: @NotNull User? = null
        private set

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    var wallet: @NotNull Wallet? = null
        private set

    @ManyToOne
    @JoinColumn(name = "coin_id")
    var coin: @NotNull Coin? = null
        private set

    @Enumerated(EnumType.STRING)
    var tradeType: TradeType? = null // BUY, SELL
        private set

    @Enumerated(EnumType.STRING)
    var ordersMethod: OrdersMethod? = null // LIMIT, MARKET
        private set

    var quantity: BigDecimal? = null // 수량
        private set

    var price: BigDecimal? = null // 지정가 주문 시 사용
        private set

    private var createdAt: LocalDateTime? = null

    @Enumerated(EnumType.STRING)
    var ordersStatus: OrdersStatus? = null // PENDING, FILLED, CANCELLED 등
        private set

    protected constructor()

    constructor(
        user: User?,
        wallet: Wallet?,
        coin: Coin?,
        tradeType: TradeType?,
        ordersMethod: OrdersMethod?,
        quantity: BigDecimal?,
        price: BigDecimal?,
        createdAt: LocalDateTime?,
        ordersStatus: OrdersStatus?
    ) {
        this.user = user
        this.wallet = wallet
        this.coin = coin
        this.tradeType = tradeType
        this.ordersMethod = ordersMethod
        this.quantity = quantity
        this.price = price
        this.createdAt = createdAt
        this.ordersStatus = ordersStatus
    }

    override fun getCreatedAt(): LocalDateTime? {
        return createdAt
    }

    class OrdersBuilder internal constructor() {
        private var user: User? = null
        private var wallet: Wallet? = null
        private var coin: Coin? = null
        private var tradeType: TradeType? = null
        private var ordersMethod: OrdersMethod? = null
        private var quantity: BigDecimal? = null
        private var price: BigDecimal? = null
        private var createdAt: LocalDateTime? = null
        private var ordersStatus: OrdersStatus? = null

        fun user(user: User?): OrdersBuilder {
            this.user = user
            return this
        }

        fun wallet(wallet: Wallet?): OrdersBuilder {
            this.wallet = wallet
            return this
        }

        fun coin(coin: Coin?): OrdersBuilder {
            this.coin = coin
            return this
        }

        fun tradeType(tradeType: TradeType?): OrdersBuilder {
            this.tradeType = tradeType
            return this
        }

        fun ordersMethod(ordersMethod: OrdersMethod?): OrdersBuilder {
            this.ordersMethod = ordersMethod
            return this
        }

        fun quantity(quantity: BigDecimal?): OrdersBuilder {
            this.quantity = quantity
            return this
        }

        fun price(price: BigDecimal?): OrdersBuilder {
            this.price = price
            return this
        }

        fun createdAt(createdAt: LocalDateTime?): OrdersBuilder {
            this.createdAt = createdAt
            return this
        }

        fun ordersStatus(ordersStatus: OrdersStatus?): OrdersBuilder {
            this.ordersStatus = ordersStatus
            return this
        }

        fun build(): Orders {
            return Orders(user, wallet, coin, tradeType, ordersMethod, quantity, price, createdAt, ordersStatus)
        }

        override fun toString(): String {
            return "Orders.OrdersBuilder(user=" + this.user + ", wallet=" + this.wallet + ", coin=" + this.coin + ", tradeType=" + this.tradeType + ", ordersMethod=" + this.ordersMethod + ", quantity=" + this.quantity + ", price=" + this.price + ", createdAt=" + this.createdAt + ", ordersStatus=" + this.ordersStatus + ")"
        }
    }

    companion object {
        @JvmStatic
        fun builder(): OrdersBuilder {
            return OrdersBuilder()
        }
    }
}
