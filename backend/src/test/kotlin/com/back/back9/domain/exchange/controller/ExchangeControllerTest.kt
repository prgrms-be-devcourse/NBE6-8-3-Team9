package com.back.back9.domain.exchange.controller

import com.back.back9.domain.exchange.dto.CandleInitialRequestDTO
import com.back.back9.domain.exchange.dto.CandlePreviousRequestDTO
import com.back.back9.domain.exchange.dto.CandleResponseDTO
import com.back.back9.domain.exchange.service.ExchangeService
import com.back.back9.domain.websocket.vo.CandleInterval
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ExchangeController 통합 테스트")
class ExchangeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val exchangeService: ExchangeService
) {

    @TestConfiguration
    class ControllerTestConfig {
        @Bean
        @Primary
        fun exchangeService(): ExchangeService = mock()
    }

    companion object {
        private const val BTC_SYMBOL = "KRW-BTC"
        private val FIXED_TIME = LocalDateTime.of(2025, 9, 1, 10, 20, 30)
        private val DUMMY_RESPONSE_DTO = CandleResponseDTO(
            timestamp = 1L,
            time = FIXED_TIME,
            symbol = BTC_SYMBOL,
            open = BigDecimal(100),
            high = BigDecimal(110),
            low = BigDecimal(90),
            close = BigDecimal(105),
            volume = BigDecimal(1000)
        )
    }

    @Test
    @DisplayName("POST /initial: 초기 캔들 데이터를 성공적으로 조회한다")
    fun getInitialCandlesSuccess() {
        // 1. 테스트용 요청 DTO와 예상 응답 리스트를 생성합니다.
        val request = CandleInitialRequestDTO(CandleInterval.MIN_1, BTC_SYMBOL)
        val responseList = listOf(DUMMY_RESPONSE_DTO)

        // 2. Mock ExchangeService가 특정 파라미터로 호출될 때, 위에서 만든 예상 응답을 반환하도록 설정합니다.
        whenever(exchangeService.getInitialCandles(request.interval, request.market)).thenReturn(responseList)

        // 3. MockMvc를 사용하여 '/api/exchange/initial' 엔드포인트에 POST 요청을 보냅니다.
        mockMvc.perform(
            post("/api/exchange/initial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            // 4. 응답 상태가 200 OK인지 검증합니다.
            .andExpect(status().isOk)
            // 5. 응답 JSON의 첫 번째 객체의 'market' 필드가 예상 값과 일치하는지 검증합니다.
            .andExpect(jsonPath("$[0].market").value(BTC_SYMBOL))
            // 6. 응답 JSON의 첫 번째 객체의 'trade_price' 필드가 예상 값과 일치하는지 검증합니다.
            .andExpect(jsonPath("$[0].trade_price").value(DUMMY_RESPONSE_DTO.close?.toDouble()))
    }

    @Test
    @DisplayName("POST /previous: 이전 캔들 데이터를 성공적으로 조회한다")
    fun getPreviousCandlesSuccess() {
        // 1. 테스트용 요청 DTO와 예상 응답 리스트를 생성합니다.
        val cursorTimestamp = 12345L
        val request = CandlePreviousRequestDTO(CandleInterval.MIN_1, BTC_SYMBOL, cursorTimestamp)
        val responseList = listOf(DUMMY_RESPONSE_DTO)

        // 2. Mock ExchangeService가 특정 파라미터로 호출될 때, 위에서 만든 예상 응답을 반환하도록 설정합니다.
        whenever(exchangeService.getPreviousCandles(request.interval, request.market, request.cursorTimestamp)).thenReturn(responseList)

        // 3. MockMvc를 사용하여 '/api/exchange/previous' 엔드포인트에 POST 요청을 보냅니다.
        mockMvc.perform(
            post("/api/exchange/previous")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            // 4. 응답 상태가 200 OK인지 검증합니다.
            .andExpect(status().isOk)
            // 5. 응답 JSON의 첫 번째 객체의 'market' 필드가 예상 값과 일치하는지 검증합니다.
            .andExpect(jsonPath("$[0].market").value(BTC_SYMBOL))
    }

    @Test
    @DisplayName("GET /coins-latest: 모든 코인의 최신 시세 정보를 성공적으로 조회한다")
    fun getCoinListSuccess() {
        // 1. 예상 응답 리스트를 생성합니다.
        val responseList = listOf(DUMMY_RESPONSE_DTO)

        // 2. Mock ExchangeService의 'coinsLatest' 프로퍼티가 호출될 때, 위에서 만든 예상 응답을 반환하도록 설정합니다.
        whenever(exchangeService.coinsLatest).thenReturn(responseList)

        // 3. MockMvc를 사용하여 '/api/exchange/coins-latest' 엔드포인트에 GET 요청을 보냅니다.
        mockMvc.perform(get("/api/exchange/coins-latest"))
            // 4. 응답 상태가 200 OK인지 검증합니다.
            .andExpect(status().isOk)
            // 5. 응답 JSON의 첫 번째 객체의 'market' 필드가 예상 값과 일치하는지 검증합니다.
            .andExpect(jsonPath("$[0].market").value(BTC_SYMBOL))
    }
}
