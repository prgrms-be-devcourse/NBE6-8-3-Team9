package com.back.back9.domain.wallet.entity;


import com.back.back9.domain.common.vo.money.Money;
import com.back.back9.domain.common.vo.money.MoneyConverter;
import com.back.back9.domain.tradeLog.entity.TradeLog;
import com.back.back9.domain.user.entity.User;
import com.back.back9.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

// Wallet 엔티티
// 이 클래스는 사용자의 지갑 정보를 나타내며, 여러 코인의 수량 정보를 가질 수 있습니다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private List<CoinAmount> coinAmounts = new ArrayList<>();

    @NotNull
    private String address;

    // 기본 잔액을 5억으로 설정
    @Builder.Default
    @Convert(converter = MoneyConverter.class) // Converter 쓰는 경우
    @NotNull
    private Money balance = Money.of(500_000_000L);

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

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
}
