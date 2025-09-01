package com.back.back9.domain.trigger.controller

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.orders.dto.OrdersRequest
import com.back.back9.domain.orders.entity.OrdersMethod
import com.back.back9.domain.orders.service.OrdersService
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.trigger.entity.Trigger
import com.back.back9.domain.trigger.entity.Direction
import com.back.back9.domain.trigger.entity.TriggerStatus
import com.back.back9.domain.trigger.repository.TriggerRepository
import com.back.back9.domain.trigger.service.TriggerService
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.wallet.entity.CoinAmount
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.domain.wallet.repository.WalletRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Tag("trigger")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class TriggerControllerLikeTest @Autowired constructor(
    val userRepository: UserRepository,
    val walletRepository: WalletRepository,
    val coinRepository: CoinRepository,
    val triggerRepository: TriggerRepository,
    val engine: TriggerService,          // 가격 틱 시뮬레이션에 사용
    val mvc: MockMvc
) {

    // OrdersService와 Redis는 외부효과가 크므로 목으로 대체
    lateinit var ordersService: OrdersService
    lateinit var redis: StringRedisTemplate

    private lateinit var wallet: Wallet
    private lateinit var coin: Coin

    // Redis ops 목
    private lateinit var hashOps: HashOperations<String, String, String>
    private lateinit var zsetOps: ZSetOperations<String, String>
    private lateinit var valueOps: ValueOperations<String, String>

    @BeforeEach
    fun setUp() {
        // 유저/지갑/코인 기본 데이터
        val user = userRepository.save(
            User.builder().userLoginId("u1").username("user1")
                .password("1234").role(User.UserRole.MEMBER).build()
        )
        wallet = walletRepository.save(
            Wallet.builder().user(user).address("addr1")
                .balance(com.back.back9.domain.common.vo.money.Money.of(1_000_000L))
                .coinAmounts(mutableListOf<CoinAmount>()).build()
        )
        coin = coinRepository.save(
            Coin.builder().symbol("KRW-BTC").koreanName("비트코인").englishName("Bitcoin").build()
        )

        // Redis ops 바인딩
        hashOps = org.mockito.kotlin.mock()
        zsetOps = org.mockito.kotlin.mock()
        valueOps = org.mockito.kotlin.mock()
        whenever(redis.opsForHash<String, String>()).thenReturn(hashOps)
        whenever(redis.opsForZSet<String, String>()).thenReturn(zsetOps)
        whenever(redis.opsForValue()).thenReturn(valueOps)
        whenever(hashOps.putAll(org.mockito.kotlin.any(), org.mockito.kotlin.any<Map<String, String>>())).thenReturn(
            Unit
        )
        whenever(zsetOps.add(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(
            true
        )
        whenever(zsetOps.remove(org.mockito.kotlin.any(), org.mockito.kotlin.any<String>())).thenReturn(1)
        whenever(valueOps.set(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(Unit)
        whenever(
            valueOps.set(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any<Duration>()
            )
        ).thenReturn(Unit)
    }

    @Test
    @DisplayName("예약 주문 생성 → Redis 인덱싱까지")
    fun reserve_create() {
        // (엔드포인트는 프로젝트에 맞게 교체) 예: POST /api/triggers
        val url = "/api/triggers"

        val body = """
            {
              "walletId": ${wallet.id},
              "coinId": ${coin.id},
              "direction": "UP",
              "threshold": "85000000",
              "tradeType": "BUY",
              "ordersMethod": "MARKET",
              "quantity": "0.01",
              "executePrice": "85000000"
            }
        """.trimIndent()

        val result = mvc.perform(
            post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andDo(print())
            .andExpect(status().isOk)                 // 컨트롤러가 200/201을 내려주도록 구현돼 있어야 함
            .andExpect(handler().methodName(org.hamcrest.Matchers.any(String::class.java)))
            .andReturn()

        // DB에 PENDING 트리거 하나가 생겼는지 확인
        val saved: Trigger = triggerRepository.findAll().first()
        assertEquals(TriggerStatus.PENDING, saved.status)
        assertEquals(Direction.UP, saved.direction)
        assertEquals(BigDecimal("85000000"), saved.threshold)
        assertEquals("KRW-BTC", saved.coin.symbol)

        // Redis 인덱싱 호출 검증
        verify(hashOps, times(1)).putAll(
            eq("trigger:${saved.id}"),
            org.mockito.kotlin.check {
                assertEquals(wallet.id.toString(), it["walletId"])
                assertEquals("KRW-BTC", it["coinSymbol"])
                assertEquals("UP", it["direction"])
                assertEquals("85000000", it["threshold"])
                assertEquals("BUY", it["tradeType"])
                assertEquals("MARKET", it["ordersMethod"])
                assertEquals("0.01", it["quantity"])
                assertEquals("85000000", it["executePrice"])
            }
        )
        verify(zsetOps, times(1)).add(
            eq("zset:triggers:price:KRW-BTC:up"),
            eq(saved.id.toString()),
            eq(85_000_000.0)
        )
    }

    @Test
    @DisplayName("가격 상승 돌파 시 예약 주문 발화 → executeTrade 1회 + ZSET 제거 + DB FIRED")
    fun reserve_fire_up() {
        // 먼저 하나 생성 (컨트롤러 호출 대신 바로 엔진을 써도 무방)
        val t = triggerRepository.save(
            Trigger(
                id = null,
                user = wallet.user!!,
                wallet = wallet,
                coin = coin,
                tradeType = TradeType.BUY,
                ordersMethod = OrdersMethod.MARKET,
                direction = Direction.UP,
                threshold = BigDecimal("85000000"),
                quantity = BigDecimal("0.01"),
                executePrice = BigDecimal("85000000"),
                status = TriggerStatus.PENDING,
                createdAt = LocalDateTime.now(),
                expiresAt = null
            )
        )
        // 인덱싱(보통 컨트롤러/서비스에서 수행). 테스트에선 직접 호출
        engine.rebuildAllPending() // 또는 engine.register(...)를 써도 됨

        // 가격 틱 시나리오: 이전가 84.9M → 현재가 85.1M
        whenever(valueOps.get("price:KRW-BTC:latest")).thenReturn("84900000")
        whenever(
            zsetOps.rangeByScore(
                eq("zset:triggers:price:KRW-BTC:up"),
                eq(84_900_000.0),
                eq(85_100_000.0)
            )
        ).thenReturn(setOf(t.id.toString()))
        // 중복방지 SETNX
        whenever(valueOps.setIfAbsent(eq("fired:trigger:${t.id}"), eq("1"), any())).thenReturn(true)
        // 주문 실행 목
        whenever(ordersService.executeTrade(any(), any())).thenReturn(org.mockito.kotlin.mock())

        // 틱 반영
        engine.onPriceTick("KRW-BTC", BigDecimal("85100000"))

        // 주문 실행 1회
        val walletIdCap = ArgumentCaptor.forClass(Long::class.java)
        val orqCap = ArgumentCaptor.forClass(OrdersRequest::class.java)
        verify(ordersService, times(1)).executeTrade(walletIdCap.capture(), orqCap.capture())
        assertEquals(wallet.id, walletIdCap.value)
        assertEquals("KRW-BTC", orqCap.value.coinSymbol)
        assertEquals(TradeType.BUY, orqCap.value.tradeType)
        assertEquals(OrdersMethod.MARKET, orqCap.value.ordersMethod)
        assertEquals(BigDecimal("85000000"), orqCap.value.price)
        assertEquals(BigDecimal("0.01"), orqCap.value.quantity)

        // ZSET 제거 + DB FIRED
        verify(zsetOps, times(1)).remove("zset:triggers:price:KRW-BTC:up", t.id.toString())
        val fired = triggerRepository.findById(t.id!!).orElseThrow()
        assertEquals(TriggerStatus.FIRED, fired.status)
        assertNotNull(fired.firedAt)
    }

    @Test
    @DisplayName("가격 하락 이탈 시 DOWN 트리거 발화")
    fun reserve_fire_down() {
        val t = triggerRepository.save(
            Trigger(
                id = null,
                user = wallet.user!!,
                wallet = wallet,
                coin = coin,
                tradeType = TradeType.SELL,
                ordersMethod = OrdersMethod.LIMIT,
                direction = Direction.DOWN,
                threshold = BigDecimal("83000000"),
                quantity = BigDecimal("0.02"),
                executePrice = BigDecimal("83000000"),
                status = TriggerStatus.PENDING,
                createdAt = LocalDateTime.now(),
                expiresAt = null
            )
        )
        engine.rebuildAllPending()

        // 이전가 85.0M → 현재가 83.5M
        whenever(valueOps.get("price:KRW-BTC:latest")).thenReturn("85000000")
    }
}
