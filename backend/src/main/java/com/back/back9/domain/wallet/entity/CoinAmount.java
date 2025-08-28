package com.back.back9.domain.wallet.entity;

import com.back.back9.domain.coin.entity.Coin;
import com.back.back9.domain.common.vo.money.Money;
import com.back.back9.domain.common.vo.money.MoneyConverter;
import com.back.back9.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// CoinAmount 엔티티
// 코인 수량 및 정보
@Entity
@Table(name = "coin_amount")
public class CoinAmount extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_id")
    private Coin coin;

    @NotNull
    @Column(name = "quantity", precision = 19, scale = 8)
    private BigDecimal quantity;  // 코인 개수 (예: 0.005개)

    @NotNull
    @Column(name = "total_amount", precision = 19, scale = 8)
    @Convert(converter = MoneyConverter.class)
    private Money totalAmount;// 총 투자 금액 (quantity * 평균 매수가)

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected CoinAmount() {
    }

    public CoinAmount(Wallet wallet, Coin coin, BigDecimal quantity, Money totalAmount, OffsetDateTime updatedAt) {
        this.wallet = wallet;
        this.coin = coin;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.updatedAt = updatedAt;
    }

    public static CoinAmountBuilder builder() {
        return new CoinAmountBuilder();
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Coin getCoin() {
        return coin;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 비즈니스 메서드
    public void updateQuantityAndAmount(BigDecimal newQuantity, Money newTotalAmount) {
        this.quantity = newQuantity;
        this.totalAmount = newTotalAmount;
        this.updatedAt = OffsetDateTime.now();
    }

    public void addQuantityAndAmount(BigDecimal additionalQuantity, Money additionalAmount) {
        this.quantity = this.quantity.add(additionalQuantity);
        this.totalAmount = this.totalAmount.add(additionalAmount);
        this.updatedAt = OffsetDateTime.now();
    }

    public void subtractQuantityAndAmount(BigDecimal subtractQuantity, Money subtractAmount) {
        this.quantity = this.quantity.subtract(subtractQuantity);
        this.totalAmount = this.totalAmount.subtract(subtractAmount);
        this.updatedAt = OffsetDateTime.now();
    }

    // 평균 매수 단가 계산 (총 투자금액 / 보유 수량)
    public Money getAverageBuyPrice() {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return Money.zero();
        }
        return totalAmount.divide(quantity);
    }

    // 기존 메서드들은 Deprecated 처리
    @Deprecated
    public void updateAmount(BigDecimal newAmount) {
        this.totalAmount = Money.of(newAmount);
        this.updatedAt = OffsetDateTime.now();
    }

    @Deprecated
    public void addAmount(BigDecimal additionalAmount) {
        this.totalAmount = this.totalAmount.add(Money.of(additionalAmount));
        this.updatedAt = OffsetDateTime.now();
    }

    @Deprecated
    public void subtractAmount(BigDecimal subtractAmount) {
        this.totalAmount = this.totalAmount.subtract(Money.of(subtractAmount));
        this.updatedAt = OffsetDateTime.now();
    }

    public static class CoinAmountBuilder {
        private Wallet wallet;
        private Coin coin;
        private BigDecimal quantity;
        private Money totalAmount;
        private OffsetDateTime updatedAt;

        CoinAmountBuilder() {
        }

        public CoinAmountBuilder wallet(Wallet wallet) {
            this.wallet = wallet;
            return this;
        }

        public CoinAmountBuilder coin(Coin coin) {
            this.coin = coin;
            return this;
        }

        public CoinAmountBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public CoinAmountBuilder totalAmount(Money totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public CoinAmountBuilder updatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CoinAmount build() {
            return new CoinAmount(wallet, coin, quantity, totalAmount, updatedAt);
        }

        public String toString() {
            return "CoinAmount.CoinAmountBuilder(wallet=" + this.wallet + ", coin=" + this.coin + ", quantity=" + this.quantity + ", totalAmount=" + this.totalAmount + ", updatedAt=" + this.updatedAt + ")";
        }
    }
}
