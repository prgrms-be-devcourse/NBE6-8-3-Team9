package com.back.back9.domain.wallet.controller

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.tradeLog.repository.TradeLogRepository
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.wallet.dto.BuyCoinRequest
import com.back.back9.domain.wallet.dto.ChargePointsRequest
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.domain.wallet.repository.WalletRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("WalletController 테스트")
@Tag("wallet")
@SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
@Sql(
    statements = [
        "SET REFERENTIAL_INTEGRITY FALSE",
        "TRUNCATE TABLE TRADE_LOG",
        "TRUNCATE TABLE COIN_AMOUNT",
        "TRUNCATE TABLE WALLET",
        "TRUNCATE TABLE COIN",
        "TRUNCATE TABLE USERS",
        "ALTER TABLE TRADE_LOG ALTER COLUMN ID RESTART WITH 1",
        "ALTER TABLE COIN_AMOUNT ALTER COLUMN ID RESTART WITH 1",
        "ALTER TABLE WALLET ALTER COLUMN ID RESTART WITH 1",
        "ALTER TABLE COIN ALTER COLUMN ID RESTART WITH 1",
        "ALTER TABLE USERS ALTER COLUMN ID RESTART WITH 1",
        "SET REFERENTIAL_INTEGRITY TRUE"
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class WalletControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var coinRepository: CoinRepository

    @Autowired
    private lateinit var tradeLogRepository: TradeLogRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        tradeLogRepository.deleteAll()
        walletRepository.deleteAll()
        userRepository.deleteAll()
        coinRepository.deleteAll()

        val user = User.builder()
            .userLoginId("testuser")
            .username("testuser")
            .password("password")
            .role(User.UserRole.ADMIN)
            .build()
        userRepository.save(user)
    }

    @Nested
    @DisplayName("사용자 지갑 조회 테스트")
    inner class GetUserWalletTest {

        @Test
        @DisplayName("성공: 유효한 사용자 ID로 지갑 조회")
        fun t1() {
            val user = userRepository.findAll().first()

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("1000.50")))
                .build()
            walletRepository.save(wallet)

            mockMvc.perform(get("/api/wallets/users/{userId}", user.id))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.userId").value(user.id))
                .andExpect(jsonPath("\$.address").value("0x123456789"))
                .andExpect(jsonPath("\$.balance").value(1000.50))
                .andExpect(jsonPath("\$.coinAmounts").isArray)
                .andExpect(jsonPath("\$.coinAmounts").isEmpty)
        }

        @Test
        @DisplayName("성공: 잔액이 0인 경우")
        fun t2() {
            val user = userRepository.findAll().first()

            val wallet = Wallet.builder()
                .user(user)
                .address("0x987654321")
                .balance(Money.zero())
                .build()
            walletRepository.save(wallet)

            mockMvc.perform(get("/api/wallets/users/{userId}", user.id))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.balance").value(0.0))
                .andExpect(jsonPath("\$.coinAmounts").isArray)
                .andExpect(jsonPath("\$.coinAmounts").isEmpty)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID")
        fun t3() {
            val userId = 999L
            mockMvc.perform(get("/api/wallets/users/{userId}", userId))
                .andDo(print())
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("실패: 잘못된 경로 파라미터 형식")
        fun t4() {
            mockMvc.perform(get("/api/wallets/users/invalid"))
                .andDo(print())
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("지갑 잔액 충전 테스트")
    inner class ChargeWalletTest {

        @Test
        @DisplayName("성공: 유효한 충전 요청")
        fun t5() {
            val user = userRepository.findAll().first()

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("1000.00")))
                .build()
            walletRepository.save(wallet)

            val chargeAmount = BigDecimal("500.00")
            val request = ChargePointsRequest(chargeAmount)

            mockMvc.perform(
                put("/api/wallets/users/{userId}/charge", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.userId").value(user.id))
                .andExpect(jsonPath("\$.address").value("0x123456789"))
                .andExpect(jsonPath("\$.balance").value(1500.00))
                .andExpect(jsonPath("\$.coinAmounts").isArray)
                .andExpect(jsonPath("\$.coinAmounts").isEmpty)
        }

        @Test
        @DisplayName("실패: 음수 충전 금액")
        fun t6() {
            val user = userRepository.findAll().first()

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("1000.00")))
                .build()
            walletRepository.save(wallet)

            val request = ChargePointsRequest(BigDecimal("-100.00"))

            mockMvc.perform(
                put("/api/wallets/users/{userId}/charge", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패: null 충전 금액")
        fun t7() {
            val user = userRepository.findAll().first()

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("1000.00")))
                .build()
            walletRepository.save(wallet)

            val requestBody = """{"amount": null}"""

            mockMvc.perform(
                put("/api/wallets/users/{userId}/charge", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
                .andDo(print())
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID")
        fun t8() {
            val userId = 999L
            val request = ChargePointsRequest(BigDecimal("100.00"))

            mockMvc.perform(
                put("/api/wallets/users/{userId}/charge", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("코인 구매 테스트")
    inner class PurchaseItemTest {

        @Test
        @DisplayName("성공: 유효한 구매 요청")
        fun t9() {
            val user = userRepository.findAll().first()

            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository.save(wallet)

            val request = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("1000.00"),
                        quantity = BigDecimal("0.5")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.userId").value(user.id))
                .andExpect(jsonPath("\$.balance").value(9000.00))
        }

        @Test
        @DisplayName("실패: 잔액 부족")
        fun t10() {
            val user = userRepository.findAll().first()

            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("500.00")))
                .build()
            walletRepository.save(wallet)

            val request = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("1000.00"),
                        quantity = BigDecimal("0.5")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isConflict)
        }

        @Test
        @DisplayName("실패: 음수 구매 금액")
        fun t11() {
            val user = userRepository.findAll().first()

            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository.save(wallet)

            val request = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("-1000.00"),
                        quantity = BigDecimal("0.5")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID")
        fun t12() {
            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val user = userRepository.findAll().first()
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository.save(wallet)

            val request = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("1000.00"),
                        quantity = BigDecimal("0.5")
                    )
                }
            }

            val invalidUserId = 999L

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", invalidUserId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("코인 판매 테스트")
    inner class SellItemTest {

        @Test
        @DisplayName("성공: 유효한 판매 요청")
        fun t13() {
            val user = userRepository.findAll().first()

            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("5000.00")))
                .build()
            walletRepository.save(wallet)

            val buyRequest = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("2000.00"),
                        quantity = BigDecimal("1.0")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyRequest))
            ).andExpect(status().isOk)

            val sellRequest = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("1000.00"),
                        quantity = BigDecimal("0.5")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/sell", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellRequest))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.userId").value(user.id))
                .andExpect(jsonPath("\$.balance").value(4000.00)) // 3000 + 1000
        }

        @Test
        @DisplayName("실패: 보유하지 않은 코인 판매")
        fun t14() {
            val user = userRepository.findAll().first()

            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository.save(wallet)

            val request = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("1000.00"),
                        quantity = BigDecimal("0.5")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/sell", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isConflict)
        }

        @Test
        @DisplayName("실패: 보유 수량보다 많은 판매")
        fun t15() {
            val user = userRepository.findAll().first()

            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository.save(wallet)

            val buyRequest = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("1000.00"),
                        quantity = BigDecimal("0.5")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyRequest))
            ).andExpect(status().isOk)

            val sellRequest = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("2000.00"),
                        quantity = BigDecimal("1.0")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/sell", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellRequest))
            )
                .andDo(print())
                .andExpect(status().isConflict)
        }

        @Test
        @DisplayName("실패: 음수 판매 금액")
        fun t16() {
            val user = userRepository.findAll().first()

            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository.save(wallet)

            val request = coin.id?.let {
                wallet.id?.let { walletId ->
                    BuyCoinRequest(
                        coinId = it,
                        walletId = walletId,
                        amount = BigDecimal("-1000.00"),
                        quantity = BigDecimal("0.5")
                    )
                }
            }

            mockMvc.perform(
                put("/api/wallets/users/{userId}/sell", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isBadRequest)
        }
    }
}
