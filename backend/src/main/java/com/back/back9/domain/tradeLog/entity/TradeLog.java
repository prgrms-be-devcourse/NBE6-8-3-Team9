package com.back.back9.domain.tradeLog.entity;

import com.back.back9.domain.coin.entity.Coin;
import com.back.back9.domain.common.vo.money.Money;
import com.back.back9.domain.common.vo.money.MoneyConverter;
import com.back.back9.domain.wallet.entity.Wallet;
import com.back.back9.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class TradeLog extends BaseEntity {
    //거래한 지갑
    @NotNull
    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;
    //거래한 거래소
//    @NotNull
//    @Column(name = "exchange_id")
//    private int exchangeId;

    //거래한 코인
    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = true)
    private Coin coin;

    //거래 타입 : BUY, SELL,CHARGE
    @Enumerated
    @Column(nullable = false)
    private TradeType type;

    // 수량
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    // 단가
    @Convert(converter = MoneyConverter.class)
    private Money price;
    //DB 저장 안함
//    @Column(nullable = false, precision = 19, scale = 8)
//    private BigDecimal profitRate;

    public TradeLog() {
    }

    public TradeLog(Wallet wallet, Coin coin, TradeType type, BigDecimal quantity, Money price) {
        this.wallet = wallet;
        this.coin = coin;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
    }

    public static TradeLogBuilder builder() {
        return new TradeLogBuilder();
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Coin getCoin() {
        return coin;
    }

    public TradeType getType() {
        return type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getPrice() {
        return price;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        super.createdAt = createdAt; // BaseEntity의 protected createdAt 필드 접근
    }

    public static class TradeLogBuilder {
        private Wallet wallet;
        private Coin coin;
        private TradeType type;
        private BigDecimal quantity;
        private Money price;

        TradeLogBuilder() {
        }

        public TradeLogBuilder wallet(Wallet wallet) {
            this.wallet = wallet;
            return this;
        }

        public TradeLogBuilder coin(Coin coin) {
            this.coin = coin;
            return this;
        }

        public TradeLogBuilder type(TradeType type) {
            this.type = type;
            return this;
        }

        public TradeLogBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public TradeLogBuilder price(Money price) {
            this.price = price;
            return this;
        }

        public TradeLog build() {
            return new TradeLog(wallet, coin, type, quantity, price);
        }

        public String toString() {
            return "TradeLog.TradeLogBuilder(wallet=" + this.wallet + ", coin=" + this.coin + ", type=" + this.type + ", quantity=" + this.quantity + ", price=" + this.price + ")";
        }
    }
}