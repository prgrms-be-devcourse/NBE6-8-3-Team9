package com.back.back9.domain.trigger.service

import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.orders.dto.OrdersRequest
import com.back.back9.domain.orders.entity.OrdersMethod
import com.back.back9.domain.orders.service.OrdersService
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.trigger.dto.PriceTriggerCreateRequest
import com.back.back9.domain.trigger.entity.Direction
import com.back.back9.domain.trigger.entity.Trigger
import com.back.back9.domain.trigger.entity.TriggerStatus
import com.back.back9.domain.trigger.repository.TriggerRepository
import com.back.back9.domain.trigger.support.RedisKeys
import com.back.back9.domain.wallet.repository.WalletRepository
import jakarta.transaction.Transactional
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID
import java.math.BigDecimal
import java.time.Duration


/**
 * 예약주문 엔진:
 * - Register: DB 저장 + Redis 인덱싱
 * - Rebuild: DB(PENDING) → Redis 인덱스 복구
 * - onPriceTick: 실시간 가격 반영 → 범위 매칭 트리거 발화
 */
@Service
class TriggerService(
    private val triggerRepository: TriggerRepository,
    private val walletRepository: WalletRepository,
    private val coinRepository: CoinRepository,
    private val redis: StringRedisTemplate,
    private val ordersService: OrdersService //// 발화 시 기존 주문 로직 재사용
) {
    // ===== 등록 =====

    /**
     * 예약 생성 (정본: DB / 런타임 인덱스: Redis)
     */
    @Transactional
    fun register(req: PriceTriggerCreateRequest): UUID {
        val wallet = walletRepository.findById(req.walletId)
            .orElseThrow { IllegalArgumentException("지갑을 찾을 수 없습니다.") }
        val coin = coinRepository.findById(req.coinId)
            .orElseThrow { IllegalArgumentException("코인을 찾을 수 없습니다.") }

        // 1) DB에 저장 (정본)
        val t = Trigger(
            id = null,
            user = wallet.user!!,
            wallet = wallet,
            coin = coin,
            tradeType = req.tradeType,
            ordersMethod = req.ordersMethod,
            direction = req.direction,
            threshold = req.threshold,
            quantity = req.quantity,
            executePrice = req.executePrice,
            status = TriggerStatus.PENDING,
            expiresAt = req.expiresAt
        )
        val saved = triggerRepository.save(t)

        // 2) Redis 인덱싱 (실패해도 재시작 시 rebuild로 복구 가능)
        indexToRedis(saved)

        return saved.id!!
    }

    /**
     * DB의 PENDING 트리거들을 Redis 인덱스로 재구성.
     * - 앱 재시작 / Redis 초기화 / 장애 복구 시 호출
     */
    @Transactional // 조회만 하므로 readOnly 권장
    fun rebuildAllPending() {
        val rows = triggerRepository.findAllPending(TriggerStatus.PENDING, LocalDateTime.now())
        rows.forEach { indexToRedis(it) }
    }

    private fun indexToRedis(t: Trigger) {
        val id = t.id!!.toString()
        val sym = t.coin.symbol // coin 엔티티에 symbol이 있다고 가정
        val score = t.threshold.toDouble()

        // 발화 시 필요한 메타를 HASH로 저장
        redis.opsForHash<String, String>().putAll(
            RedisKeys.triggerHash(id), mapOf(
            "walletId" to t.wallet.id!!.toString(),
            "coinSymbol" to sym,
            "direction" to t.direction.name,
            "threshold" to t.threshold.toPlainString(),
            "tradeType" to t.tradeType.name,
            "ordersMethod" to t.ordersMethod.name,
            "quantity" to t.quantity.toPlainString(),
            "executePrice" to t.executePrice.toPlainString(),
            "triggerId" to id
        ))

        // 가격 인덱스(ZSET) 등록
        when (t.direction) {
            Direction.UP   -> redis.opsForZSet().add(RedisKeys.upZset(sym), id, score)
            Direction.DOWN -> redis.opsForZSet().add(RedisKeys.downZset(sym), id, score)
        }
    }

    // ===== 실시간 가격 틱 반영 / 발화 =====

    /**
     * 인저스터가 실시간 가격을 받아 호출.
     * 경계 규칙:
     *  - 상승: (pPrev, pNow] → UP 발화
     *  - 하락: [pNow, pPrev) → DOWN 발화
     */
    @Transactional
    fun onPriceTick(symbol: String, pNow: BigDecimal) {
        val latestKey = RedisKeys.latestPrice(symbol)
        val prevStr = redis.opsForValue().get(latestKey)
        val pPrev = prevStr?.toBigDecimalOrNull() ?: pNow

        // 최신가 갱신
        redis.opsForValue().set(latestKey, pNow.toPlainString())

        when {
            pNow > pPrev -> fireRangeUp(symbol, pPrev, pNow)
            pNow < pPrev -> fireRangeDown(symbol, pNow, pPrev)
            else -> { /* 같으면 아무것도 안 함 */ }
        }
    }

    private fun fireRangeUp(symbol: String, pPrev: BigDecimal, pNow: BigDecimal) {
        val key = RedisKeys.upZset(symbol)
        val ids = redis.opsForZSet().rangeByScore(key, pPrev.toDouble(), pNow.toDouble()) ?: emptySet()
        fireOnceAndExecute(ids, key)
    }

    private fun fireRangeDown(symbol: String, pNow: BigDecimal, pPrev: BigDecimal) {
        val key = RedisKeys.downZset(symbol)
        val ids = redis.opsForZSet().rangeByScore(key, pNow.toDouble(), pPrev.toDouble()) ?: emptySet()
        fireOnceAndExecute(ids, key)
    }

    /**
     * 중복(재진입, 중복 틱 등) 방지:
     *  - fired:trigger:{id} 를 SETNX(=setIfAbsent)로 마크
     *  - 처음 마크한 워커만 발화/주문 실행
     * 실행 후:
     *  - DB status=FIRED, firedAt 기록
     *  - ZSET에서 제거(1회성)
     */
    private fun fireOnceAndExecute(ids: Set<String>, indexKey: String) {
        if (ids.isEmpty()) return

        for (id in ids) {
            val firedKey = RedisKeys.firedFlag(id)
            val isFirst = redis.opsForValue().setIfAbsent(firedKey, "1", Duration.ofDays(1))
            if (isFirst != true) continue

            try {
                // ✅ 발화 메타 로드 (그냥 Map으로 받으면 됩니다)
                val meta: Map<String, String> =
                    redis.opsForHash<String, String>().entries(RedisKeys.triggerHash(id))

                // ✅ 값 꺼내기 (없으면 예외)
                val walletId = meta.getValue("walletId").toLong()
                val coinSymbol = meta.getValue("coinSymbol")
                val tradeType = TradeType.valueOf(meta.getValue("tradeType"))
                val ordersMethod = OrdersMethod.valueOf(meta.getValue("ordersMethod"))
                val quantity = BigDecimal(meta.getValue("quantity"))
                val executePrice = BigDecimal(meta.getValue("executePrice"))

                // 기존 주문 로직 재사용
                val orq = OrdersRequest(
                    coinSymbol = coinSymbol,
                    tradeType = tradeType,
                    ordersMethod = ordersMethod,
                    price = executePrice,
                    quantity = quantity
                )
                ordersService.executeTrade(walletId, orq)
            } finally {
                // 1회성 트리거면 인덱스 제거
                redis.opsForZSet().remove(indexKey, id)
            }
        }
    }


    @Transactional
    fun markFired(triggerId: UUID) {
        val t = triggerRepository.findById(triggerId).orElseThrow()
        if (t.status == TriggerStatus.FIRED) return
        t.status = TriggerStatus.FIRED
        t.firedAt = LocalDateTime.now()
        // JPA dirty checking으로 반영
    }
}