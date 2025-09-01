package com.back.back9.domain.orders.service

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.orders.orders.service.OrdersFacade
import com.back.back9.domain.orders.orders.service.OrdersService
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.wallet.entity.CoinAmount
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.domain.wallet.repository.WalletRepository
import com.back.back9.global.error.ErrorException
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.assertEquals

@Tag("order")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class OrdersServiceTest {
    @Autowired
    private val ordersFacade: OrdersFacade? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val walletRepository: WalletRepository? = null

    @Autowired
    private val coinRepository: CoinRepository? = null

    private var user1: User? = null

    private var wallet1: Wallet? = null

    private var coin1: Coin? = null

    @BeforeEach
    fun setUp() {
        coinRepository!!.deleteAll()
        walletRepository!!.deleteAll()
        userRepository!!.deleteAll()
        user1 = userRepository.save<User>(
            User.builder()
                .userLoginId("user1")
                .username("유저1")
                .password("password")
                .role(User.UserRole.ADMIN)
                .build()
        )
        wallet1 = walletRepository.save<Wallet>(
            Wallet.builder()
                .user(user1!!)
                .address("Korea")
                .balance(Money.of(500000000L))
                .coinAmounts(ArrayList<CoinAmount>()) // null 방지
                .build()
        )
        coin1 = coinRepository.save<Coin>(
            Coin.builder()
                .symbol("KRW-BTC4")
                .koreanName("비트코인4")
                .englishName("Bitcoin4")
                .build()
        )
    }

    @DisplayName("OrderService - 매수 주문 성공")
    @Test
    fun createOrder1() {
        val ordersRequest = OrdersRequest(
            coin1!!.symbol,
            TradeType.BUY,
            OrdersMethod.MARKET,
            BigDecimal.valueOf(0.1),
            BigDecimal.valueOf(10000000L)

        )
        // when
        val orderResponse = ordersFacade?.placeOrder(wallet1!!.id!!, ordersRequest)
        // then
        Assertions.assertNotNull(orderResponse)
        orderResponse?.let { Assertions.assertEquals(coin1!!.id, it.coinId) }
        orderResponse?.let { Assertions.assertEquals(BigDecimal.valueOf(0.1), it.quantity) }
        orderResponse?.let { Assertions.assertEquals(BigDecimal.valueOf(10000000L), it.price) }
        orderResponse?.let { Assertions.assertEquals("BUY", it.tradeType) }


        //        log.info("매수 주문 성공: {}", orderResponse);
        //oinAmountRepository.findByWalletId 함수 구현이 안되어있어 주석 처리
//        CoinAmount coinAmount = coinAmountRepository.findByWalletId(wallet1.getId())
//                .orElseThrow(() -> new IllegalArgumentException("코인 금액 정보를 찾을 수 없습니다."));
//        log.info("코인 수 {}", coinAmount.getQuantity());
//        log.info("총 투자 금액 {}", coinAmount.getTotalAmount());
    }

    @DisplayName("OrderService - 매수 실패")
    @Test
    fun createOrder2() {
        val ordersRequest = OrdersRequest(
            coinSymbol = coin1!!.symbol,
            tradeType = TradeType.BUY,
            ordersMethod = OrdersMethod.MARKET,
            quantity = BigDecimal.valueOf(1),
            price = BigDecimal.valueOf(6000000000L)
        )

        val ex = assertThrows<ErrorException> {
            ordersFacade!!.placeOrder(wallet1!!.id!!, ordersRequest)
        }
        assertEquals("INSUFFICIENT_BALANCE", ex.message)
    }


}
