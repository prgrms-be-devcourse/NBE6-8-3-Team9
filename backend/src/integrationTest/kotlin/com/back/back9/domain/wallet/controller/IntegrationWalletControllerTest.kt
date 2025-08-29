package com.back.back9.domain.wallet.controller

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("WalletController 통합 테스트")
@Transactional
@Tag("wallet")
class IntegrationWalletControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var walletRepository: WalletRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var coinRepository: CoinRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
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
    @DisplayName("사용자 지갑 조회 통합 테스트")
    inner class GetUserWalletTest {

        @Test
        @DisplayName("성공: 유효한 사용자 ID로 지갑 조회")
        fun t1() {
            val user = userRepository.findAll()[0]
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
                .andExpect(jsonPath("$.userId").value(user.id))
                .andExpect(jsonPath("$.address").value("0x123456789"))
                .andExpect(jsonPath("$.balance").value(1000.50))
                .andExpect(jsonPath("$.coinAmounts").isArray)
                .andExpect(jsonPath("$.coinAmounts").isEmpty)
        }

        @Test
        @DisplayName("성공: 잔액이 0인 경우")
        fun t2() {
            val user = userRepository.findAll()[0]
            val wallet = Wallet.builder()
                .user(user)
                .address("0x987654321")
                .balance(Money.of(BigDecimal.ZERO))
                .build()
            walletRepository.save(wallet)

            mockMvc.perform(get("/api/wallets/users/{userId}", user.id))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.coinAmounts").isArray)
                .andExpect(jsonPath("$.coinAmounts").isEmpty)
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
    @DisplayName("지갑 잔액 충전 통합 테스트")
    inner class ChargeWalletTest {

        @Test
        @DisplayName("성공: 유효한 충전 요청")
        fun t5() {
            val user = userRepository.findAll()[0]
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
                .andExpect(jsonPath("$.userId").value(user.id))
                .andExpect(jsonPath("$.address").value("0x123456789"))
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.coinAmounts").isArray)
                .andExpect(jsonPath("$.coinAmounts").isEmpty)
        }

        @Test
        @DisplayName("실패: 음수 충전 금액")
        fun t6() {
            val user = userRepository.findAll()[0]
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
        @DisplayName("성공: 여러 번 충전하여 누적 확인")
        fun t7() {
            val user = userRepository.findAll()[0]
            val initialBalance = BigDecimal("1000.00")
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(initialBalance))
                .build()
            walletRepository.save(wallet)

            val request1 = ChargePointsRequest(BigDecimal("500.00"))
            mockMvc.perform(
                put("/api/wallets/users/{userId}/charge", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.balance").value(1500.00))

            val request2 = ChargePointsRequest(BigDecimal("300.00"))
            mockMvc.perform(
                put("/api/wallets/users/{userId}/charge", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.balance").value(1800.00))
                .andExpect(jsonPath("$.coinAmounts").isArray)
                .andExpect(jsonPath("$.coinAmounts").isEmpty)
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
    @DisplayName("코인 구매 통합 테스트")
    inner class PurchaseItemIntegrationTest {

        @Test
        @DisplayName("성공: 유효한 구매 요청")
        fun t9() {
            val user = userRepository.findAll()[0]

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

            val request = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(user.id))
                .andExpect(jsonPath("$.balance").value(9000.00))
                .andExpect(jsonPath("$.coinAmounts").isArray)
                .andExpect(jsonPath("$.coinAmounts").isNotEmpty)
        }

        @Test
        @DisplayName("실패: 잔액 부족")
        fun t10() {
            val user = userRepository.findAll()[0]

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

            val request = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isConflict)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 코인 ID")
        fun t11() {
            val user = userRepository.findAll()[0]

            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository.save(wallet)

            val request = BuyCoinRequest(
                999L,
                wallet.id,
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("실패: 다른 사용자의 지갑 접근")
        fun t12() {
            val user = userRepository.findAll()[0]

            val otherUser = User.builder()
                .userLoginId("otheruser")
                .username("otheruser")
                .password("password")
                .role(User.UserRole.MEMBER)
                .build()
            userRepository.save(otherUser)

            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository.save(coin)

            val otherWallet = Wallet.builder()
                .user(otherUser)
                .address("0x987654321")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository.save(otherWallet)

            val request = BuyCoinRequest(
                coin.id,
                otherWallet.id, // 다른 사용자의 지갑
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andDo(print())
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("코인 판매 통합 테스트")
    inner class SellItemIntegrationTest {

        @Test
        @DisplayName("성공: 유효한 판매 요청")
        fun t13() {
            val user = userRepository.findAll()[0]

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

            val buyRequest = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("2000.00"),
                BigDecimal("1.0")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyRequest))
            )
                .andExpect(status().isOk)

            val sellRequest = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/sell", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellRequest))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(user.id))
                .andExpect(jsonPath("$.balance").value(4000.00))
                .andExpect(jsonPath("$.coinAmounts").isArray)
                .andExpect(jsonPath("$.coinAmounts").isNotEmpty)
        }

        @Test
        @DisplayName("실패: 보유하지 않은 코인 판매")
        fun t14() {
            val user = userRepository.findAll()[0]

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

            val request = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

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
            val user = userRepository.findAll()[0]

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

            val buyRequest = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyRequest))
            )
                .andExpect(status().isOk)

            val sellRequest = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("2000.00"),
                BigDecimal("1.0")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/sell", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellRequest))
            )
                .andDo(print())
                .andExpect(status().isConflict)
        }

        @Test
        @DisplayName("성공: 연속 거래 테스트 (구매→판매→구매)")
        fun t16() {
            val user = userRepository.findAll()[0]

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

            val buyRequest1 = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("2000.00"),
                BigDecimal("1.0")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyRequest1))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.balance").value(8000.00))

            val sellRequest = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("1500.00"),
                BigDecimal("0.7")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/sell", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.balance").value(9500.00))

            val buyRequest2 = BuyCoinRequest(
                coin.id,
                wallet.id,
                BigDecimal("1000.00"),
                BigDecimal("0.4")
            )

            mockMvc.perform(
                put("/api/wallets/users/{userId}/purchase", user.id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyRequest2))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.balance").value(8500.00))
                .andExpect(jsonPath("$.coinAmounts").isArray)
                .andExpect(jsonPath("$.coinAmounts").isNotEmpty)
        }
    }
}
