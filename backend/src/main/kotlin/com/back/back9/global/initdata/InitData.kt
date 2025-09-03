package com.back.back9.global.initdata

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.user.entity.User
import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.domain.wallet.repository.WalletRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.springframework.security.crypto.password.PasswordEncoder

@Component
class InitData(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
    private val coinRepository: CoinRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val log: Logger = LoggerFactory.getLogger(InitData::class.java)
    @PostConstruct
    fun init() {
//        if (userRepository.count() > 0){
//            log.info("Initdata 이미 존재")
//            return
//        } // 중복 삽입 방지
//        val encodedPassword = passwordEncoder.encode("user")
//
//        val user1 = userRepository.save(
//            User.builder()
//                .userLoginId("user1")
//                .username("유저1")
//                .password(encodedPassword) // 실제에선 반드시 인코딩 필요
//                .role(User.UserRole.ADMIN)
//                .build()
//        )
//
//        val wallet1 = walletRepository.save(
//            Wallet.builder()
//                .user(user1)
//                .address("Korea")
//                .balance(Money.of(500_000_000L))
//                .coinAmounts(mutableListOf()) // null 방지
//                .build()
//        )
//
//        coinRepository.saveAll(
//            listOf(
//                Coin.builder()
//                    .symbol("KRW-BTC")
//                    .koreanName("비트코인")
//                    .englishName("Bitcoin")
//                    .build(),
//                Coin.builder()
//                    .symbol("KRW-ETH")
//                    .koreanName("이더리움")
//                    .englishName("Ethereum")
//                    .build()
//            )
//        )
//
//        println("✅ InitData 로드 완료: user=${user1.userLoginId}, wallet=${wallet1.address}")
    }
}