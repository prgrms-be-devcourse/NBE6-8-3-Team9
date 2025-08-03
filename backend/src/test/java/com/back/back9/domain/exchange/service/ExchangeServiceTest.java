package com.back.back9.domain.exchange.service;

import com.back.back9.domain.exchange.dto.CoinPriceResponse;
import com.back.back9.domain.exchange.dto.ExchangeDTO;
import com.back.back9.global.redis.initializer.RedisInitializer;
import com.back.back9.global.redis.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.ApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

//테스트의 반환형식이 일치하면 성공
@Slf4j
class ExchangeServiceTest {

    @Mock
    private RedisService redisService;

    @Mock
    private RedisInitializer redisInitializer;

    @InjectMocks
    private ExchangeService exchangeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        openMocks(this);
        redisInitializer.run(mock(ApplicationArguments.class));
    }

    @Test
    void getInitialCandleTest() {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("timestamp", 123456);
        json.put("trade_price", 50000);
        json.put("opening_price", 49000);
        json.put("high_price", 51000);
        json.put("low_price", 48000);
        json.put("candle_acc_trade_volume", 12.5);

        when(redisService.getLatestCandle("1m", "KRW-BTC")).thenReturn(List.of(json));

        List<ExchangeDTO> result = exchangeService.getInitialCandles("1m", "KRW-BTC");

        assertThat(result).isNotEmpty();
        ExchangeDTO dto = result.getFirst();
        assertThat(dto).hasFieldOrProperty("timestamp");
        assertThat(dto).hasFieldOrProperty("open");
        assertThat(dto).hasFieldOrProperty("high");
        assertThat(dto).hasFieldOrProperty("low");
        assertThat(dto).hasFieldOrProperty("close");
        assertThat(dto).hasFieldOrProperty("volume");

        log.info("getInitialCandles 결과: {}", result);
    }

    @Test
    void getPreviousCandlesTest() {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("timestamp", 654321);
        json.put("trade_price", 49000);
        json.put("opening_price", 48000);
        json.put("high_price", 50000);
        json.put("low_price", 47000);
        json.put("candle_acc_trade_volume", 8.0);

        when(redisService.getPreviousCandlesByRange("1m", "KRW-BTC", 0, "9999999999999"))
                .thenReturn(List.of(json));

        List<ExchangeDTO> result = exchangeService.getPreviousCandles("1m", "KRW-BTC", 0, "9999999999999");

        assertThat(result).isNotEmpty();
        ExchangeDTO dto = result.getFirst();
        assertThat(dto).hasFieldOrProperty("timestamp");
        assertThat(dto).hasFieldOrProperty("open");
        assertThat(dto).hasFieldOrProperty("high");
        assertThat(dto).hasFieldOrProperty("low");
        assertThat(dto).hasFieldOrProperty("close");
        assertThat(dto).hasFieldOrProperty("volume");

        log.info("getPreviousCandles 결과: {}", result);
    }

    @Test
    void getLatestCandleByScanTest() {
        // given
        ObjectNode json = objectMapper.createObjectNode();
        json.put("timestamp", 123456);
        json.put("trade_price", 12345.67);

        String coinName = "비트코인";
        String expectedSymbol = "KRW-BTC";

        when(redisService.getLatest1sCandle(expectedSymbol)).thenReturn(json);

        // when
        CoinPriceResponse result = exchangeService.getLatestCandleByScan(coinName);

        // then: 반환 객체가 CoinPriceResponse이고, 필드 구성(형식)만 확인
        assertThat(result).isNotNull();
        assertThat(result).hasFieldOrProperty("symbol");
        assertThat(result).hasFieldOrProperty("price");
        assertThat(result).hasFieldOrProperty("time");

        log.info("getLatestCandleByScan 결과: {}", result);
    }
}