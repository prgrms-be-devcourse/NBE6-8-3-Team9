package com.back.back9.domain.tradeLog.entity

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.common.vo.money.MoneyConverter
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class TradeLog : BaseEntity {
    //거래한 지갑
    @ManyToOne
    @JoinColumn(name = "wallet_id")
    var wallet: @NotNull Wallet? = null
        private set

    //거래한 거래소
    //    @NotNull
    //    @Column(name = "exchange_id")
    //    private int exchangeId;
    //거래한 코인
    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = true)
    var coin: Coin? = null
        private set

    //거래 타입 : BUY, SELL,CHARGE
    @Enumerated
    @Column(nullable = false)
    var type: TradeType? = null
        private set

    // 수량
    @Column(nullable = false, precision = 19, scale = 8)
    var quantity: BigDecimal? = null
        private set

    // 단가
    var price: Money? = null
        private set

    //DB 저장 안함
    //    @Column(nullable = false, precision = 19, scale = 8)
    //    private BigDecimal profitRate;
    constructor()

    constructor(wallet: Wallet?, coin: Coin?, type: TradeType?, quantity: BigDecimal?, price: Money?) {
        this.wallet = wallet
        this.coin = coin
        this.type = type
        this.quantity = quantity
        this.price = price
    }


    class TradeLogBuilder internal constructor() {
        private var wallet: Wallet? = null
        private var coin: Coin? = null
        private var type: TradeType? = null
        private var quantity: BigDecimal? = null
        private var price: Money? = null

        fun wallet(wallet: Wallet?): TradeLogBuilder {
            this.wallet = wallet
            return this
        }

        fun coin(coin: Coin?): TradeLogBuilder {
            this.coin = coin
            return this
        }

        fun type(type: TradeType?): TradeLogBuilder {
            this.type = type
            return this
        }

        fun quantity(quantity: BigDecimal?): TradeLogBuilder {
            this.quantity = quantity
            return this
        }

        fun price(price: Money?): TradeLogBuilder {
            this.price = price
            return this
        }

        fun build(): TradeLog {
            return TradeLog(wallet, coin, type, quantity, price)
        }

        override fun toString(): String {
            return "TradeLog.TradeLogBuilder(wallet=" + this.wallet + ", coin=" + this.coin + ", type=" + this.type + ", quantity=" + this.quantity + ", price=" + this.price + ")"
        }
    }

    companion object {
        @JvmStatic
        fun builder(): TradeLogBuilder {
            return TradeLogBuilder()
        }
    }
}