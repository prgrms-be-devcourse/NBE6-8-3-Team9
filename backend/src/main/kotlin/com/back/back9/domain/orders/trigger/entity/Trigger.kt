package com.back.back9.domain.orders.trigger.entity

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 예약주문(가격 트리거) 정본 테이블.
 * - DB에 영구 저장 → 앱 재시작/장애 복구 시 Redis 인덱스를 이 테이블로부터 재구성(rebuild)
 * - 발화되면 status=FIRED + firedAt 기록
 */
@Entity
@Table(name = "triggers")
class Trigger(

    @OneToOne
    @JoinColumn(name = "order_id")
    var order: Orders? = null,

    @ManyToOne @JoinColumn(name = "user_id")
    var user: @NotNull User? = null,

    @ManyToOne @JoinColumn(name = "wallet_id")
    var wallet: @NotNull Wallet? = null,

    @ManyToOne @JoinColumn(name = "coin_id")
    var coin: @NotNull Coin? = null,

    @Enumerated(EnumType.STRING)
    var tradeType: TradeType? = null,

    @Enumerated(EnumType.STRING)
    var ordersMethod: OrdersMethod? = null,

    @Enumerated(EnumType.STRING)
    var direction: Direction? = null,

    var threshold: BigDecimal? = null,
    var quantity: BigDecimal? = null,
    var executePrice: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    var status: TriggerStatus = TriggerStatus.PENDING,

    var expiresAt: LocalDateTime? = null,
    var firedAt: LocalDateTime? = null
) : BaseEntity() {
    // ✅ Hibernate 용 no-args 생성자
    constructor() : this(
        order = null,
        user = null,
        wallet = null,
        coin = null,
        tradeType = null,
        ordersMethod = null,
        direction = null,
        threshold = null,
        quantity = null,
        executePrice = null,
        status = TriggerStatus.PENDING,
        expiresAt = null,
        firedAt = null
    )
}
