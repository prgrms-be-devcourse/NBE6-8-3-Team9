package com.back.back9.domain.orders.entity;

import com.back.back9.global.jpa.entity.BaseEntity;
import com.back.back9.domain.coin.entity.Coin;
import com.back.back9.domain.tradeLog.entity.TradeType;
import com.back.back9.domain.user.entity.User;
import com.back.back9.domain.wallet.entity.Wallet;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Orders extends BaseEntity {
    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @NotNull
    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;
    @NotNull
    @ManyToOne
    @JoinColumn(name = "coin_id")
    private Coin coin;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType; // BUY, SELL

    @Enumerated(EnumType.STRING)
    private OrdersMethod ordersMethod; // LIMIT, MARKET

    private BigDecimal quantity; // 수량

    private BigDecimal price; // 지정가 주문 시 사용

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private OrdersStatus ordersStatus; // PENDING, FILLED, CANCELLED 등

    protected Orders() {
    }

    public Orders(User user, Wallet wallet, Coin coin, TradeType tradeType, OrdersMethod ordersMethod, BigDecimal quantity, BigDecimal price, LocalDateTime createdAt, OrdersStatus ordersStatus) {
        this.user = user;
        this.wallet = wallet;
        this.coin = coin;
        this.tradeType = tradeType;
        this.ordersMethod = ordersMethod;
        this.quantity = quantity;
        this.price = price;
        this.createdAt = createdAt;
        this.ordersStatus = ordersStatus;
    }

    public static OrdersBuilder builder() {
        return new OrdersBuilder();
    }

    public User getUser() {
        return user;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Coin getCoin() {
        return coin;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public OrdersMethod getOrdersMethod() {
        return ordersMethod;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrdersStatus getOrdersStatus() {
        return ordersStatus;
    }

    public static class OrdersBuilder {
        private User user;
        private Wallet wallet;
        private Coin coin;
        private TradeType tradeType;
        private OrdersMethod ordersMethod;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDateTime createdAt;
        private OrdersStatus ordersStatus;

        OrdersBuilder() {
        }

        public OrdersBuilder user(User user) {
            this.user = user;
            return this;
        }

        public OrdersBuilder wallet(Wallet wallet) {
            this.wallet = wallet;
            return this;
        }

        public OrdersBuilder coin(Coin coin) {
            this.coin = coin;
            return this;
        }

        public OrdersBuilder tradeType(TradeType tradeType) {
            this.tradeType = tradeType;
            return this;
        }

        public OrdersBuilder ordersMethod(OrdersMethod ordersMethod) {
            this.ordersMethod = ordersMethod;
            return this;
        }

        public OrdersBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrdersBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public OrdersBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrdersBuilder ordersStatus(OrdersStatus ordersStatus) {
            this.ordersStatus = ordersStatus;
            return this;
        }

        public Orders build() {
            return new Orders(user, wallet, coin, tradeType, ordersMethod, quantity, price, createdAt, ordersStatus);
        }

        public String toString() {
            return "Orders.OrdersBuilder(user=" + this.user + ", wallet=" + this.wallet + ", coin=" + this.coin + ", tradeType=" + this.tradeType + ", ordersMethod=" + this.ordersMethod + ", quantity=" + this.quantity + ", price=" + this.price + ", createdAt=" + this.createdAt + ", ordersStatus=" + this.ordersStatus + ")";
        }
    }
}
