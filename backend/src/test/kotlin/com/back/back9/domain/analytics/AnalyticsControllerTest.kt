package com.back.back9.domain.analytics

import com.back.back9.domain.analytics.controller.AnalyticsController
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
import com.back.back9.domain.wallet.repository.CoinAmountRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Tag("trade_log")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AnalyticsControllerTest {
    @Autowired
    private val tradeLogService: TradeLogService? = null

    @Autowired
    private val tradeLogRepository: TradeLogRepository? = null

    @Autowired
    private val walletRepository: WalletRepository? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val coinRepository: CoinRepository? = null

    @Autowired
    private val coinAmountRepository: CoinAmountRepository? = null

    @Autowired
    private val mockMvc: MockMvc? = null
    private lateinit var user1: User
    private lateinit var user2: User
    private lateinit var user3: User
    private lateinit var coin1: Coin
    private lateinit var coin2: Coin
    private lateinit var coin3: Coin
    private lateinit var coin4: Coin

    private lateinit var wallet1: Wallet
    private lateinit var wallet2: Wallet
    private lateinit var wallet3: Wallet

    @BeforeEach
    fun setUp() {
        tradeLogRepository!!.deleteAll()
        coinAmountRepository!!.deleteAll()
        walletRepository!!.deleteAll()
        coinRepository!!.deleteAll()
        userRepository!!.deleteAll()

        userCreate()
        walletCreate()
        coinCreate()
        coinAmountCreate()
        tradeLogCreate()
        tradeLogChargeCreate()
    }

    fun userCreate() {
        user1 = userRepository!!.save<User?>(
            User.builder()
                .userLoginId("user1")
                .username("유저1")
                .password("password")
                .role(User.UserRole.ADMIN)
                .build()
        )
        user2 = userRepository.save<User?>(
            User.builder()
                .userLoginId("user2")
                .username("유저2")
                .password("password")
                .role(User.UserRole.MEMBER)
                .build()
        )
        user3 = userRepository.save<User?>(
            User.builder()
                .userLoginId("user3")
                .username("유저3")
                .password("password")
                .role(User.UserRole.MEMBER)
                .build()
        )
    }

    fun walletCreate() {
        wallet1 = walletRepository!!.save<Wallet>(
            Wallet.builder()
                .user(user1)
                .address("Korea")
                .balance(Money.of(500000000L))
                .coinAmounts(ArrayList<CoinAmount>())
                .build()
        )
        wallet2 = walletRepository.save<Wallet?>(
            Wallet.builder()
                .user(user2)
                .address("Korea")
                .balance(Money.of(500000000L))
                .coinAmounts(ArrayList<CoinAmount>())
                .build()
        )
        wallet3 = walletRepository.save<Wallet?>(
            Wallet.builder()
                .user(user3)
                .address("Korea")
                .balance(Money.of(500000000L))
                .coinAmounts(ArrayList<CoinAmount>())
                .build()
        )
    }

    fun tradeLogCreate() {
        if (tradeLogService!!.count() > 0) return

        val logs: MutableList<TradeLog> = ArrayList()
        val baseDate = LocalDateTime.of(2025, 7, 25, 0, 0)

        for (i in 1..15) {
            val log = TradeLog.builder()
                .wallet(wallet1)
                .coin(if (i <= 6 || i > 12) coin1 else coin2)
                .type(if (i % 3 == 0) TradeType.SELL else TradeType.BUY)
                .quantity(BigDecimal.ONE)
                .price(Money.of(100000000L + (i * 10000000L)))
                .build()
            log.setCreatedAt(baseDate.plusDays(((i - 1) * 7).toLong()))
            logs.add(log)
        }
        val log = TradeLog.builder()
            .wallet(wallet1)
            .coin(null)
            .type(TradeType.CHARGE)
            .quantity(BigDecimal.valueOf(0))
            .price(Money.of(200000000L))
            .build()
        logs.add(log)
        tradeLogService.saveAll(logs)
    }

    fun tradeLogChargeCreate() {
        val logs: MutableList<TradeLog?> = ArrayList<TradeLog?>()
        for (i in 0..2) {
            val log = TradeLog.builder()
                .wallet(wallet1) // 실제 Wallet 객체 주입
                .coin(coin1) // 필요하다면 코인도 지정
                .type(TradeType.CHARGE)
                .quantity(BigDecimal.ONE)
                .price(Money.of(200000000L))
                .build()

            log.setCreatedAt(LocalDateTime.now().minusDays((3 - i).toLong())) // 생성일 세팅
            logs.add(log)
        }
        tradeLogRepository!!.saveAll<TradeLog?>(logs)
    }

    fun coinCreate() {
        coin1 = coinRepository!!.save<Coin>(
            Coin.builder()
                .symbol("KRW-BTC2")
                .koreanName("비트코인2")
                .englishName("Bitcoin2")
                .build()
        )

        coin2 = coinRepository.save<Coin>(
            Coin.builder()
                .symbol("KRW-ETH2")
                .koreanName("이더리움2")
                .englishName("Ethereum2")
                .build()
        )

        coin3 = coinRepository.save<Coin?>(
            Coin.builder()
                .symbol("KRW-XRP2")
                .koreanName("리플2")
                .englishName("Ripple2")
                .build()
        )

        coin4 = coinRepository.save<Coin?>(
            Coin.builder()
                .symbol("KRW-DOGE2")
                .koreanName("도지코인2")
                .englishName("Dogecoin2")
                .build()
        )
    }

    fun coinAmountCreate() {
        val ca1 = CoinAmount.builder()
            .wallet(wallet1)
            .coin(coin1)
            .quantity(BigDecimal.valueOf(3.0))
            .totalAmount(Money.of(620000000L))
            .build()

        val ca2 = CoinAmount.builder()
            .wallet(wallet1)
            .coin(coin2)
            .quantity(BigDecimal.valueOf(2.0))
            .totalAmount(Money.of(410000000L))
            .build()

        wallet1.coinAmounts.add(ca1)
        wallet1.coinAmounts.add(ca2)
        coinAmountRepository!!.save<CoinAmount?>(ca1)
        coinAmountRepository.save<CoinAmount?>(ca2)
    }

    @DisplayName("유저 실현 수익률 계산 API - 성공")
    @Test
    @Throws(Exception::class)
    fun t1() {
        val url = "/api/analytics/wallet/" + wallet1!!.getId() + "/realized"

        val resultActions = mockMvc!!
            .perform(MockMvcRequestBuilders.get(url).contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(AnalyticsController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("calculateRealizedProfitRates"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics[0].coinName").value(coin1!!.getId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics[0].totalQuantity").value(3))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.coinAnalytics[0].averageBuyPrice").value(165000000.0)
            ) //                .andExpect(jsonPath("$.coinAnalytics[0].realizedProfitRate").value(closeTo(9.09090900, 0.000001)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics[1].coinName").value(coin2!!.getId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics[1].totalQuantity").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics[1].averageBuyPrice").value(190000000.0))
        //                .andExpect(jsonPath("$.coinAnalytics[1].realizedProfitRate").value(closeTo(7.89473600, 0.000001)))
//                .andExpect(jsonPath("$.profitRateOnInvestment").value(closeTo(6.81818100, 0.000001)));
    }

    @DisplayName("유저 평가 수익률 계산 API - 성공")
    @Test
    @Throws(Exception::class)
    fun t2() {
        val url = "/api/analytics/wallet/" + wallet1!!.getId() + "/unrealized"

        val resultActions = mockMvc!!
            .perform(MockMvcRequestBuilders.get(url).contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(AnalyticsController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("calculateUnRealizedProfitRates"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics.length()").value(2)) // 코인 1
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics[0].coinName").value(coin1!!.getId()))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.coinAnalytics[0].totalQuantity").value(3)
            ) //                .andExpect(jsonPath("$.coinAnalytics[0].averageBuyPrice").value(closeTo(206666666.66666667, 0.000001)))
            //                .andExpect(jsonPath("$.coinAnalytics[0].realizedProfitRate").value(closeTo(11.29032300, 0.000001)))
            // 코인 2
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics[1].coinName").value(coin2!!.getId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.coinAnalytics[1].totalQuantity").value(2))

        //                .andExpect(jsonPath("$.coinAnalytics[1].averageBuyPrice")
//                        .value(closeTo(205000000.00, 0.000001)))
//                .andExpect(jsonPath("$.coinAnalytics[1].realizedProfitRate").value(closeTo(12.19512200, 0.000001)))

        // 총 수익률
//                .andExpect(jsonPath("$.profitRateOnInvestment").value(closeTo(11.65048500, 0.000001)));
    }
}

