package com.back.back9.domain.orders.controller

import com.back.back9.domain.orders.orders.controller.OrdersController
import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.orders.orders.entity.OrdersStatus
import com.back.back9.domain.orders.orders.repository.OrdersRepository
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.repository.UserRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Tag("orders")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class OrdersControllerTest @Autowired constructor(
    val ordersRepository: OrdersRepository,
    val userRepository: UserRepository,
    val walletRepository: WalletRepository,
    val coinRepository: CoinRepository,
    val mvc: MockMvc
) {
    private lateinit var wallet1: Wallet
    private lateinit var coin1: Coin
    private lateinit var coin2: Coin

    @BeforeEach
    fun setUp() {
        ordersRepository.deleteAll()

        // 유저 생성
        val user = userRepository.save(
            User.builder().userLoginId("u1").username("user1").password("1234").role(User.UserRole.MEMBER).build()
        )

        wallet1 = walletRepository.save(
            Wallet.builder().user(user).address("addr1").balance(Money.of(1_000_000L)).build()
        )

        // 코인 생성
        coin1 = coinRepository.save(Coin.builder().symbol("KRW-BTC").koreanName("비트코인").englishName("Bitcoin").build())
        coin2 = coinRepository.save(Coin.builder().symbol("KRW-ETH").koreanName("이더리움").englishName("Ethereum").build())

        createOrders()
    }

    private fun createOrders() {
        val baseDate = LocalDateTime.of(2025, 7, 25, 10, 0)

        for (i in 1..10) {
            val tradeType = if (i % 2 == 0) TradeType.BUY else TradeType.SELL
            val coin = if (i <= 5) coin1 else coin2

            val order = Orders.builder()
                .wallet(wallet1)
                .coin(coin)
                .tradeType(tradeType)
                .ordersMethod(OrdersMethod.LIMIT)
                .quantity(BigDecimal.ONE)
                .price(BigDecimal(1000 * i))
                .build()

//            order.setCreatedAt(baseDate.plusDays(i.toLong()))
            ordersRepository.save(order)
        }
    }

    @Test
    @DisplayName("주문 전체 조회")
    fun t1() {
        val url = "/api/orders/wallet/${wallet1.id}"

        val resultActions = mvc.perform(get(url)
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(OrdersController::class.java))
            .andExpect(handler().methodName("getOrders"))
        //.andExpect(jsonPath("$.length()").value(10))
    }

    @Test
    @DisplayName("주문 조회 - 특정 기간 + BUY만")
    fun t2() {
        val url = "/api/orders/wallet/${wallet1.id}"

        val resultActions = mvc.perform(get(url)
            .param("startDate", "2025-07-25")
            .param("endDate", "2025-08-05")
            .param("tradeType", "BUY")
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(OrdersController::class.java))
            .andExpect(handler().methodName("getOrders"))
        //.andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    @DisplayName("주문 조회 - 코인 심볼 필터")
    fun t3() {
        val url = "/api/orders/wallet/${wallet1.id}"

        val resultActions = mvc.perform(get(url)
            .param("coinSymbol", "KRW-BTC")
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(OrdersController::class.java))
            .andExpect(handler().methodName("getOrders"))
        //.andExpect(jsonPath("$.length()").value(5))
    }

    @Test
    @DisplayName("주문 조회 - 기간 잘못된 경우 (시작일 > 종료일)")
    fun t4() {
        val url = "/api/orders/wallet/${wallet1.id}"

        val resultActions = mvc.perform(get(url)
            .param("startDate", "2025-08-10")
            .param("endDate", "2025-07-25")
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(OrdersController::class.java))
            .andExpect(handler().methodName("getOrders"))
        //.andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @DisplayName("주문 조회 - 파라미터 없음")
    fun t5() {
        val url = "/api/orders/wallet/${wallet1.id}"

        val resultActions = mvc.perform(get(url)
            .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(OrdersController::class.java))
            .andExpect(handler().methodName("getOrders"))
        //.andExpect(jsonPath("$.length()").value(10))
    }
}
