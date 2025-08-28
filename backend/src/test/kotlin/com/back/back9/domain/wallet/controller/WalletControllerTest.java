package com.back.back9.domain.wallet.controller;

import com.back.back9.domain.coin.entity.Coin;
import com.back.back9.domain.coin.repository.CoinRepository;
import com.back.back9.domain.tradeLog.repository.TradeLogRepository;
import com.back.back9.domain.user.entity.User;
import com.back.back9.domain.user.repository.UserRepository;
import com.back.back9.domain.wallet.dto.BuyCoinRequest;
import com.back.back9.domain.wallet.dto.ChargePointsRequest;
import com.back.back9.domain.wallet.entity.Wallet;
import com.back.back9.domain.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("WalletController 테스트")
@Tag("wallet")
public class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private TradeLogRepository tradeLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        tradeLogRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
        coinRepository.deleteAll();

        // 테스트용 User 데이터 미리 생성
        User user = User.builder()
                .userLoginId("testuser")
                .username("testuser")
                .password("password")
                .role(User.UserRole.ADMIN)
                .build();
        userRepository.save(user);
    }

    @Nested
    @DisplayName("사용자 지갑 조회 테스트")
    class GetUserWalletTest {

        @Test
        @DisplayName("성공: 유효한 사용자 ID로 지갑 조회")
        void t1() throws Exception {
            User user = userRepository.findAll().get(0);
            // 데이터 저장
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("1000.50"))
                    .build();
            walletRepository.save(wallet);

            // 검증
            mockMvc.perform(get("/api/wallets/users/{userId}", user.getId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId").value(user.getId()))
                    .andExpect(jsonPath("$.address").value("0x123456789"))
                    .andExpect(jsonPath("$.balance").value(1000.50))
                    .andExpect(jsonPath("$.coinAmounts").isArray())
                    .andExpect(jsonPath("$.coinAmounts").isEmpty());
        }

        @Test
        @DisplayName("성공: 잔액이 0인 경우")
        void t2() throws Exception {
            User user = userRepository.findAll().get(0);
            // 데이터 저장(0원)
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x987654321")
                    .balance(BigDecimal.ZERO)
                    .build();
            walletRepository.save(wallet);

            // 검증
            mockMvc.perform(get("/api/wallets/users/{userId}", user.getId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(0.0))
                    .andExpect(jsonPath("$.coinAmounts").isArray())
                    .andExpect(jsonPath("$.coinAmounts").isEmpty());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID")
        void t3() throws Exception {
            // userId가 존재하지 않는 경우
            Long userId = 999L;

            // 검증
            mockMvc.perform(get("/api/wallets/users/{userId}", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 잘못된 경로 파라미터 형식")
        void t4() throws Exception {
            // 잘못된 사용자 ID
            mockMvc.perform(get("/api/wallets/users/invalid"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("지갑 잔액 충전 테스트")
    class ChargeWalletTest {

        @Test
        @DisplayName("성공: 유효한 충전 요청")
        void t5() throws Exception {
            User user = userRepository.findAll().get(0);
            // DB에 지갑 데이터 저장
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("1000.00"))
                    .build();
            walletRepository.save(wallet);

            BigDecimal chargeAmount = new BigDecimal("500.00");
            ChargePointsRequest request = new ChargePointsRequest(chargeAmount);

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/charge", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId").value(user.getId()))
                    .andExpect(jsonPath("$.address").value("0x123456789"))
                    .andExpect(jsonPath("$.balance").value(1500.00)) // 1000 + 500
                    .andExpect(jsonPath("$.coinAmounts").isArray())
                    .andExpect(jsonPath("$.coinAmounts").isEmpty());
        }

        @Test
        @DisplayName("실패: 음수 충전 금액")
        void t6() throws Exception {
            User user = userRepository.findAll().get(0);
            // DB에 지갑 데이터 저장
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("1000.00"))
                    .build();
            walletRepository.save(wallet);

            ChargePointsRequest request = new ChargePointsRequest(new BigDecimal("-100.00"));

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/charge", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: null 충전 금액")
        void t7() throws Exception {
            User user = userRepository.findAll().get(0);
            // DB에 지갑 데이터 저장
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("1000.00"))
                    .build();
            walletRepository.save(wallet);

            String requestBody = "{\"amount\": null}";

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/charge", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID")
        void t8() throws Exception {
            Long userId = 999L;
            ChargePointsRequest request = new ChargePointsRequest(new BigDecimal("100.00"));

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/charge", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("코인 구매 테스트")
    class PurchaseItemTest {

        @Test
        @DisplayName("성공: 유효한 구매 요청")
        void t9() throws Exception {
            User user = userRepository.findAll().get(0);

            // 테스트용 코인 생성
            Coin coin = Coin.builder()
                    .englishName("Bitcoin")
                    .symbol("BTC")
                    .build();
            coinRepository.save(coin);

            // 지갑 생성 (충분한 잔액)
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("10000.00"))
                    .build();
            walletRepository.save(wallet);

            BuyCoinRequest request = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("1000.00"),  // 구매 금액
                    new BigDecimal("0.5")       // 구매 수량
            );

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/purchase", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId").value(user.getId()))
                    .andExpect(jsonPath("$.balance").value(9000.00)); // 10000 - 1000
        }

        @Test
        @DisplayName("실패: 잔액 부족")
        void t10() throws Exception {
            User user = userRepository.findAll().get(0);

            // 테스트용 코인 생성
            Coin coin = Coin.builder()
                    .englishName("Bitcoin")
                    .symbol("BTC")
                    .build();
            coinRepository.save(coin);

            // 지갑 생성 (부족한 잔액)
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("500.00"))
                    .build();
            walletRepository.save(wallet);

            BuyCoinRequest request = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("1000.00"),  // 구매 금액 (잔액보다 큼)
                    new BigDecimal("0.5")
            );

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/purchase", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("실패: 음수 구매 금액")
        void t11() throws Exception {
            User user = userRepository.findAll().get(0);

            Coin coin = Coin.builder()
                    .englishName("Bitcoin")
                    .symbol("BTC")
                    .build();
            coinRepository.save(coin);

            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("10000.00"))
                    .build();
            walletRepository.save(wallet);

            BuyCoinRequest request = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("-1000.00"),  // 음수 금액
                    new BigDecimal("0.5")
            );

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/purchase", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID")
        void t12() throws Exception {
            Coin coin = Coin.builder()
                    .englishName("Bitcoin")
                    .symbol("BTC")
                    .build();
            coinRepository.save(coin);

            User user = userRepository.findAll().get(0);
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("10000.00"))
                    .build();
            walletRepository.save(wallet);

            BuyCoinRequest request = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("1000.00"),
                    new BigDecimal("0.5")
            );

            Long invalidUserId = 999L;

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/purchase", invalidUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("코인 판매 테스트")
    class SellItemTest {

        @Test
        @DisplayName("성공: 유효한 판매 요청")
        void t13() throws Exception {
            User user = userRepository.findAll().get(0);

            // 테스트용 코인 생성
            Coin coin = Coin.builder()
                    .englishName("Bitcoin")
                    .symbol("BTC")
                    .build();
            coinRepository.save(coin);

            // 지갑 생성
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("5000.00"))
                    .build();
            walletRepository.save(wallet);

            // 먼저 코인을 구매해서 보유량을 만듦
            BuyCoinRequest buyRequest = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("2000.00"),
                    new BigDecimal("1.0")
            );

            mockMvc.perform(put("/api/wallets/users/{userId}/purchase", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buyRequest)))
                    .andExpect(status().isOk());

            // 이제 판매 테스트
            BuyCoinRequest sellRequest = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("1000.00"),  // 판매 금액
                    new BigDecimal("0.5")       // 판매 수량
            );

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/sell", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sellRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId").value(user.getId()))
                    .andExpect(jsonPath("$.balance").value(4000.00)); // 3000 + 1000 (구매 후 잔액 + 판매 수익)
        }

        @Test
        @DisplayName("실패: 보유하지 않은 코인 판매")
        void t14() throws Exception {
            User user = userRepository.findAll().get(0);

            // 테스트용 코인 생성
            Coin coin = Coin.builder()
                    .englishName("Bitcoin")
                    .symbol("BTC")
                    .build();
            coinRepository.save(coin);

            // 지갑 생성 (코인 보유 없음)
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("10000.00"))
                    .build();
            walletRepository.save(wallet);

            BuyCoinRequest request = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("1000.00"),
                    new BigDecimal("0.5")
            );

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/sell", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("실패: 보유 수량보다 많은 판매")
        void t15() throws Exception {
            User user = userRepository.findAll().get(0);

            // 테스트용 코인 생성
            Coin coin = Coin.builder()
                    .englishName("Bitcoin")
                    .symbol("BTC")
                    .build();
            coinRepository.save(coin);

            // 지갑 생성
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("10000.00"))
                    .build();
            walletRepository.save(wallet);

            // 먼저 적은 양의 코인 구매
            BuyCoinRequest buyRequest = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("1000.00"),
                    new BigDecimal("0.5")  // 0.5개 구매
            );

            mockMvc.perform(put("/api/wallets/users/{userId}/purchase", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buyRequest)))
                    .andExpect(status().isOk());

            // 보유량보다 많이 판매 시도
            BuyCoinRequest sellRequest = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("2000.00"),
                    new BigDecimal("1.0")  // 1.0개 판매 시도 (0.5개만 보유)
            );

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/sell", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sellRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("실패: 음수 판매 금액")
        void t16() throws Exception {
            User user = userRepository.findAll().get(0);

            Coin coin = Coin.builder()
                    .englishName("Bitcoin")
                    .symbol("BTC")
                    .build();
            coinRepository.save(coin);

            Wallet wallet = Wallet.builder()
                    .user(user)
                    .address("0x123456789")
                    .balance(new BigDecimal("10000.00"))
                    .build();
            walletRepository.save(wallet);

            BuyCoinRequest request = new BuyCoinRequest(
                    coin.getId(),           // coinId
                    wallet.getId(),         // walletId
                    new BigDecimal("-1000.00"),  // 음수 금액
                    new BigDecimal("0.5")
            );

            // 검증
            mockMvc.perform(put("/api/wallets/users/{userId}/sell", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}
