package com.back.back9.domain.orders.trigger.service

import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.orders.orders.dto.OrderResponse
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.orders.orders.service.OrdersService
import com.back.back9.domain.orders.price.fetcher.ExchangePriceFetcher
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.orders.trigger.entity.Direction
import com.back.back9.domain.orders.trigger.entity.Direction.*
import com.back.back9.domain.orders.trigger.entity.Trigger
import com.back.back9.domain.orders.trigger.entity.TriggerStatus
import com.back.back9.domain.orders.trigger.repository.TriggerRepository
import com.back.back9.domain.orders.trigger.support.RedisKeys
import com.back.back9.domain.wallet.repository.WalletRepository
import jakarta.transaction.Transactional
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.math.BigDecimal
import java.time.Duration
import org.slf4j.LoggerFactory

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
    private val ordersService: OrdersService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @Transactional
    fun registerFromOrder(walletId: Long, request: OrdersRequest): OrderResponse {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { IllegalArgumentException("지갑을 찾을 수 없습니다.") }
        val coin = coinRepository.findBySymbol(request.coinSymbol)
            .orElseThrow { IllegalArgumentException("코인을 찾을 수 없습니다.") }

        // BUY → DOWN, SELL → UP 으로 방향 결정 (저점 매수 / 고점 매도 전략)
        val direction = if (request.tradeType == TradeType.BUY) Direction.DOWN else Direction.UP

        val trigger = Trigger(
            user = wallet.user,
            wallet = wallet,
            coin = coin,
            tradeType = request.tradeType!!,
            ordersMethod = request.ordersMethod!!,
            direction = direction,
            threshold = request.price,
            quantity = request.quantity,
            executePrice = request.price,
            status = TriggerStatus.PENDING,
            expiresAt = null
        )

        val saved = triggerRepository.save(trigger)

        indexToRedis(saved)

        return OrderResponse.fromLimit(saved)
    }
    // ===== 등록 =====

    /**
     * 예약 생성 (정본: DB / 런타임 인덱스: Redis)
     */
    @Transactional
    fun register(trigger: Trigger): Trigger {
        val wallet = trigger.wallet?.let { walletRepository.findById(it.id!!) }
            ?.orElseThrow { IllegalArgumentException("지갑을 찾을 수 없습니다.") }
        val coin = trigger.coin?.let { coinRepository.findById(it.id!!) }
            ?.orElseThrow { IllegalArgumentException("코인을 찾을 수 없습니다.") }

        val t = Trigger(
            user = wallet?.let { it.user!! },
            wallet = wallet,
            coin = coin,
            tradeType = trigger.tradeType,
            ordersMethod = trigger.ordersMethod,
            direction = trigger.direction,
            threshold = trigger.threshold,
            quantity = trigger.quantity,
            executePrice = trigger.executePrice,
            status = TriggerStatus.PENDING,
            expiresAt = trigger.expiresAt
        )

        val saved = triggerRepository.save(t)

        // Redis 인덱싱
        indexToRedis(saved)

        return saved
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
        val sym = t.coin?.symbol ?: throw IllegalArgumentException("코인 심볼이 없습니다.")
        val score = t.threshold?.toDouble()

        // 발화 시 필요한 메타를 HASH로 저장
        val meta = mutableMapOf<String, String>().apply {
            put("walletId", t.wallet?.let { it.id!! }.toString())
            put("coinSymbol", sym)
            t.direction?.let { put("direction", it.name) }
            t.threshold?.let { put("threshold", it.toPlainString()) }
            t.tradeType?.let { put("tradeType", it.name) }
            t.ordersMethod?.let { put("ordersMethod", it.name) }
            t.quantity?.let { put("quantity", it.toPlainString()) }
            t.executePrice?.let { put("executePrice", it.toPlainString()) }
            put("triggerId", id)
        }

        redis.opsForHash<String, String>().putAll(RedisKeys.triggerHash(id), meta)

        // 가격 인덱스(ZSET) 등록
        when (t.direction) {
            Direction.UP   -> score?.let { redis.opsForZSet().add(RedisKeys.upZset(sym), id, it) }
            Direction.DOWN -> score?.let { redis.opsForZSet().add(RedisKeys.downZset(sym), id, it) }
            null -> { /* TODO: 방향이 없을 경우 처리 */ }
        }
    }
    // ===== 실시간 가격 틱 반영 / 발화 =====

    /**
     * 인저스터가 실시간 가격을 받아 호출.
     * 경계 규칙:
     *  - 하락: 구매
     *  - 상승: 판매
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
            else -> { 
                log.info("fire 해당 없음")
            }
        }
    }

    private fun fireRangeUp(symbol: String, pPrev: BigDecimal, pNow: BigDecimal) {
        log.info("fireRangeUp: $symbol, ($pPrev, $pNow]")
        val key = RedisKeys.upZset(symbol)
        val ids = redis.opsForZSet().rangeByScore(key, pPrev.toDouble(), pNow.toDouble()) ?: emptySet()
        fireOnceAndExecute(ids, key)
    }

    private fun fireRangeDown(symbol: String, pNow: BigDecimal, pPrev: BigDecimal) {
        log.info("fireRangeDown: $symbol, ($pPrev, $pNow)")
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
                // 발화 메타 로드 Map
                val meta: Map<String, String> =
                    redis.opsForHash<String, String>().entries(RedisKeys.triggerHash(id))

                // 값 꺼내기 (없으면 예외)
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
                ordersService.executeMarketOrder(walletId, orq)
                log.info("✅ Trigger ${id} fired, BUY 주문 체결됨: $orq")

            } finally {
                // 1회성 트리거면 인덱스 제거
                redis.opsForZSet().remove(indexKey, id)
            }
        }
    }


    @Transactional
    fun markFired(triggerId: Long) {
        val t = triggerRepository.findById(triggerId).orElseThrow()
        if (t.status == TriggerStatus.FIRED) return
        t.status = TriggerStatus.FIRED
        t.firedAt = LocalDateTime.now()
        // JPA dirty checking으로 반영
    }
}