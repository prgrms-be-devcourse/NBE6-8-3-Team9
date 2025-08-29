package com.back.back9.domain.tradeLog.controller

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.tradeLog.repository.TradeLogRepository
import com.back.back9.domain.tradeLog.service.TradeLogService
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.wallet.entity.CoinAmount
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.domain.wallet.repository.WalletRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
@Tag("trade_log")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class TradeLogControllerTest @Autowired constructor(
    val tradeLogService: TradeLogService,
    val tradeLogRepository: TradeLogRepository,
    val userRepository: UserRepository,
    val walletRepository: WalletRepository,
    val coinRepository: CoinRepository,
    val mvc: MockMvc
) {

    private lateinit var wallet1: Wallet
    private lateinit var wallet2: Wallet
    private lateinit var wallet3: Wallet
    private lateinit var coin1: Coin
    private lateinit var coin2: Coin
    private lateinit var coin3: Coin

    @BeforeEach
    fun setUp() {
        tradeLogRepository.deleteAll()

        // 유저 3명 생성
        val user1 = userRepository.save(
            User.builder().userLoginId("u1").username("user1").password("1234").role(User.UserRole.MEMBER).build()
        )
        val user2 = userRepository.save(
            User.builder().userLoginId("u2").username("user2").password("1234").role(User.UserRole.MEMBER).build()
        )
        val user3 = userRepository.save(
            User.builder().userLoginId("u3").username("user3").password("1234").role(User.UserRole.MEMBER).build()
        )

        wallet1 = walletRepository.save(
            Wallet.builder().user(user1).address("addr1").balance(Money.of(1_000_000L)).coinAmounts(mutableListOf<CoinAmount>()).build()
        )
        wallet2 = walletRepository.save(
            Wallet.builder().user(user2).address("addr2").balance(Money.of(1_000_000L)).coinAmounts(mutableListOf<CoinAmount>()).build()
        )
        wallet3 = walletRepository.save(
            Wallet.builder().user(user3).address("addr3").balance(Money.of(1_000_000L)).coinAmounts(mutableListOf<CoinAmount>()).build()
        )

        // 코인 3개 생성
        coin1 = coinRepository.save(Coin.builder().symbol("KRW-BTC3").koreanName("비트코인3").englishName("Bitcoin3").build())
        coin2 = coinRepository.save(Coin.builder().symbol("KRW-ETH3").koreanName("이더리움3").englishName("Ethereum3").build())
        coin3 = coinRepository.save(Coin.builder().symbol("KRW-XRP3").koreanName("리플3").englishName("Ripple3").build())

        createTradeLogs()
    }

    private fun createTradeLogs() {
        if (tradeLogService.count() > 0) return

        val logs = mutableListOf<TradeLog>()
        val baseDate = LocalDateTime.of(2025, 7, 25, 0, 0)

        for (i in 1..15) {
            val type = if (i % 3 == 0) TradeType.SELL else TradeType.BUY
            val coin = when {
                i <= 5 -> coin1
                i <= 10 -> coin2
                else -> coin3
            }

            val log = TradeLog.builder()
                .wallet(wallet1)
                .coin(coin)
                .type(type)
                .quantity(BigDecimal.ONE)
                .price(Money.of((1000 + i * 1000).toLong()))
                .build()

            log.setCreatedAt(baseDate.plusDays(((i - 1) * 7).toLong()))
            logs.add(log)
        }

        tradeLogService.saveAll(logs.toMutableList())
    }

    @Test
    @DisplayName("거래 내역 전체 조회")
    fun t4() {
        val url = "/api/tradeLog/wallet/${wallet1!!.id}"

        val resultActions = mvc.perform(get(url)
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(TradeLogController::class.java))
            .andExpect(handler().methodName("getItems"))
        //.andExpect(jsonPath("$.length()").value(15))
    }

    @Test
    @DisplayName("거래 내역 필터 조회 - 당일, 모든 거래")
    fun t5() {
        val url = "/api/tradeLog/wallet/${wallet1!!.id}"

        val resultActions = mvc.perform(get(url)
            .param("startDate", "2025-07-25")
            .param("endDate", "2025-07-25")
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(TradeLogController::class.java))
            .andExpect(handler().methodName("getItems"))
        //.andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    @DisplayName("거래 내역 조회 - 일별, 매수 거래")
    fun t6() {
        val url = "/api/tradeLog/wallet/${wallet1!!.id}"

        val resultActions = mvc.perform(get(url)
            .param("startDate", "2025-07-27")
            .param("endDate", "2025-08-27")
            .param("type", "BUY")
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(TradeLogController::class.java))
            .andExpect(handler().methodName("getItems"))
        //.andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    @DisplayName("거래 내역 조회 - 월별, 매도 거래")
    fun t7() {
        val url = "/api/tradeLog/wallet/${wallet1!!.id}"

        val resultActions = mvc.perform(get(url)
            .param("startDate", "2025-07-01")
            .param("endDate", "2025-08-31")
            .param("type", "SELL")
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(TradeLogController::class.java))
            .andExpect(handler().methodName("getItems"))
        //.andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @DisplayName("시작일이 종료일보다 이후일 때")
    fun t8() {
        val url = "/api/tradeLog/wallet/${wallet1!!.id}"

        val resultActions = mvc.perform(get(url)
            .param("startDate", "2025-03-01")
            .param("endDate", "2025-01-01")
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(TradeLogController::class.java))
            .andExpect(handler().methodName("getItems"))
        //.andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @DisplayName("모든 필터 없음 (파라미터 없음)")
    fun t9() {
        val url = "/api/tradeLog/wallet/${wallet1!!.id}"

        val resultActions = mvc.perform(get(url)
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(TradeLogController::class.java))
            .andExpect(handler().methodName("getItems"))
        //.andExpect(jsonPath("$.length()").value(15))
    }

    @Test
    @DisplayName("거래 없음")
    fun t10() {
        val url = "/api/tradeLog/wallet/${wallet1!!.id}"

        val resultActions = mvc.perform(get(url)
            .param("startDate", "1999-01-01")
            .param("endDate", "1999-01-31")
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(TradeLogController::class.java))
            .andExpect(handler().methodName("getItems"))
        //.andExpect(jsonPath("$.length()").value(0))
    }
}
