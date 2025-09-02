package com.back.back9.domain.coin.controller

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Tag("coin")
class CoinControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val coinRepository: CoinRepository,
    private val objectMapper: ObjectMapper // JSON 파싱을 위해 추가
) {

    private lateinit var coin1: Coin

    @BeforeEach
    fun setUp() {
        coinRepository.deleteAll()
        // 테스트 데이터는 필요한 만큼만 생성합니다.
        coin1 = coinRepository.save(Coin(symbol = "BTC", koreanName = "비트코인", englishName = "Bitcoin"))
        coinRepository.save(Coin(symbol = "ETH", koreanName = "이더리움", englishName = "Ethereum"))
    }

    @Test
    @DisplayName("Coin 전체 조회")
    fun getCoins() {
        mvc.perform(get("/api/v1/adm/coins"))
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("getCoins"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].symbol").value("BTC"))
            .andExpect(jsonPath("$[1].symbol").value("ETH"))
            .andDo(print())
    }

    @Test
    @DisplayName("Coin 단건 조회")
    fun getCoin() {
        mvc.perform(get("/api/v1/adm/coins/${coin1.id}"))
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("getCoin"))
            .andExpect(jsonPath("$.id").value(coin1.id!!))
            .andExpect(jsonPath("$.symbol").value(coin1.symbol))
            .andDo(print())
    }

    @Test
    @DisplayName("Coin 단건 조회, 404")
    fun getCoinNotFound() {
        mvc.perform(get("/api/v1/adm/coins/99999"))
            .andExpect(status().isNotFound)
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("getCoin"))
            .andExpect(jsonPath("$.status").value("404"))
            .andDo(print())
    }

    @Test
    @DisplayName("Coin 추가")
    fun addCoin() {
        val newCoinInfo = mapOf(
            "symbol" to "XRP",
            "koreanName" to "리플",
            "englishName" to "Ripple"
        )

        mvc.perform(
            post("/api/v1/adm/coins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCoinInfo))
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("addCoin"))
            // 응답값 자체를 검증
            .andExpect(jsonPath("$.id").exists()) // ID가 생성되었는지 확인
            .andExpect(jsonPath("$.symbol").value("XRP"))
            .andExpect(jsonPath("$.koreanName").value("리플"))
            .andDo(print())
    }

    @Test
    @DisplayName("코인 추가, symbol 누락 시 400 Bad Request")
    fun addCoinWithoutSymbol() {
        val newCoinInfo = mapOf(
            "symbol" to "",
            "koreanName" to "비트코인 new",
            "englishName" to "Bitcoin new"
        )

        mvc.perform(
            post("/api/v1/adm/coins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCoinInfo))
        )
            .andExpect(status().isBadRequest)
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("addCoin"))
            .andExpect(jsonPath("$.status").value("400"))
            .andDo(print())
    }

    @Test
    @DisplayName("Coin 삭제")
    fun deleteCoin() {
        // 1. 삭제 API 호출
        mvc.perform(delete("/api/v1/adm/coins/${coin1.id}"))
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("deleteCoin"))
            .andDo(print())

        // 2. 정말 삭제되었는지 확인하기 위해 다시 조회 API를 호출
        mvc.perform(get("/api/v1/adm/coins/${coin1.id}"))
            .andExpect(status().isNotFound) // 404 Not Found 응답을 기대
            .andDo(print())
    }

    @Test
    @DisplayName("Coin 수정")
    fun modifyCoin() {
        val modifiedInfo = mapOf(
            "symbol" to "BTC-MODIFIED",
            "koreanName" to "비트코인(수정)",
            "englishName" to "Bitcoin-Modified"
        )

        // 1. 수정 API 호출
        mvc.perform(
            put("/api/v1/adm/coins/${coin1.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedInfo))
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("modifyCoin"))
            .andDo(print())

        // 2. 정말 수정되었는지 확인하기 위해 다시 조회 API를 호출
        mvc.perform(get("/api/v1/adm/coins/${coin1.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.symbol").value("BTC-MODIFIED"))
            .andExpect(jsonPath("$.koreanName").value("비트코인(수정)"))
            .andDo(print())
    }
}
