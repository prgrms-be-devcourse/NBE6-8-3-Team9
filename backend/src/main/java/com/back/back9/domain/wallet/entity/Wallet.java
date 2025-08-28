package com.back.back9.domain.wallet.entity;


import com.back.back9.domain.common.vo.money.Money;
import com.back.back9.domain.common.vo.money.MoneyConverter;
import com.back.back9.domain.tradeLog.entity.TradeLog;
import com.back.back9.domain.user.entity.User;
import com.back.back9.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

// Wallet 엔티티
// 이 클래스는 사용자의 지갑 정보를 나타내며, 여러 코인의 수량 정보를 가질 수 있습니다.
@Entity
@Table(name = "wallet")
public class Wallet extends BaseEntity {

    @NotNull
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany
    @JoinColumn(name = "tradelog_id")
    private List<TradeLog> tradeLog;

    @OneToMany(mappedBy = "wallet", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CoinAmount> coinAmounts = new ArrayList<>();

    @NotNull
    private String address;

    // 기본 잔액을 5억으로 설정
    @Convert(converter = MoneyConverter.class) // Converter 쓰는 경우
    @NotNull
    private Money balance = Money.of(500_000_000L);

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected Wallet() {
    }

    public Wallet(User user, List<TradeLog> tradeLog, List<CoinAmount> coinAmounts, String address, Money balance, OffsetDateTime updatedAt) {
        this.user = user;
        this.tradeLog = tradeLog;
        this.coinAmounts = coinAmounts;
        this.address = address;
        this.balance = balance;
        this.updatedAt = updatedAt;
    }

    public static WalletBuilder builder() {
        return new WalletBuilder();
    }

    public User getUser() {
        return user;
    }

    public List<TradeLog> getTradeLog() {
        return tradeLog;
    }

    public List<CoinAmount> getCoinAmounts() {
        return coinAmounts;
    }

    public String getAddress() {
        return address;
    }

    public Money getBalance() {
        return balance;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 비즈니스 메서드
    public void charge(Money amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = OffsetDateTime.now();
    }

    public void deduct(Money amount) {
        if (!this.balance.isGreaterThanOrEqual(amount)) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = OffsetDateTime.now();
    }

    public static class WalletBuilder {
        private User user;
        private List<TradeLog> tradeLog;
        private List<CoinAmount> coinAmounts = new ArrayList<>();
        private String address;
        private Money balance = Money.of(500_000_000L);
        private OffsetDateTime updatedAt;

        WalletBuilder() {
        }

        public WalletBuilder user(User user) {
            this.user = user;
            return this;
        }

        public WalletBuilder tradeLog(List<TradeLog> tradeLog) {
            this.tradeLog = tradeLog;
            return this;
        }

        public WalletBuilder coinAmounts(List<CoinAmount> coinAmounts) {
            this.coinAmounts = coinAmounts;
            return this;
        }

        public WalletBuilder address(String address) {
            this.address = address;
            return this;
        }

        public WalletBuilder balance(Money balance) {
            this.balance = balance;
            return this;
        }

        public WalletBuilder updatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Wallet build() {
            return new Wallet(user, tradeLog, coinAmounts, address, balance, updatedAt);
        }

        public String toString() {
            return "Wallet.WalletBuilder(user=" + this.user + ", tradeLog=" + this.tradeLog + ", coinAmounts=" + this.coinAmounts + ", address=" + this.address + ", balance=" + this.balance + ", updatedAt=" + this.updatedAt + ")";
        }
    }
}
