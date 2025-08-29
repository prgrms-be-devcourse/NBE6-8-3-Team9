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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
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
    private val mockMvc: MockMvc? = null

    @Autowired
    private val walletRepository: WalletRepository? = null

    @Autowired
    private val userRepository: UserRepository? = null // UserRepository 추가

    @Autowired
    private val coinRepository: CoinRepository? = null // CoinRepository 추가

    @Autowired
    private val objectMapper: ObjectMapper? = null

    @BeforeEach
    fun setUp() {
        walletRepository!!.deleteAll()
        userRepository!!.deleteAll()
        coinRepository!!.deleteAll() // CoinRepository 초기화 추가

        val user = User.builder()
            .userLoginId("testuser")
            .username("testuser")
            .password("password")
            .role(User.UserRole.ADMIN)
            .build()
        // 사용자 데이터 저장
        userRepository.save<User?>(user)
    }

    @Nested
    @DisplayName("사용자 지갑 조회 통합 테스트")
    internal inner class GetUserWalletTest {
        @Test
        @DisplayName("성공: 유효한 사용자 ID로 지갑 조회")
        @Throws(Exception::class)
        fun t1() {
            val user = userRepository!!.findAll().get(0)
            // 데이터 저장
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("1000.50")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            // 검증
            mockMvc!!.perform(MockMvcRequestBuilders.get("/api/wallets/users/{userId}", user.getId()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(user.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.address").value("0x123456789"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(1000.50))
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isEmpty())
        }

        @Test
        @DisplayName("성공: 잔액이 0인 경우")
        @Throws(Exception::class)
        fun t2() {
            val user = userRepository!!.findAll().get(0)
            // 데이터 저장(0원)
            val wallet = Wallet.builder()
                .user(user)
                .address("0x987654321")
                .balance(Money.of(BigDecimal.ZERO))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            // 검증
            mockMvc!!.perform(MockMvcRequestBuilders.get("/api/wallets/users/{userId}", user.getId()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(0.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isEmpty())
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID")
        @Throws(Exception::class)
        fun t3() {
            // userId가 존재하지 않는 경우
            val userId = 999L

            // 검증
            mockMvc!!.perform(MockMvcRequestBuilders.get("/api/wallets/users/{userId}", userId))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
        }

        @Test
        @DisplayName("실패: 잘못된 경로 파라미터 형식")
        @Throws(Exception::class)
        fun t4() {
            // 잘못된 사용자 ID
            mockMvc!!.perform(MockMvcRequestBuilders.get("/api/wallets/users/invalid"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
        }
    }

    @Nested
    @DisplayName("지갑 잔액 충전 통합 테스트")
    internal inner class ChargeWalletTest {
        @Test
        @DisplayName("성공: 유효한 충전 요청")
        @Throws(Exception::class)
        fun t5() {
            val user = userRepository!!.findAll().get(0)
            // DB에 지갑 데이터 저장
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("1000.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            val chargeAmount = BigDecimal("500.00")
            val request = ChargePointsRequest(chargeAmount)

            // 검증 - 충전은 정상 동작하고, coinAmounts는 빈 배열이어야 함 (유효한 CoinAmount가 없으므로)
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/charge", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(user.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.address").value("0x123456789"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(1500.00)) // 1000 + 500
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isEmpty()) // 유효한 CoinAmount가 없으므로 빈 배열
        }

        @Test
        @DisplayName("실패: 음수 충전 금액")
        @Throws(Exception::class)
        fun t6() {
            val user = userRepository!!.findAll().get(0)
            // DB에 지갑 데이터 저장
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("1000.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            val request = ChargePointsRequest(BigDecimal("-100.00"))

            // 검증
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/charge", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
        }

        @Test
        @DisplayName("성공: 여러 번 충전하여 누적 확인")
        @Throws(Exception::class)
        fun t7() {
            val user = userRepository!!.findAll().get(0)
            // DB에 지갑 데이터 저장
            val initialBalance = BigDecimal("1000.00")
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(initialBalance))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            // 첫 번째 충전
            val request1 = ChargePointsRequest(BigDecimal("500.00"))
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/charge", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request1))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(1500.00)) // 1000 + 500

            // 두 번째 충전
            val request2 = ChargePointsRequest(BigDecimal("300.00"))
            mockMvc.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/charge", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(1800.00)) // 1500 + 300
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isEmpty()) // 유효한 CoinAmount가 없으므로 빈 배열
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID")
        @Throws(Exception::class)
        fun t8() {
            val userId = 999L
            val request = ChargePointsRequest(BigDecimal("100.00"))

            // 검증
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/charge", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
        }
    }

    @Nested
    @DisplayName("코인 구매 통합 테스트")
    internal inner class PurchaseItemIntegrationTest {
        @Test
        @DisplayName("성공: 유효한 구매 요청")
        @Throws(Exception::class)
        fun t9() {
            val user = userRepository!!.findAll().get(0)

            // 테스트용 코인 생성
            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository!!.save<Coin?>(coin)

            // 지갑 생성 (충분한 잔액)
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            val request = BuyCoinRequest(
                coin.getId(),  // coinId
                wallet.getId(),  // walletId
                BigDecimal("1000.00"),  // 구매 금액
                BigDecimal("0.5") // 구매 수량
            )

            // 검증
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/purchase", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(user.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(9000.00)) // 10000 - 1000
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isNotEmpty()) // 구매 후 코인 보유
        }

        @Test
        @DisplayName("실패: 잔액 부족")
        @Throws(Exception::class)
        fun t10() {
            val user = userRepository!!.findAll().get(0)

            // 테스트용 코인 생성
            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository!!.save<Coin?>(coin)

            // 지갑 생성 (부족한 잔액)
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("500.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            val request = BuyCoinRequest(
                coin.getId(),  // coinId
                wallet.getId(),  // walletId
                BigDecimal("1000.00"),  // 구매 금액 (잔액보다 큼)
                BigDecimal("0.5")
            )

            // 검증
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/purchase", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isConflict())
        }

        @Test
        @DisplayName("실패: 존재하지 않는 코인 ID")
        @Throws(Exception::class)
        fun t11() {
            val user = userRepository!!.findAll().get(0)

            // 지갑 생성
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            val request = BuyCoinRequest(
                999L,  // 존재하지 않는 coinId
                wallet.getId(),
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            // 검증
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/purchase", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
        }

        @Test
        @DisplayName("실패: 다른 사용자의 지갑 접근")
        @Throws(Exception::class)
        fun t12() {
            val user = userRepository!!.findAll().get(0)

            // 다른 사용자 생성
            val otherUser = User.builder()
                .userLoginId("otheruser")
                .username("otheruser")
                .password("password")
                .role(User.UserRole.MEMBER)
                .build()
            userRepository.save<User?>(otherUser)

            // 테스트용 코인 생성
            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository!!.save<Coin?>(coin)

            // 다른 사용자의 지갑 생성
            val otherWallet = Wallet.builder()
                .user(otherUser)
                .address("0x987654321")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository!!.save<Wallet?>(otherWallet)

            val request = BuyCoinRequest(
                coin.getId(),
                otherWallet.getId(),  // 다른 사용자의 지갑 ID
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            // 검증 - 권한이 없어야 함
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/purchase", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized()) // UNAUTHORIZED_ACCESS 에러
        }
    }

    @Nested
    @DisplayName("코인 판매 통합 테스트")
    internal inner class SellItemIntegrationTest {
        @Test
        @DisplayName("성공: 유효한 판매 요청")
        @Throws(Exception::class)
        fun t13() {
            val user = userRepository!!.findAll().get(0)

            // 테스트용 코인 생성
            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository!!.save<Coin?>(coin)

            // 지갑 생성
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("5000.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            // 먼저 코인을 구매해서 보유량을 만듦
            val buyRequest = BuyCoinRequest(
                coin.getId(),
                wallet.getId(),
                BigDecimal("2000.00"),
                BigDecimal("1.0")
            )

            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/purchase", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(buyRequest))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())

            // 이제 판매 테스트
            val sellRequest = BuyCoinRequest(
                coin.getId(),
                wallet.getId(),
                BigDecimal("1000.00"),  // 판매 금액
                BigDecimal("0.5") // 판매 수량
            )

            // 검증
            mockMvc.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/sell", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellRequest))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(user.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(4000.00)) // 3000 + 1000 (구매 후 잔액 + 판매 수익)
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isNotEmpty()) // 아직 코인 보유
        }

        @Test
        @DisplayName("실패: 보유하지 않은 코인 판매")
        @Throws(Exception::class)
        fun t14() {
            val user = userRepository!!.findAll().get(0)

            // 테스트용 코인 생성
            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository!!.save<Coin?>(coin)

            // 지갑 생성 (코인 보유 없음)
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            val request = BuyCoinRequest(
                coin.getId(),
                wallet.getId(),
                BigDecimal("1000.00"),
                BigDecimal("0.5")
            )

            // 검증
            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/sell", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isConflict())
        }

        @Test
        @DisplayName("실패: 보유 수량보다 많은 판매")
        @Throws(Exception::class)
        fun t15() {
            val user = userRepository!!.findAll().get(0)

            // 테스트용 코인 생성
            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository!!.save<Coin?>(coin)

            // 지갑 생성
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            // 먼저 적은 양의 코인 구매
            val buyRequest = BuyCoinRequest(
                coin.getId(),
                wallet.getId(),
                BigDecimal("1000.00"),
                BigDecimal("0.5") // 0.5개 구매
            )

            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/purchase", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(buyRequest))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())

            // 보유량보다 많이 판매 시도
            val sellRequest = BuyCoinRequest(
                coin.getId(),
                wallet.getId(),
                BigDecimal("2000.00"),
                BigDecimal("1.0") // 1.0개 판매 시도 (0.5개만 보유)
            )

            // 검증
            mockMvc.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/sell", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellRequest))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isConflict())
        }

        @Test
        @DisplayName("성공: 연속 거래 테스트 (구매→판매→구매)")
        @Throws(Exception::class)
        fun t16() {
            val user = userRepository!!.findAll().get(0)

            // 테스트용 코인 생성
            val coin = Coin.builder()
                .englishName("Bitcoin")
                .symbol("BTC")
                .build()
            coinRepository!!.save<Coin?>(coin)

            // 지갑 생성
            val wallet = Wallet.builder()
                .user(user)
                .address("0x123456789")
                .balance(Money.of(BigDecimal("10000.00")))
                .build()
            walletRepository!!.save<Wallet?>(wallet)

            // 1. 첫 번째 구매
            val buyRequest1 = BuyCoinRequest(
                coin.getId(),
                wallet.getId(),
                BigDecimal("2000.00"),
                BigDecimal("1.0")
            )

            mockMvc!!.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/purchase", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper!!.writeValueAsString(buyRequest1))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(8000.00)) // 10000 - 2000

            // 2. 판매
            val sellRequest = BuyCoinRequest(
                coin.getId(),
                wallet.getId(),
                BigDecimal("1500.00"),
                BigDecimal("0.7")
            )

            mockMvc.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/sell", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellRequest))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(9500.00)) // 8000 + 1500

            // 3. 두 번째 구매
            val buyRequest2 = BuyCoinRequest(
                coin.getId(),
                wallet.getId(),
                BigDecimal("1000.00"),
                BigDecimal("0.4")
            )

            mockMvc.perform(
                MockMvcRequestBuilders.put("/api/wallets/users/{userId}/purchase", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyRequest2))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(8500.00)) // 9500 - 1000
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.coinAmounts").isNotEmpty())
        }
    }
}
