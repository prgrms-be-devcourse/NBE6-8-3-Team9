package com.back.back9.domain.tradeLog.service

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.wallet.entity.CoinAmount
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.domain.wallet.repository.WalletRepository
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Tag("trade_log")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class TradeLogServiceTest {
    @Autowired
    private val tradeLogService: TradeLogService? = null

    @Autowired
    private val walletRepository: WalletRepository? = null

    @Autowired
    private val coinRepository: CoinRepository? = null

    @Autowired
    private val userRepository: UserRepository? = null

    private var wallet: Wallet? = null
    private var coin: Coin? = null
    private var user: User? = null

    @BeforeEach
    fun setUp() {
        userRepository!!.deleteAll()
        walletRepository!!.deleteAll()
        coinRepository!!.deleteAll()
        user = userRepository.save<User?>(
            User.builder()
                .userLoginId("user1")
                .username("테스트유저")
                .password("test1234")
                .role(User.UserRole.MEMBER)
                .build()
        )

        wallet = walletRepository.save<Wallet>(
            Wallet.builder()
                .user(user)
                .address("TestAddress")
                .balance(Money.of(1000000L))
                .coinAmounts(ArrayList<CoinAmount?>())
                .build()
        )

        coin = coinRepository.save<Coin?>(
            Coin.builder()
                .symbol("COIN")
                .koreanName("코인1")
                .englishName("coin1")
                .build()
        )
    }

    @Test
    @DisplayName("거래 내역 생성")
    fun createTradeLog() {
        val tradeLog = TradeLog.builder()
            .wallet(wallet)
            .coin(coin)
            .type(TradeType.BUY)
            .quantity(BigDecimal("0.5"))
            .price(Money.of(43000L))
            .build()

        val saved = tradeLogService!!.save(tradeLog)

        Assertions.assertNotNull(saved.getId())
        Assertions.assertEquals(wallet!!.getId(), saved.wallet!!.getId())
    }
}
