package com.back.back9.websocket.service

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.coin.service.CoinService
import com.back.back9.domain.websocket.service.DatabaseCoinListProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class DatabaseCoinListProviderIntegrationTest {

    @Autowired
    private lateinit var provider: DatabaseCoinListProvider

    @Autowired
    private lateinit var coinService: CoinService

    @Autowired
    private lateinit var coinRepository: CoinRepository

    @AfterEach
    fun tearDown() {
        // 각 테스트가 끝난 후 DB를 정리하여 테스트 독립성을 보장합니다.
        coinRepository.deleteAll()
    }

    @BeforeEach
    fun setUp() {
        // 테스트 시작 전 초기 데이터 설정 및 캐시 동기화
        coinRepository.saveAllAndFlush(listOf(
            Coin("KRW-BTC", "비트코인", "Bitcoin"),
            Coin("KRW-ETH", "이더리움", "Ethereum")
        ))
        provider.refreshCache()
    }

    @Test
    @DisplayName("CoinService로 새 코인을 추가하면 Provider 캐시에 실시간으로 반영된다")
    fun addCoinUpdatesCache() {
        // 동작: CoinService를 통해 새 코인 추가
        coinService.add("KRW-XRP", "리플", "Ripple")

        // 검증: Provider의 내부 캐시가 자동으로 업데이트되었는지 확인
        assertThat(provider.getMarketCodes()).containsExactlyInAnyOrder("KRW-BTC", "KRW-ETH", "KRW-XRP")
        assertThat(provider.getNameBySymbol("KRW-XRP")).isEqualTo("리플")
    }

    @Test
    @DisplayName("CoinService로 기존 코인을 수정하면 Provider 캐시에 실시간으로 반영된다")
    fun modifyCoinUpdatesCache() {
        // 준비: 수정할 기존 코인
        val btc = coinService.findBySymbol("KRW-BTC")!!

        // 동작: CoinService를 통해 코인 이름 수정
        coinService.modify(btc, null, "비트코인-수정됨", null)

        // 검증: Provider의 캐시에서 수정된 이름이 조회되는지 확인
        assertThat(provider.getNameBySymbol("KRW-BTC")).isEqualTo("비트코인-수정됨")
    }

    @Test
    @DisplayName("CoinService로 코인을 삭제하면 Provider 캐시에서 실시간으로 제거된다")
    fun deleteCoinRemovesFromCache() {
        // 준비: 삭제할 기존 코인
        val eth = coinService.findBySymbol("KRW-ETH")!!

        // 동작: CoinService를 통해 코인 삭제
        coinService.delete(eth)

        // 검증: Provider의 캐시에서 해당 코인이 제거되었는지 확인
        assertThat(provider.getMarketCodes()).containsExactlyInAnyOrder("KRW-BTC")
        assertThat(provider.getNameBySymbol("KRW-ETH")).isNull()
    }
}