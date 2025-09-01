package com.back.back9.domain.trigger.entity

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.orders.entity.OrdersMethod
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.domain.trigger.entity.Direction
import com.back.back9.domain.trigger.entity.TriggerStatus
import com.back.back9.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * 예약주문(가격 트리거) 정본 테이블.
 * - DB에 영구 저장 → 앱 재시작/장애 복구 시 Redis 인덱스를 이 테이블로부터 재구성(rebuild)
 * - 발화되면 status=FIRED + firedAt 기록
 */
@Entity
@Table(name = "triggers")
class Trigger(
    @Id
    @GeneratedValue
    var id: UUID? = null,

    @ManyToOne @JoinColumn(name = "user_id")
    var user: @NotNull User,

    @ManyToOne @JoinColumn(name = "wallet_id")
    var wallet: @NotNull Wallet,

    @ManyToOne @JoinColumn(name = "coin_id")
    var coin: @NotNull Coin,

    @Enumerated(EnumType.STRING)
    var tradeType: TradeType,                  // BUY / SELL

    @Enumerated(EnumType.STRING)
    var ordersMethod: OrdersMethod,            // MARKET / LIMIT (UI 표기용)

    @Enumerated(EnumType.STRING)
    var direction: Direction,                  // UP(상승 돌파) / DOWN(하락 이탈)

    var threshold: BigDecimal,                 // 임계가 (이 값에 '도달'하면 발화)

    var quantity: BigDecimal,                  // 체결 수량

    /**
     * 발화 시 주문에 기록/사용할 가격.
     * - 이번 설계에선 "프론트에서 전달한 가격을 그대로 기록"하는 정책이므로 저장해둠
     */
    var executePrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    var status: TriggerStatus = TriggerStatus.PENDING,

    var expiresAt: LocalDateTime? = null,      // 옵션: 만료시각

    var firedAt: LocalDateTime? = null         // 발화 시각
) : BaseEntity()
