package com.back.back9.domain.coin.controller

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.coin.service.CoinService
import com.back.back9.global.error.ErrorException
import org.junit.jupiter.api.*
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
    private val coinService: CoinService,
    private val coinRepository: CoinRepository
) {

    private lateinit var coin1: Coin
    private lateinit var coin2: Coin
    private lateinit var coin3: Coin
    private lateinit var coin4: Coin

    @BeforeEach
    fun setUp() {
        coinRepository.deleteAll()

        coin1 = coinRepository.save(Coin(symbol = "BTC", koreanName = "비트코인", englishName = "Bitcoin"))
        coin2 = coinRepository.save(Coin(symbol = "ETH", koreanName = "이더리움", englishName = "Ethereum"))
        coin3 = coinRepository.save(Coin(symbol = "XRP", koreanName = "리플", englishName = "Ripple"))
        coin4 = coinRepository.save(Coin(symbol = "DOGE", koreanName = "도지코인", englishName = "Dogecoin"))
    }

    @Test
    @DisplayName("Coin 전체 조회")
    fun getCoins() {
        val resultActions = mvc.perform(get("/api/v1/adm/coins"))
            .andDo(print())

        val testCoins = coinService.findAll()

        resultActions
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("getCoins"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(4))

        testCoins.forEachIndexed { i, coin ->
            resultActions
                .andExpect(jsonPath("$[$i].id").value(coin.id))
                .andExpect(jsonPath("$[$i].symbol").value(coin.symbol))
                .andExpect(jsonPath("$[$i].koreanName").value(coin.koreanName))
                .andExpect(jsonPath("$[$i].englishName").value(coin.englishName))
        }
    }

    @Test
    @DisplayName("Coin 단건 조회")
    fun getCoin() {
        val resultActions = mvc.perform(get("/api/v1/adm/coins/${coin1.id}"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("getCoin"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.symbol").value(coin1.symbol))
    }

    @Test
    @DisplayName("Coin 단건 조회, 404")
    fun getCoinNotFound() {
        val resultActions = mvc.perform(get("/api/v1/adm/coins/999"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("getCoin"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value("404"))
    }

    @Test
    @DisplayName("Coin 추가")
    fun addCoin() {
        val resultActions = mvc.perform(
            post("/api/v1/adm/coins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "symbol" : "BTC new",
                        "koreanName" : "비트코인 new",
                        "englishName" : "Bitcoin new"
                    }
                    """.trimIndent()
                )
        ).andDo(print())

        val coin = coinService.findLastest()

        resultActions
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("addCoin"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(coin?.id))
            .andExpect(jsonPath("$.symbol").value(coin?.symbol))
            .andExpect(jsonPath("$.koreanName").value(coin?.koreanName))
            .andExpect(jsonPath("$.englishName").value(coin?.englishName))
    }

    @Test
    @DisplayName("코인 추가, Without symbol")
    fun addCoinWithoutSymbol() {
        val resultActions = mvc.perform(
            post("/api/v1/adm/coins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "symbol" : "",
                        "koreanName" : "비트코인 new",
                        "englishName" : "Bitcoin new"
                    }
                    """.trimIndent()
                )
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("addCoin"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value("400"))
    }

    @Test
    @DisplayName("Coin 삭제")
    fun deleteCoin() {
        val resultActions = mvc.perform(delete("/api/v1/adm/coins/${coin1.id}"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("deleteCoin"))
            .andExpect(status().isOk)

        assertThrows<ErrorException> {
            coinService.findById(coin1.id!!)
        }
    }

    @Test
    @DisplayName("Coin 수정")
    fun modifyCoin() {
        val resultActions = mvc.perform(
            put("/api/v1/adm/coins/${coin1.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "symbol" : "BTC 수정",
                        "koreanName" : "비트코인 수정",
                        "englishName" : "Bitcoin 수정"
                    }
                    """.trimIndent()
                )
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CoinController::class.java))
            .andExpect(handler().methodName("modifyCoin"))
            .andExpect(status().isOk)
    }
}
